package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  androidx.compose.material3.lightColorScheme(
    primary = ShieldCyan,
    secondary = SlateLight,
    tertiary = ShieldGreen,
    background = SlateBackground,
    surface = SlateMedium,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = SlateDark,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextLight,
    onSurface = TextLight
  )

private val LightColorScheme =
  androidx.compose.material3.lightColorScheme(
    primary = ShieldCyan,
    secondary = SlateLight,
    tertiary = ShieldGreen,
    background = SlateBackground,
    surface = SlateMedium,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = SlateDark,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextLight,
    onSurface = TextLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic colors to enforce the high-fidelity tactical slate theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
