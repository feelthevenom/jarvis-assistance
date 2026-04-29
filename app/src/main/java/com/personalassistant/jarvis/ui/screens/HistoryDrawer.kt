package com.personalassistant.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalassistant.jarvis.data.ChatSession
import com.personalassistant.jarvis.ui.theme.LocalConciergePalette
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryDrawer(
    open: Boolean,
    sessions: List<ChatSession>,
    activeSessionId: String?,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    if (!open) return
    val palette = LocalConciergePalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClose),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(310.dp)
                .background(palette.surface)
                .clickable(enabled = false, onClick = {}),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                DrawerHeader(onClose = onClose, onNewChat = onNewChat)

                if (sessions.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                active = session.id == activeSessionId,
                                onClick = { onOpenSession(session.id) },
                                onDelete = { onDeleteSession(session.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(onClose: () -> Unit, onNewChat: () -> Unit) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close history",
                tint = palette.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "Chats",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            ),
        )
        IconButton(onClick = onNewChat) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New chat",
                tint = palette.text,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSession,
    active: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) palette.accentSoft else palette.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(palette.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = palette.text,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title.ifBlank { "New chat" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.text,
                ),
                maxLines = 1,
            )
            Text(
                text = formatTimestamp(session.updatedAt) +
                    if (session.preview.isNotBlank()) " · ${session.preview.take(40)}" else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = palette.mutedText,
                ),
                maxLines = 1,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = "Delete chat",
                tint = palette.lightText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val palette = LocalConciergePalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No saved chats yet",
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                color = palette.text,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Send a message and your conversations will appear here, like ChatGPT.",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = palette.mutedText,
            ),
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(timestamp))
}
