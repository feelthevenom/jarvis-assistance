package com.personalassistant.jarvis.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personalassistant.jarvis.ui.theme.LocalConciergePalette
import com.personalassistant.jarvis.voice.VoicePhase
import com.personalassistant.jarvis.voice.VoiceUiState

@Composable
fun VoiceScreen(
    voiceState: VoiceUiState,
    isThinking: Boolean,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    val phase = when {
        !micPermissionGranted -> VoicePhase.Error
        isThinking -> VoicePhase.Thinking
        else -> voiceState.phase
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(palette.voiceBackground, palette.background),
                    radius = 1100f,
                ),
            ),
    ) {
        AnimatedGlow(active = phase == VoicePhase.Listening || phase == VoicePhase.Speaking)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.22f))
            Orb(phase = phase)
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = phase.label(micPermissionGranted),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.text,
                ),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = transcriptText(voiceState, phase, micPermissionGranted),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = palette.mutedText,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            ActionRow(
                phase = phase,
                micPermissionGranted = micPermissionGranted,
                onRequestPermission = onRequestPermission,
                onStartListening = onStartListening,
                onStopListening = onStopListening,
                onClose = onClose,
            )
            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}

@Composable
private fun AnimatedGlow(active: Boolean) {
    val palette = LocalConciergePalette.current
    val transition = rememberInfiniteTransition(label = "voice-glow")
    val scale by transition.animateFloat(
        initialValue = if (active) 0.85f else 1f,
        targetValue = if (active) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow-scale",
    )
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(scale)
                .blur(56.dp)
                .clip(CircleShape)
                .background(palette.voiceGlow.copy(alpha = if (active) 0.34f else 0.18f)),
        )
    }
}

@Composable
private fun Orb(phase: VoicePhase) {
    val palette = LocalConciergePalette.current
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (phase == VoicePhase.Listening || phase == VoicePhase.Speaking) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-pulse",
    )
    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(pulse)
            .clip(CircleShape)
            .background(palette.voiceOrb),
    )
}

@Composable
private fun ActionRow(
    phase: VoicePhase,
    micPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = LocalConciergePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(palette.chip)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close voice",
                tint = palette.text,
                modifier = Modifier.size(26.dp),
            )
        }

        val (icon, action, contentDesc) = when {
            !micPermissionGranted -> Triple(Icons.Rounded.MicOff, onRequestPermission, "Grant microphone")
            phase == VoicePhase.Listening -> Triple(Icons.Rounded.MicOff, onStopListening, "Stop listening")
            else -> Triple(Icons.Rounded.Mic, onStartListening, "Start listening")
        }
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(palette.userBubble)
                .clickable(onClick = action),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = palette.userBubbleText,
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(modifier = Modifier.width(58.dp))
    }
}

private fun VoicePhase.label(micPermissionGranted: Boolean): String {
    if (!micPermissionGranted) return "Microphone access needed"
    return when (this) {
        VoicePhase.Idle -> "Tap to talk"
        VoicePhase.Listening -> "Listening…"
        VoicePhase.Thinking -> "Thinking…"
        VoicePhase.Speaking -> "Speaking…"
        VoicePhase.Error -> "Try again"
    }
}

private fun transcriptText(
    voice: VoiceUiState,
    phase: VoicePhase,
    micPermissionGranted: Boolean,
): String {
    if (!micPermissionGranted) {
        return "Concierge needs your microphone to capture voice prompts. The audio stays on this device."
    }
    if (voice.error != null && phase == VoicePhase.Error) return voice.error
    if (phase == VoicePhase.Listening) {
        return voice.partial.takeIf { it.isNotBlank() }
            ?: "Speak naturally. I will reply when you pause."
    }
    if (voice.transcript.isNotBlank()) return voice.transcript
    return "Tap the mic to start a real-time voice conversation."
}
