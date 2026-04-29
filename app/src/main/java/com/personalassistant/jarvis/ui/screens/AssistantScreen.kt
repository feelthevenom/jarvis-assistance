package com.personalassistant.jarvis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalassistant.jarvis.assistant.ModelStatus
import com.personalassistant.jarvis.data.ChatMessage
import com.personalassistant.jarvis.ui.components.AssistantHero
import com.personalassistant.jarvis.ui.components.ChatBubble
import com.personalassistant.jarvis.ui.components.ChatComposer
import com.personalassistant.jarvis.ui.components.ModelStatusCard
import com.personalassistant.jarvis.ui.components.ProfileBadge
import com.personalassistant.jarvis.ui.theme.LocalConciergePalette

@Composable
fun AssistantScreen(
    messages: List<ChatMessage>,
    composerValue: String,
    isThinking: Boolean,
    modelStatus: ModelStatus,
    modelRepoUrl: String,
    modelDownloadUrl: String,
    modelSizeBytes: Long,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onProfileClick: () -> Unit,
    onRetryEngine: () -> Unit,
    onDownloadModel: () -> Unit,
    contentPadding: PaddingValues,
) {
    val palette = LocalConciergePalette.current
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size + 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(top = contentPadding.calculateTopPadding())
            .imePadding(),
    ) {
        AssistantTopBar(
            onMenuClick = onMenuClick,
            onNewChat = onNewChat,
            onProfileClick = onProfileClick,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = if (messages.isEmpty()) 60.dp else 18.dp,
                bottom = 18.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (messages.isEmpty()) {
                item { AssistantHero() }
            }

            statusBanner(
                status = modelStatus,
                modelRepoUrl = modelRepoUrl,
                modelDownloadUrl = modelDownloadUrl,
                modelSizeBytes = modelSizeBytes,
                onRetry = onRetryEngine,
                onDownloadModel = onDownloadModel,
            )?.let { banner ->
                item { banner() }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }

            if (messages.isEmpty()) {
                item { EmptyChatHint() }
            }
        }

        ChatComposer(
            value = composerValue,
            onValueChange = onComposerChange,
            onVoiceClick = onVoiceClick,
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick,
            onSend = onSend,
            sendEnabled = composerValue.isNotBlank() && !isThinking,
            modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        )
    }
}

@Composable
private fun AssistantTopBar(
    onMenuClick: () -> Unit,
    onNewChat: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Rounded.Menu,
                contentDescription = "Open chat history",
                tint = palette.text,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "Thragg",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
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
        Box(modifier = Modifier.padding(horizontal = 6.dp)) {
            ProfileBadge(modifier = Modifier.clickable(onClick = onProfileClick))
        }
    }
}

@Composable
private fun EmptyChatHint() {
    val palette = LocalConciergePalette.current
    Text(
        text = "Tip: open the menu icon to switch between past chats. Each thread keeps its own memory.",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = palette.lightText,
        ),
    )
}

private fun statusBanner(
    status: ModelStatus,
    modelRepoUrl: String,
    modelDownloadUrl: String,
    modelSizeBytes: Long,
    onRetry: () -> Unit,
    onDownloadModel: () -> Unit,
): (@Composable () -> Unit)? = when (status) {
    is ModelStatus.Missing -> ({
        ModelStatusCard(
            title = "Download Gemma 4 E2B",
            description = "Gallery uses Gemma-4-E2B-it from the Android 1.0.12 allowlist.\n\nModel: gemma-4-E2B-it.litertlm (${formatBytes(modelSizeBytes)})\nSource: $modelRepoUrl\nDownload: $modelDownloadUrl\n\nThragg will save it to:\n${status.expectedPath}",
            actionLabel = "Download model",
            onAction = onDownloadModel,
            showRetry = false,
        )
    })
    is ModelStatus.Downloading -> ({
        ModelStatusCard(
            title = "Downloading Gemma 4 E2B",
            description = "${formatBytes(status.downloadedBytes)} of ${formatBytes(status.totalBytes)} downloaded.\n\nKeep the app open until the download finishes.",
        )
    })
    is ModelStatus.Error -> ({
        ModelStatusCard(
            title = "Gemma engine error",
            description = status.message,
            actionLabel = "Retry",
            onAction = onRetry,
            showRetry = true,
            onRetry = onRetry,
        )
    })
    ModelStatus.Initializing -> ({
        ModelStatusCard(
            title = "Loading on-device Gemma",
            description = "First-time load can take a few seconds while the model is mapped into memory.",
        )
    })
    ModelStatus.Ready, ModelStatus.NotInitialized -> null
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    if (gb >= 1.0) return String.format("%.2f GB", gb)
    val mb = bytes / 1024.0 / 1024.0
    return String.format("%.1f MB", mb)
}
