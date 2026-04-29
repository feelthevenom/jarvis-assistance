package com.personalassistant.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class VoicePhase { Idle, Listening, Thinking, Speaking, Error }

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.Idle,
    val transcript: String = "",
    val partial: String = "",
    val error: String? = null,
    val canRecord: Boolean = false,
)

interface VoiceControllerListener {
    fun onFinalTranscript(text: String)
}

/**
 * Wraps Android SpeechRecognizer for speech-to-text and TextToSpeech for the
 * spoken assistant reply. Exposes a StateFlow so the Voice screen can render
 * Idle / Listening / Thinking / Speaking states without the caller managing
 * platform listeners.
 */
class VoiceController(context: Context) {

    private val appContext = context.applicationContext

    private val _state = MutableStateFlow(VoiceUiState(canRecord = SpeechRecognizer.isRecognitionAvailable(appContext)))
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var listener: VoiceControllerListener? = null
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeech: String? = null
    private var keepListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    fun setListener(listener: VoiceControllerListener?) {
        this.listener = listener
    }

    fun refreshAvailability() {
        _state.value = _state.value.copy(
            canRecord = SpeechRecognizer.isRecognitionAvailable(appContext),
        )
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                phase = VoicePhase.Error,
                error = "Speech recognition is not available on this device.",
            )
            return
        }
        keepListening = true
        stopTts()
        ensureRecognizer()
        runCatching { recognizer?.cancel() }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_SWITCH_ALLOWED_LANGUAGES,
                arrayListOf("en-IN", "en-US", "ta-IN"),
            )
            putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
            putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_SWITCH, RecognizerIntent.LANGUAGE_SWITCH_BALANCED)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1_800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1_200L)
        }
        _state.value = VoiceUiState(
            phase = VoicePhase.Listening,
            transcript = "",
            partial = "",
            canRecord = true,
        )
        runCatching { recognizer?.startListening(intent) }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    phase = VoicePhase.Error,
                    error = error.message ?: "Could not start recognizer",
                )
            }
    }

    fun stopListening() {
        keepListening = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.cancel() }
        if (_state.value.phase == VoicePhase.Listening) {
            _state.value = _state.value.copy(phase = VoicePhase.Idle)
        }
    }

    fun markThinking() {
        _state.value = _state.value.copy(phase = VoicePhase.Thinking)
    }

    fun resumeListeningIfNeeded() {
        if (keepListening) restartListeningSoon()
    }

    fun speak(text: String) {
        if (text.isBlank()) {
            _state.value = _state.value.copy(phase = VoicePhase.Idle)
            resumeListeningIfNeeded()
            return
        }
        ensureTts()
        if (!ttsReady) {
            pendingSpeech = text
            return
        }
        _state.value = _state.value.copy(phase = VoicePhase.Speaking, error = null)
        tts?.language = languageFor(text)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stopTts() {
        if (tts?.isSpeaking == true) tts?.stop()
        if (_state.value.phase == VoicePhase.Speaking) {
            _state.value = _state.value.copy(phase = VoicePhase.Idle)
        }
    }

    fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        ttsReady = false
        pendingSpeech = null
        listener = null
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(recognitionListener)
        }
    }

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale("en", "IN")
                tts?.setOnUtteranceProgressListener(progressListener)
                pendingSpeech?.let {
                    pendingSpeech = null
                    speak(it)
                }
            } else {
                _state.value = _state.value.copy(
                    phase = VoicePhase.Error,
                    error = "Text-to-speech engine could not start.",
                )
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?: return
            _state.value = _state.value.copy(partial = partial)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            _state.value = _state.value.copy(
                phase = VoicePhase.Thinking,
                transcript = text,
                partial = "",
            )
            if (text.isNotBlank()) {
                listener?.onFinalTranscript(text)
            } else {
                _state.value = _state.value.copy(phase = VoicePhase.Idle)
                restartListeningSoon()
            }
        }

        override fun onError(error: Int) {
            val message = errorMessage(error)
            Log.w(TAG, "SpeechRecognizer error $error: $message")
            if (keepListening && error.isRecoverableRecognitionError()) {
                _state.value = _state.value.copy(
                    phase = VoicePhase.Listening,
                    error = null,
                    partial = "",
                )
                restartListeningSoon()
            } else {
                _state.value = _state.value.copy(
                    phase = VoicePhase.Error,
                    error = message,
                )
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            _state.value = _state.value.copy(phase = VoicePhase.Speaking)
        }

        override fun onDone(utteranceId: String?) {
            _state.value = _state.value.copy(phase = VoicePhase.Idle)
            resumeListeningIfNeeded()
        }

        @Deprecated(
            message = "Use onError(String?, Int) instead",
            replaceWith = ReplaceWith("onError(utteranceId, -1)"),
        )
        override fun onError(utteranceId: String?) {
            onError(utteranceId, -1)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            _state.value = _state.value.copy(
                phase = VoicePhase.Error,
                error = "Text-to-speech failed (code $errorCode).",
            )
        }
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
        SpeechRecognizer.ERROR_CLIENT -> "Recognizer client error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
        SpeechRecognizer.ERROR_NETWORK -> "Network error during recognition."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out."
        SpeechRecognizer.ERROR_NO_MATCH -> "I did not catch that, try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer is busy."
        SpeechRecognizer.ERROR_SERVER -> "Server error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I did not hear anything."
        else -> "Speech recognition error."
    }

    private fun restartListeningSoon() {
        if (!keepListening) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (keepListening && _state.value.phase != VoicePhase.Thinking && _state.value.phase != VoicePhase.Speaking) {
                startListening()
            }
        }, RESTART_DELAY_MS)
    }

    private fun Int.isRecoverableRecognitionError(): Boolean {
        return this == SpeechRecognizer.ERROR_NO_MATCH ||
            this == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
            this == SpeechRecognizer.ERROR_CLIENT ||
            this == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
    }

    private fun languageFor(text: String): Locale {
        val hasTamil = text.any { it.code in TAMIL_UNICODE_RANGE }
        return if (hasTamil) Locale("ta", "IN") else Locale("en", "IN")
    }

    companion object {
        private const val TAG = "VoiceController"
        private const val RESTART_DELAY_MS = 450L
        private val TAMIL_UNICODE_RANGE = 0x0B80..0x0BFF
    }
}
