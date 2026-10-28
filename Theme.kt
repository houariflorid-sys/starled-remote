package com.starled.remote.hk320df.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CharcoalBackground = Color(0xFF14171C)
val RemotePlastic = Color(0xFF1F242D)
val ButtonDark = Color(0xFF2B313D)
val ButtonLight = Color(0xFF3E4657)
val PowerRed = Color(0xFFDC2626)
val MuteGreen = Color(0xFF10B981)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val AccentAmber = Color(0xFFF59E0B)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF94A3B8),
    tertiary = AccentAmber,
    background = CharcoalBackground,
    surface = RemotePlastic,
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun StarLEDHK320DFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
