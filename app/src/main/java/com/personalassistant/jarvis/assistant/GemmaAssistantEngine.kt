package com.personalassistant.jarvis.assistant

import android.content.Context
import android.util.Log
import com.personalassistant.jarvis.data.ChatMessage
import com.personalassistant.jarvis.data.MessageRole
import com.personalassistant.jarvis.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed class ModelStatus {
    object NotInitialized : ModelStatus()
    object Initializing : ModelStatus()
    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : ModelStatus()
    data class Missing(val expectedPath: String) : ModelStatus()
    object Ready : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

/**
 * Wrapper around Google AI Edge LiteRT-LM. The runtime classes are looked up
 * with reflection so the rest of the app keeps building even if the artifact
 * version drifts; once the user drops a `gemma-4-E2B-it.litertlm` file into the
 * expected folder we initialize the engine and return real on-device answers.
 */
class GemmaAssistantEngine(context: Context) {

    private val appContext = context.applicationContext

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotInitialized)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    private val modelDir: File = File(appContext.filesDir, MODEL_DIR_NAME).apply { mkdirs() }
    val modelFile: File = File(modelDir, MODEL_FILE_NAME)

    @Volatile
    private var engine: Any? = null

    fun expectedModelPath(): String = modelFile.absolutePath

    fun modelRepoUrl(): String = MODEL_REPO_URL

    fun modelDownloadUrl(): String = MODEL_DOWNLOAD_URL

    fun modelSizeBytes(): Long = MODEL_SIZE_BYTES

    fun isModelPresent(): Boolean = modelFile.exists() && modelFile.length() >= MODEL_SIZE_BYTES

    suspend fun ensureReady(): ModelStatus = withContext(Dispatchers.IO) {
        if (engine != null) {
            _status.value = ModelStatus.Ready
            return@withContext ModelStatus.Ready
        }
        if (!isModelPresent()) {
            val missing = ModelStatus.Missing(modelFile.absolutePath)
            _status.value = missing
            return@withContext missing
        }
        if (!isRuntimeAvailable()) {
            val error = ModelStatus.Error(
                "LiteRT-LM runtime is not bundled. Add the runtime AAR at app/libs/litertlm-android.aar.",
            )
            _status.value = error
            return@withContext error
        }
        _status.value = ModelStatus.Initializing
        runCatching { createEngineReflectively() }
            .onSuccess {
                engine = it
                _status.value = ModelStatus.Ready
            }
            .onFailure { error ->
                Log.w(TAG, "Failed to initialize LiteRT-LM engine", error)
                _status.value = ModelStatus.Error(
                    error.message ?: "LiteRT-LM engine could not be initialized.",
                )
            }
        _status.value
    }

    suspend fun downloadModel(): ModelStatus = withContext(Dispatchers.IO) {
        if (isModelPresent()) {
            return@withContext ensureReady()
        }

        modelDir.mkdirs()
        val tempFile = File(modelDir, "$MODEL_FILE_NAME.download")
        runCatching {
            val connection = (URL(MODEL_DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "ThraggAndroid/1.0")
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    var lastEmit = 0L
                    _status.value = ModelStatus.Downloading(0L, MODEL_SIZE_BYTES)

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        total += read
                        if (total - lastEmit > PROGRESS_EMIT_BYTES || total == MODEL_SIZE_BYTES) {
                            _status.value = ModelStatus.Downloading(total, MODEL_SIZE_BYTES)
                            lastEmit = total
                        }
                    }
                    output.flush()
                }
            }

            if (tempFile.length() < MODEL_SIZE_BYTES) {
                error(
                    "Model download incomplete. Expected ${formatBytes(MODEL_SIZE_BYTES)}, got ${formatBytes(tempFile.length())}.",
                )
            }
            if (modelFile.exists()) modelFile.delete()
            check(tempFile.renameTo(modelFile)) {
                "Could not move downloaded model to ${modelFile.absolutePath}."
            }
        }.onFailure { error ->
            tempFile.delete()
            Log.w(TAG, "Model download failed", error)
            _status.value = ModelStatus.Error(error.message ?: "Model download failed.")
            return@withContext _status.value
        }

        ensureReady()
    }

    suspend fun generate(
        prompt: String,
        history: List<ChatMessage>,
        profile: UserProfile,
    ): String = withContext(Dispatchers.Default) {
        when (val state = ensureReady()) {
            is ModelStatus.Missing -> "I need a model file at\n${state.expectedPath}\nbefore I can answer."
            is ModelStatus.Error -> "Local Gemma runtime error: ${state.message}"
            ModelStatus.NotInitialized,
            is ModelStatus.Downloading,
            ModelStatus.Initializing -> "I am still loading the on-device model."
            ModelStatus.Ready -> runGeneration(prompt, history, profile)
        }
    }

    fun release() {
        closeReflectively(engine)
        engine = null
        _status.value = ModelStatus.NotInitialized
    }

    private fun runGeneration(prompt: String, history: List<ChatMessage>, profile: UserProfile): String {
        val activeEngine = engine ?: return "Engine not initialized."
        val composedPrompt = buildPromptString(prompt, history, profile)
        return runCatching {
            val conversation = createConversationReflectively(activeEngine)
            try {
                val response = sendMessageReflectively(conversation, composedPrompt)
                extractMessageText(response)
            } finally {
                closeReflectively(conversation)
            }
        }.getOrElse { error ->
            Log.w(TAG, "Generation failed", error)
            "I hit a local generation error: ${error.message ?: "unknown"}"
        }
    }

    private fun buildPromptString(
        prompt: String,
        history: List<ChatMessage>,
        profile: UserProfile,
    ): String {
        val builder = StringBuilder()
        builder.append("You are Thragg, a helpful private on-device assistant. ")
        builder.append("Reply concisely. Use the user's language: English, Tamil, or a natural mix when the user mixes them. ")
        builder.append("Treat the conversation history as reliable memory for this chat. ")
        builder.append("If the user asks about something they told you earlier, answer from the earlier turns instead of saying you do not know.\n\n")
        if (profile.hasContext()) {
            builder.append("Persistent user profile:\n")
            if (profile.name.isNotBlank()) builder.append("- Name: ").append(profile.name.trim()).append('\n')
            if (profile.age.isNotBlank()) builder.append("- Age: ").append(profile.age.trim()).append('\n')
            if (profile.dateOfBirth.isNotBlank()) {
                builder.append("- Date of birth: ").append(profile.dateOfBirth.trim()).append('\n')
            }
            if (profile.personalDetail.isNotBlank()) {
                builder.append("- Personal detail: ").append(profile.personalDetail.trim()).append('\n')
            }
            builder.append('\n')
        }
        builder.append("Recent conversation:\n")
        history.takeLast(MAX_HISTORY_TURNS).forEach { message ->
            val speaker = if (message.role == MessageRole.User) "User" else "Assistant"
            builder.append(speaker).append(": ").append(message.body.trim()).append('\n')
            if (message.imageUri != null) {
                builder.append(speaker).append(" attached an image.\n")
            }
        }
        builder.append("User: ").append(prompt.trim()).append('\n')
        builder.append("Assistant:")
        return builder.toString()
    }

    private fun createConversationReflectively(activeEngine: Any): Any {
        val method = activeEngine.javaClass.methods.firstOrNull { method ->
            method.name == "createConversation" && method.parameterTypes.size == 1
        } ?: error("LiteRT-LM Engine does not expose createConversation(ConversationConfig).")

        val config = instantiateLeniently(method.parameterTypes[0])
            ?: error("Unable to create LiteRT-LM ConversationConfig.")
        return method.invoke(activeEngine, config)
            ?: error("LiteRT-LM createConversation returned null.")
    }

    private fun sendMessageReflectively(conversation: Any, text: String): Any {
        val method = conversation.javaClass.methods.firstOrNull { method ->
            method.name == "sendMessage" &&
                method.parameterTypes.isNotEmpty() &&
                method.parameterTypes[0] == String::class.java
        } ?: error("LiteRT-LM Conversation does not expose sendMessage(String, ...).")

        val args = when (method.parameterTypes.size) {
            1 -> arrayOf<Any?>(text)
            2 -> arrayOf<Any?>(text, emptyMap<String, Any>())
            else -> error("Unsupported sendMessage signature: ${method.parameterTypes.joinToString()}.")
        }
        return method.invoke(conversation, *args)
            ?: error("LiteRT-LM sendMessage returned null.")
    }

    private fun extractMessageText(message: Any): String {
        val text = runCatching {
            message.javaClass.getMethod("getText").invoke(message) as? String
        }.getOrNull()
        if (!text.isNullOrBlank()) return text

        val contents = runCatching {
            message.javaClass.getMethod("getContents").invoke(message)
        }.getOrNull()
        val contentText = contents?.toString().orEmpty()
        if (contentText.isNotBlank()) return contentText

        return message.toString()
    }

    private fun closeReflectively(target: Any?) {
        runCatching {
            target?.javaClass?.methods
                ?.firstOrNull { it.name == "close" && it.parameterTypes.isEmpty() }
                ?.invoke(target)
        }
    }

    private fun createEngineReflectively(): Any {
        val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend")
        val cpuClass = Class.forName("com.google.ai.edge.litertlm.Backend\$CPU")
        val cpuBackend = instantiateLeniently(cpuClass)
            ?: error("Unable to instantiate Backend.CPU")

        val configClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
        val configCtor = configClass.constructors
            .sortedByDescending { it.parameterTypes.size }
            .firstOrNull { it.parameterTypes.isNotEmpty() && it.parameterTypes[0] == String::class.java }
            ?: error("EngineConfig signature not recognised.")

        val configArgs = arrayOfNulls<Any?>(configCtor.parameterTypes.size).apply {
            this[0] = modelFile.absolutePath
            for (i in 1 until size) {
                this[i] = when (configCtor.parameterTypes[i]) {
                    backendClass -> cpuBackend
                    Int::class.javaPrimitiveType, Int::class.java -> 0
                    Boolean::class.javaPrimitiveType, Boolean::class.java -> false
                    else -> null
                }
            }
        }
        val config = configCtor.newInstance(*configArgs)

        val engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
        val engineCtor = engineClass.getDeclaredConstructor(configClass)
        val instance = engineCtor.newInstance(config)
        engineClass.methods.firstOrNull { it.name == "initialize" }?.invoke(instance)
        return instance
    }

    private fun isRuntimeAvailable(): Boolean {
        return runCatching {
            Class.forName("com.google.ai.edge.litertlm.Engine")
            Class.forName("com.google.ai.edge.litertlm.EngineConfig")
            Class.forName("com.google.ai.edge.litertlm.Backend")
        }.isSuccess
    }

    private fun instantiateLeniently(cls: Class<*>): Any? {
        return cls.constructors
            .sortedBy { it.parameterTypes.size }
            .firstNotNullOfOrNull { ctor ->
                runCatching {
                    val args = ctor.parameterTypes.map { type ->
                        when (type) {
                            Int::class.javaPrimitiveType, Int::class.java -> 4
                            Long::class.javaPrimitiveType, Long::class.java -> 0L
                            Boolean::class.javaPrimitiveType, Boolean::class.java -> false
                            String::class.java -> ""
                            else -> null
                        }
                    }.toTypedArray()
                    ctor.newInstance(*args)
                }.getOrNull()
            }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / 1024.0 / 1024.0 / 1024.0
        return String.format("%.2f GB", gb)
    }

    companion object {
        private const val TAG = "GemmaAssistantEngine"
        private const val MODEL_DIR_NAME = "models"
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val MODEL_ID = "litert-community/gemma-4-E2B-it-litert-lm"
        const val MODEL_COMMIT_HASH = "7fa1d78473894f7e736a21d920c3aa80f950c0db"
        const val MODEL_SIZE_BYTES = 2_583_085_056L
        const val MODEL_REPO_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm"
        const val MODEL_DOWNLOAD_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/7fa1d78473894f7e736a21d920c3aa80f950c0db/gemma-4-E2B-it.litertlm"
        private const val MAX_HISTORY_TURNS = 12
        private const val PROGRESS_EMIT_BYTES = 4L * 1024L * 1024L
    }
}
