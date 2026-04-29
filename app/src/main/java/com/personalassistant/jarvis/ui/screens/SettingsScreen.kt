package com.personalassistant.jarvis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalassistant.jarvis.assistant.ModelStatus
import com.personalassistant.jarvis.data.AppSettings
import com.personalassistant.jarvis.ui.theme.AppTheme
import com.personalassistant.jarvis.ui.theme.LocalConciergePalette
import com.personalassistant.jarvis.ui.theme.NeonPalette
import com.personalassistant.jarvis.ui.theme.LightPalette
import com.personalassistant.jarvis.ui.theme.DarkPalette
import com.personalassistant.jarvis.ui.theme.ConciergePalette

@Composable
fun SettingsScreen(
    settings: AppSettings,
    modelStatus: ModelStatus,
    chatStorageBytes: Long,
    expectedModelPath: String,
    modelRepoUrl: String,
    modelDownloadUrl: String,
    modelSizeBytes: Long,
    onSettingsChange: (AppSettings) -> Unit,
    onClearChats: () -> Unit,
    onClearCache: () -> Unit,
    onDownloadModel: () -> Unit,
    contentPadding: PaddingValues,
) {
    val palette = LocalConciergePalette.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item { SettingsHeader() }
        item { SectionTitle("Appearance") }
        item {
            AppearancePicker(
                current = settings.theme,
                onChange = { onSettingsChange(settings.copy(theme = it)) },
            )
        }
        item { Spacer(modifier = Modifier.height(36.dp)) }
        item { SectionTitle("Notifications") }
        item {
            ToggleGroup(
                rows = listOf(
                    ToggleRow(
                        title = "Response sounds",
                        subtitle = "Play a soft chime when a reply finishes",
                        icon = Icons.Rounded.NotificationsNone,
                        iconBackground = palette.accentSoft,
                        iconTint = palette.accent,
                        checked = settings.responseSounds,
                        onCheckedChange = { onSettingsChange(settings.copy(responseSounds = it)) },
                    ),
                    ToggleRow(
                        title = "Voice auto-speak",
                        subtitle = "Read voice replies aloud through text-to-speech",
                        icon = Icons.Rounded.NotificationsNone,
                        iconBackground = palette.accentSoft,
                        iconTint = palette.accent,
                        checked = settings.voiceAutoSpeak,
                        onCheckedChange = { onSettingsChange(settings.copy(voiceAutoSpeak = it)) },
                    ),
                    ToggleRow(
                        title = "History reminders",
                        subtitle = "Nudge me to revisit recent chats",
                        icon = Icons.Rounded.NotificationsNone,
                        iconBackground = palette.accentSoft,
                        iconTint = palette.accent,
                        checked = settings.historyReminders,
                        onCheckedChange = { onSettingsChange(settings.copy(historyReminders = it)) },
                    ),
                ),
            )
        }
        item { Spacer(modifier = Modifier.height(36.dp)) }
        item { SectionTitle("Privacy") }
        item {
            ToggleGroup(
                rows = listOf(
                    ToggleRow(
                        title = "Save chat history",
                        subtitle = "Keep conversations on this device",
                        icon = Icons.Rounded.Lock,
                        iconBackground = Color(0xFFEBD8F6),
                        iconTint = Color(0xFF6E3B95),
                        checked = settings.saveHistory,
                        onCheckedChange = { onSettingsChange(settings.copy(saveHistory = it)) },
                    ),
                    ToggleRow(
                        title = "Store voice clips",
                        subtitle = "Audio is processed locally; recordings are not kept by default",
                        icon = Icons.Rounded.Lock,
                        iconBackground = Color(0xFFEBD8F6),
                        iconTint = Color(0xFF6E3B95),
                        checked = settings.storeAudio,
                        onCheckedChange = { onSettingsChange(settings.copy(storeAudio = it)) },
                    ),
                    ToggleRow(
                        title = "Anonymous diagnostics",
                        subtitle = "Off by default - the model and history stay on your phone",
                        icon = Icons.Rounded.Lock,
                        iconBackground = Color(0xFFEBD8F6),
                        iconTint = Color(0xFF6E3B95),
                        checked = settings.analyticsEnabled,
                        onCheckedChange = { onSettingsChange(settings.copy(analyticsEnabled = it)) },
                    ),
                ),
            )
            Spacer(modifier = Modifier.height(8.dp))
            ConfirmActionRow(
                label = "Clear all chat memory",
                description = "Removes every saved conversation from this device",
                onConfirm = onClearChats,
                icon = Icons.Rounded.DeleteSweep,
            )
        }
        item { Spacer(modifier = Modifier.height(36.dp)) }
        item { SectionTitle("Data & Storage") }
        item {
            DataStorageGroup(
                modelStatus = modelStatus,
                expectedModelPath = expectedModelPath,
                modelRepoUrl = modelRepoUrl,
                modelDownloadUrl = modelDownloadUrl,
                modelSizeBytes = modelSizeBytes,
                chatStorageBytes = chatStorageBytes,
                cacheModel = settings.cacheModelInMemory,
                onCacheModelChange = { onSettingsChange(settings.copy(cacheModelInMemory = it)) },
                onClearCache = onClearCache,
                onDownloadModel = onDownloadModel,
            )
        }
        item {
            Text(
                text = "@feelthevenom",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.mutedText,
                ),
            )
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun SettingsHeader() {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(30.dp))
        Text(
            text = "Settings",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    val palette = LocalConciergePalette.current
    Text(
        text = text,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 12.dp),
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = palette.mutedText,
        ),
    )
}

@Composable
private fun AppearancePicker(current: AppTheme, onChange: (AppTheme) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeCard("Light", AppTheme.Light, LightPalette, current == AppTheme.Light) { onChange(AppTheme.Light) }
        ThemeCard("Dark", AppTheme.Dark, DarkPalette, current == AppTheme.Dark) { onChange(AppTheme.Dark) }
        ThemeCard("Neon", AppTheme.Neon, NeonPalette, current == AppTheme.Neon) { onChange(AppTheme.Neon) }
    }
}

@Composable
private fun RowScope.ThemeCard(
    label: String,
    @Suppress("UNUSED_PARAMETER") theme: AppTheme,
    preview: ConciergePalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(preview.surface)
                .border(
                    BorderStroke(if (selected) 2.dp else 1.dp, if (selected) palette.text else preview.border),
                    RoundedCornerShape(12.dp),
                )
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(preview.assistantBubble),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(preview.surfaceVariant),
                )
            }
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(preview.userBubble),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(preview.assistantBubble),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(palette.text),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.surface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = palette.text,
            ),
        )
    }
}

private data class ToggleRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun ToggleGroup(rows: List<ToggleRow>) {
    val palette = LocalConciergePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(14.dp))
            .background(palette.surface),
    ) {
        rows.forEachIndexed { index, row ->
            ToggleRowView(row)
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(start = 70.dp)
                        .background(palette.border),
                )
            }
        }
    }
}

@Composable
private fun ToggleRowView(row: ToggleRow) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(row.iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = row.iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.text,
                ),
            )
            Text(
                text = row.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = palette.mutedText,
                ),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.surface,
                checkedTrackColor = palette.userBubble,
                uncheckedThumbColor = palette.surface,
                uncheckedTrackColor = palette.chip,
            ),
        )
    }
}

@Composable
private fun ConfirmActionRow(
    label: String,
    description: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    var armed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(14.dp))
            .background(palette.surface)
            .clickable {
                if (armed) {
                    armed = false
                    onConfirm()
                } else armed = true
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(palette.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.danger,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (armed) "Tap again to confirm" else label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (armed) palette.danger else palette.text,
                ),
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = palette.mutedText,
                ),
            )
        }
    }
}

@Composable
private fun DataStorageGroup(
    modelStatus: ModelStatus,
    expectedModelPath: String,
    modelRepoUrl: String,
    modelDownloadUrl: String,
    modelSizeBytes: Long,
    chatStorageBytes: Long,
    cacheModel: Boolean,
    onCacheModelChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onDownloadModel: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, palette.border), RoundedCornerShape(14.dp))
            .background(palette.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InfoRow(
            icon = Icons.Rounded.DataUsage,
            iconBackground = palette.accentSoft,
            iconTint = palette.accent,
            title = "Local model",
            value = modelStatusLabel(modelStatus),
            footer = modelFooter(modelStatus, expectedModelPath, modelRepoUrl, modelDownloadUrl, modelSizeBytes),
        )
        if (modelStatus is ModelStatus.Missing) {
            Text(
                text = "Download Gemma 4 E2B",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.userBubble)
                    .clickable(onClick = onDownloadModel)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.userBubbleText,
                ),
            )
        }
        InfoRow(
            icon = Icons.Rounded.DataUsage,
            iconBackground = palette.accentSoft,
            iconTint = palette.accent,
            title = "Saved chat memory",
            value = formatBytes(chatStorageBytes),
            footer = "Your conversations live only on this device.",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Keep model loaded in memory",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.text,
                    ),
                )
                Text(
                    text = "Faster replies, more RAM usage.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        color = palette.mutedText,
                    ),
                )
            }
            Switch(
                checked = cacheModel,
                onCheckedChange = onCacheModelChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.surface,
                    checkedTrackColor = palette.userBubble,
                    uncheckedThumbColor = palette.surface,
                    uncheckedTrackColor = palette.chip,
                ),
            )
        }
        Text(
            text = "Clear cached model state",
            modifier = Modifier
                .clip(CircleShape)
                .background(palette.chip)
                .clickable(onClick = onClearCache)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            ),
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    value: String,
    footer: String,
) {
    val palette = LocalConciergePalette.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.text,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.accent,
                    ),
                )
            }
            Text(
                text = footer,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = palette.mutedText,
                ),
            )
        }
    }
}

private fun modelStatusLabel(status: ModelStatus): String = when (status) {
    ModelStatus.NotInitialized -> "Idle"
    ModelStatus.Initializing -> "Loading…"
    is ModelStatus.Downloading -> "${downloadPercent(status)}%"
    is ModelStatus.Missing -> "Missing"
    ModelStatus.Ready -> "Ready"
    is ModelStatus.Error -> "Error"
}

private fun modelFooter(
    status: ModelStatus,
    expectedModelPath: String,
    modelRepoUrl: String,
    modelDownloadUrl: String,
    modelSizeBytes: Long,
): String {
    val base = "Expected file: $expectedModelPath\nSize: ${formatBytes(modelSizeBytes)}\nSource: $modelRepoUrl\nDirect: $modelDownloadUrl"
    return when (status) {
        is ModelStatus.Downloading -> "Downloading ${formatBytes(status.downloadedBytes)} of ${formatBytes(status.totalBytes)}.\n$base"
        else -> base
    }
}

private fun downloadPercent(status: ModelStatus.Downloading): Int {
    if (status.totalBytes <= 0L) return 0
    return ((status.downloadedBytes * 100) / status.totalBytes).coerceIn(0, 100).toInt()
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}
