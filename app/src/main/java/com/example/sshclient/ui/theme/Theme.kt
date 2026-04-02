package com.example.sshclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = Emerald300,
    secondary = Slate500,
    background = SurfaceDark,
    surface = Slate900
)

private val LightScheme = lightColorScheme(
    primary = Emerald500,
    secondary = Slate700,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White
)

@Composable
fun SshClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}
