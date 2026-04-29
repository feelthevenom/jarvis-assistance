package com.personalassistant.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppTheme { Light, Dark, Neon }

val LocalConciergePalette = staticCompositionLocalOf { LightPalette }

private fun AppTheme.palette(): ConciergePalette = when (this) {
    AppTheme.Light -> LightPalette
    AppTheme.Dark -> DarkPalette
    AppTheme.Neon -> NeonPalette
}

@Composable
fun JarvisTheme(
    appTheme: AppTheme = AppTheme.Light,
    content: @Composable () -> Unit,
) {
    val palette = appTheme.palette()
    val colorScheme = when (appTheme) {
        AppTheme.Light -> lightColorScheme(
            primary = palette.userBubble,
            onPrimary = palette.userBubbleText,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
        )
        AppTheme.Dark, AppTheme.Neon -> darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
        )
    }

    CompositionLocalProvider(LocalConciergePalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
