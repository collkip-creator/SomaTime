package com.somatime.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SomaTeal = Color(0xFF0F766E)
val SomaEmerald = Color(0xFF059669)
val SomaAmber = Color(0xFFF59E0B)
val SomaBlue = Color(0xFF1D4ED8)
val SomaSlateBg = Color(0xFFF8FAFC)
val SomaSlateDark = Color(0xFF0F172A)

private val SomaLightColors = lightColorScheme(
    primary = SomaTeal,
    onPrimary = Color.White,
    secondary = SomaEmerald,
    onSecondary = Color.White,
    tertiary = SomaAmber,
    background = SomaSlateBg,
    onBackground = SomaSlateDark,
    surface = Color.White,
    onSurface = SomaSlateDark,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun SomaTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SomaLightColors,
        content = content
    )
}
