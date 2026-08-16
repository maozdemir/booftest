package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ImmersiveDarkColorScheme =
  darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color(0xFF1C1B1F),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = CyanAccent,
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004D61),
    onSecondaryContainer = Color(0xFFBEE9FF),
    tertiary = IndigoGlow,
    onTertiary = Color(0xFF1E1A4D),
    background = DarkCanvas,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = GlassBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ImmersiveDarkColorScheme,
    typography = Typography,
    content = content
  )
}

