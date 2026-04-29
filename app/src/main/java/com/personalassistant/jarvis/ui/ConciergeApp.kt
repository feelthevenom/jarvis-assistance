package com.personalassistant.jarvis.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personalassistant.jarvis.ui.components.ProfileEditorDialog
import com.personalassistant.jarvis.ui.screens.AssistantScreen
import com.personalassistant.jarvis.ui.screens.HistoryDrawer
import com.personalassistant.jarvis.ui.screens.SettingsScreen
import com.personalassistant.jarvis.ui.screens.VoiceScreen
import com.personalassistant.jarvis.ui.theme.JarvisTheme
import com.personalassistant.jarvis.ui.theme.LocalConciergePalette
import java.io.File
import java.util.UUID

@Composable
fun ConciergeApp(viewModel: ConciergeViewModel = viewModel()) {
    val state by viewModel.ui.collectAsState()

    JarvisTheme(appTheme = state.settings.theme) {
        val context = LocalContext.current
        var micPermissionGranted by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED,
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            micPermissionGranted = granted
            viewModel.handleMicrophonePermissionResult(granted)
            if (granted && state.tab == Tab.Voice) {
                viewModel.startVoiceCapture()
            }
        }
        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri ->
            uri?.let { copyImageToPrivateStorage(context, it)?.let(viewModel::sendImagePrompt) }
        }
        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview(),
        ) { bitmap ->
            bitmap?.let { saveCameraBitmap(context, it)?.let(viewModel::sendImagePrompt) }
        }

        LaunchedEffect(state.tab) {
            if (state.tab == Tab.Voice) {
                if (!micPermissionGranted) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viewModel.startVoiceCapture()
                }
            } else {
                viewModel.stopVoiceCapture()
                viewModel.stopVoicePlayback()
            }
        }

        val systemPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LocalConciergePalette.current.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = state.tab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "tab-content",
                        ) { tab ->
                            when (tab) {
                                Tab.Assistant -> AssistantScreen(
                                    messages = state.currentMessages,
                                    composerValue = state.composer,
                                    isThinking = state.isThinking,
                                    modelStatus = state.modelStatus,
                                    modelRepoUrl = viewModel.modelRepoUrl,
                                    modelDownloadUrl = viewModel.modelDownloadUrl,
                                    modelSizeBytes = viewModel.modelSizeBytes,
                                    onComposerChange = viewModel::updateComposer,
                                    onSend = viewModel::sendComposerPrompt,
                                    onVoiceClick = { viewModel.selectTab(Tab.Voice) },
                                    onCameraClick = { cameraLauncher.launch(null) },
                                    onGalleryClick = { galleryLauncher.launch("image/*") },
                                    onMenuClick = { viewModel.setDrawerOpen(true) },
                                    onNewChat = viewModel::newChat,
                                    onProfileClick = { viewModel.setProfileEditorOpen(true) },
                                    onRetryEngine = viewModel::retryEngine,
                                    onDownloadModel = viewModel::downloadModel,
                                    contentPadding = systemPadding,
                                )
                                Tab.Voice -> VoiceScreen(
                                    voiceState = state.voice,
                                    isThinking = state.isThinking,
                                    micPermissionGranted = micPermissionGranted,
                                    onRequestPermission = {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    },
                                    onStartListening = viewModel::startVoiceCapture,
                                    onStopListening = viewModel::stopVoiceCapture,
                                    onClose = { viewModel.selectTab(Tab.Assistant) },
                                )
                                Tab.Settings -> SettingsScreen(
                                    settings = state.settings,
                                    modelStatus = state.modelStatus,
                                    chatStorageBytes = viewModel.chatStorageBytes(),
                                    expectedModelPath = viewModel.expectedModelPath,
                                    modelRepoUrl = viewModel.modelRepoUrl,
                                    modelDownloadUrl = viewModel.modelDownloadUrl,
                                    modelSizeBytes = viewModel.modelSizeBytes,
                                    onSettingsChange = { updated ->
                                        viewModel.updateSettings { updated }
                                    },
                                    onClearChats = viewModel::clearAllSessions,
                                    onClearCache = viewModel::reloadEngine,
                                    onDownloadModel = viewModel::downloadModel,
                                    contentPadding = systemPadding,
                                )
                            }
                        }
                    }

                    if (state.tab != Tab.Voice) {
                        BottomNavigation(
                            tab = state.tab,
                            onSelect = viewModel::selectTab,
                        )
                    }
                }

                HistoryDrawer(
                    open = state.drawerOpen,
                    sessions = state.sessions,
                    activeSessionId = state.currentSessionId,
                    contentPadding = systemPadding,
                    onClose = { viewModel.setDrawerOpen(false) },
                    onNewChat = viewModel::newChat,
                    onOpenSession = viewModel::openSession,
                    onDeleteSession = viewModel::deleteSession,
                )
                if (state.profileEditorOpen) {
                    ProfileEditorDialog(
                        profile = state.profile,
                        onDismiss = { viewModel.setProfileEditorOpen(false) },
                        onSave = viewModel::updateProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    tab: Tab,
    onSelect: (Tab) -> Unit,
) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(WindowInsets.navigationBars.asPaddingValues())
            .padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavItem("Assistant", Icons.Rounded.Tune, tab == Tab.Assistant) { onSelect(Tab.Assistant) }
        NavItem("Voice", Icons.Rounded.Mic, tab == Tab.Voice) { onSelect(Tab.Voice) }
        NavItem("Settings", Icons.Rounded.Settings, tab == Tab.Settings) { onSelect(Tab.Settings) }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    val iconTint by animateColorAsState(
        targetValue = if (selected) palette.text else palette.lightText,
        label = "nav-icon",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) palette.text else palette.lightText,
        label = "nav-label",
    )
    val verticalPadding by animateDpAsState(
        targetValue = if (selected) 8.dp else 4.dp,
        label = "nav-padding",
    )
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = labelTint,
            ),
        )
    }
}

private fun copyImageToPrivateStorage(context: Context, uri: Uri): String? {
    return runCatching {
        val imageDir = File(context.filesDir, "images").apply { mkdirs() }
        val target = File(imageDir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()
}

private fun saveCameraBitmap(context: Context, bitmap: Bitmap): String? {
    return runCatching {
        val imageDir = File(context.filesDir, "images").apply { mkdirs() }
        val target = File(imageDir, "${UUID.randomUUID()}.jpg")
        target.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        target.absolutePath
    }.getOrNull()
}
