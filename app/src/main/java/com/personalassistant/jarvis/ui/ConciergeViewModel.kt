package com.personalassistant.jarvis.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personalassistant.jarvis.assistant.GemmaAssistantEngine
import com.personalassistant.jarvis.assistant.ModelStatus
import com.personalassistant.jarvis.data.AppSettings
import com.personalassistant.jarvis.data.ChatMessage
import com.personalassistant.jarvis.data.ChatSession
import com.personalassistant.jarvis.data.ConversationStore
import com.personalassistant.jarvis.data.MessageRole
import com.personalassistant.jarvis.data.SettingsStore
import com.personalassistant.jarvis.ui.theme.AppTheme
import com.personalassistant.jarvis.voice.VoiceController
import com.personalassistant.jarvis.voice.VoiceControllerListener
import com.personalassistant.jarvis.voice.VoicePhase
import com.personalassistant.jarvis.voice.VoiceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Tab { Assistant, Voice, Settings }

data class ConciergeUiState(
    val tab: Tab = Tab.Assistant,
    val drawerOpen: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val currentSessionId: String? = null,
    val composer: String = "",
    val isThinking: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val modelStatus: ModelStatus = ModelStatus.NotInitialized,
    val voice: VoiceUiState = VoiceUiState(),
) {
    val currentSession: ChatSession?
        get() = sessions.firstOrNull { it.id == currentSessionId }

    val currentMessages: List<ChatMessage>
        get() = currentSession?.messages.orEmpty()
}

class ConciergeViewModel(
    application: Application,
) : AndroidViewModel(application), VoiceControllerListener {

    private val conversationStore = ConversationStore(application)
    private val settingsStore = SettingsStore(application)
    private val engine = GemmaAssistantEngine(application)
    private val voice = VoiceController(application)

    private val _ui = MutableStateFlow(ConciergeUiState())
    val ui: StateFlow<ConciergeUiState> = _ui.asStateFlow()

    val expectedModelPath: String get() = engine.expectedModelPath()
    val modelRepoUrl: String get() = engine.modelRepoUrl()
    val modelDownloadUrl: String get() = engine.modelDownloadUrl()
    val modelSizeBytes: Long get() = engine.modelSizeBytes()

    init {
        voice.setListener(this)
        val settings = settingsStore.load()
        val sessions = if (settings.saveHistory) conversationStore.loadSessions() else emptyList()
        _ui.value = _ui.value.copy(
            settings = settings,
            sessions = sessions,
        )
        observeEngine()
        observeVoice()
        viewModelScope.launch { engine.ensureReady() }
    }

    private fun observeEngine() {
        viewModelScope.launch {
            engine.status.collect { status ->
                _ui.value = _ui.value.copy(modelStatus = status)
            }
        }
    }

    private fun observeVoice() {
        viewModelScope.launch {
            voice.state.collect { voiceState ->
                _ui.value = _ui.value.copy(voice = voiceState)
            }
        }
    }

    fun selectTab(tab: Tab) {
        _ui.value = _ui.value.copy(tab = tab, drawerOpen = false)
        if (tab != Tab.Voice) {
            voice.stopListening()
            voice.stopTts()
        }
    }

    fun setDrawerOpen(open: Boolean) {
        _ui.value = _ui.value.copy(drawerOpen = open)
    }

    fun updateComposer(value: String) {
        _ui.value = _ui.value.copy(composer = value)
    }

    fun newChat() {
        _ui.value = _ui.value.copy(
            currentSessionId = null,
            composer = "",
            drawerOpen = false,
            tab = Tab.Assistant,
        )
    }

    fun openSession(sessionId: String) {
        _ui.value = _ui.value.copy(
            currentSessionId = sessionId,
            drawerOpen = false,
            tab = Tab.Assistant,
        )
    }

    fun deleteSession(sessionId: String) {
        val updated = _ui.value.sessions.filterNot { it.id == sessionId }
        _ui.value = _ui.value.copy(
            sessions = updated,
            currentSessionId = if (_ui.value.currentSessionId == sessionId) null else _ui.value.currentSessionId,
        )
        persistSessions()
    }

    fun clearAllSessions() {
        _ui.value = _ui.value.copy(sessions = emptyList(), currentSessionId = null)
        conversationStore.clearAll()
    }

    fun sendComposerPrompt() {
        val text = _ui.value.composer.trim()
        if (text.isEmpty() || _ui.value.isThinking) return
        _ui.value = _ui.value.copy(composer = "")
        sendUserMessage(text, speakReply = false)
    }

    private fun sendUserMessage(text: String, speakReply: Boolean) {
        val state = _ui.value
        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(role = MessageRole.User, body = text, timestamp = now)
        val session = state.currentSession ?: ChatSession(
            title = ConversationStore.deriveTitleFromPrompt(text),
            createdAt = now,
            updatedAt = now,
        )
        val sessionWithUser = session.copy(
            title = if (session.title == ConversationStore.DEFAULT_TITLE && session.messages.isEmpty()) {
                ConversationStore.deriveTitleFromPrompt(text)
            } else session.title,
            messages = session.messages + userMessage,
            updatedAt = now,
        )
        val pending = ChatMessage(
            role = MessageRole.Assistant,
            body = "",
            timestamp = now + 1,
            pending = true,
        )
        val sessionWithPending = sessionWithUser.copy(
            messages = sessionWithUser.messages + pending,
        )
        upsertSession(sessionWithPending)
        _ui.value = _ui.value.copy(
            currentSessionId = sessionWithPending.id,
            isThinking = true,
        )

        if (speakReply) voice.markThinking()

        viewModelScope.launch {
            val reply = engine.generate(text, sessionWithUser.messages)
            val updatedMessages = sessionWithPending.messages.dropLast(1) + ChatMessage(
                id = pending.id,
                role = MessageRole.Assistant,
                body = reply,
                timestamp = System.currentTimeMillis(),
            )
            val finalSession = sessionWithPending.copy(
                messages = updatedMessages,
                updatedAt = System.currentTimeMillis(),
            )
            upsertSession(finalSession)
            _ui.value = _ui.value.copy(isThinking = false)
            if (speakReply && _ui.value.settings.voiceAutoSpeak) {
                voice.speak(reply)
            } else if (speakReply) {
                voice.stopTts()
            }
        }
    }

    private fun upsertSession(session: ChatSession) {
        val existing = _ui.value.sessions.toMutableList()
        val index = existing.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            existing[index] = session
        } else {
            existing.add(0, session)
        }
        existing.sortByDescending { it.updatedAt }
        _ui.value = _ui.value.copy(sessions = existing, currentSessionId = session.id)
        persistSessions()
    }

    private fun persistSessions() {
        if (!_ui.value.settings.saveHistory) {
            conversationStore.clearAll()
            return
        }
        conversationStore.saveSessions(_ui.value.sessions)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_ui.value.settings)
        _ui.value = _ui.value.copy(settings = updated)
        settingsStore.save(updated)
        if (!updated.saveHistory) {
            conversationStore.clearAll()
        } else {
            conversationStore.saveSessions(_ui.value.sessions)
        }
        if (!updated.voiceAutoSpeak) voice.stopTts()
    }

    fun setTheme(theme: AppTheme) {
        updateSettings { it.copy(theme = theme) }
    }

    fun startVoiceCapture() {
        voice.startListening()
    }

    fun stopVoiceCapture() {
        voice.stopListening()
    }

    fun stopVoicePlayback() {
        voice.stopTts()
    }

    fun handleMicrophonePermissionResult(granted: Boolean) {
        voice.refreshAvailability()
        if (!granted) {
            _ui.value = _ui.value.copy(
                voice = _ui.value.voice.copy(
                    phase = VoicePhase.Error,
                    error = "Microphone permission is required for voice mode.",
                ),
            )
        }
    }

    fun retryEngine() {
        viewModelScope.launch { engine.ensureReady() }
    }

    fun downloadModel() {
        if (_ui.value.modelStatus is ModelStatus.Downloading) return
        viewModelScope.launch { engine.downloadModel() }
    }

    fun reloadEngine() {
        engine.release()
        viewModelScope.launch { engine.ensureReady() }
    }

    fun chatStorageBytes(): Long {
        return _ui.value.sessions.sumOf { session ->
            session.messages.sumOf { (it.body.length * 2L) + 64L } + session.title.length * 2L + 128L
        }
    }

    override fun onCleared() {
        super.onCleared()
        voice.release()
        engine.release()
    }

    override fun onFinalTranscript(text: String) {
        sendUserMessage(text, speakReply = true)
    }
}
