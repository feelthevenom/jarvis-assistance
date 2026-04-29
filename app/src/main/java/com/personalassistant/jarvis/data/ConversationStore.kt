package com.personalassistant.jarvis.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight JSON-backed persistence for chat sessions. Each session keeps its
 * messages so the side history drawer can show prior conversations and the user
 * can resume any of them, similar to ChatGPT's left sidebar.
 */
class ConversationStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSessions(): List<ChatSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> array.getJSONObject(index).toSession() }
                .sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun saveSessions(sessions: List<ChatSession>) {
        val array = JSONArray()
        sessions.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    private fun JSONObject.toSession(): ChatSession {
        val messageArray = optJSONArray("messages") ?: JSONArray()
        val messages = List(messageArray.length()) { i ->
            val item = messageArray.getJSONObject(i)
            ChatMessage(
                id = item.optString("id"),
                role = runCatching { MessageRole.valueOf(item.getString("role")) }
                    .getOrDefault(MessageRole.Assistant),
                body = item.optString("body"),
                timestamp = item.optLong("timestamp", System.currentTimeMillis()),
            )
        }
        return ChatSession(
            id = optString("id"),
            title = optString("title", DEFAULT_TITLE),
            messages = messages,
            createdAt = optLong("createdAt", System.currentTimeMillis()),
            updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        )
    }

    private fun ChatSession.toJson(): JSONObject {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", message.role.name)
                    .put("body", message.body)
                    .put("timestamp", message.timestamp),
            )
        }
        return JSONObject()
            .put("id", id)
            .put("title", title)
            .put("messages", array)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
    }

    companion object {
        private const val PREFS_NAME = "concierge_chats"
        private const val KEY_SESSIONS = "sessions"
        const val DEFAULT_TITLE = "New chat"

        fun deriveTitleFromPrompt(prompt: String): String {
            val trimmed = prompt.trim().replace("\n", " ")
            if (trimmed.isEmpty()) return DEFAULT_TITLE
            return trimmed.take(40) + if (trimmed.length > 40) "…" else ""
        }
    }
}
