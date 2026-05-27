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

import androidx.compose.ui.graphics.Color

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    tertiary = Color(0xFF2C2C2E),
    onTertiary = Color(0xFFE5E5EA),
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF121214),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFE5E5EA),
    outline = Color(0xFF2C2C2E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF121212),
    onPrimary = Color.White,
    secondary = Color(0xFF6E6E73),
    onSecondary = Color(0xFF121212),
    tertiary = Color(0xFFF2F2F7),
    onTertiary = Color(0xFF121212),
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF121212),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF121212),
    outline = Color(0xFFE5E5EA)
)

@Composable
fun animateColorScheme(targetScheme: ColorScheme): ColorScheme {
    val duration = 400
    val animSpec = tween<Color>(durationMillis = duration)
    return ColorScheme(
        primary = animateColorAsState(targetScheme.primary, animSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, animSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetScheme.primaryContainer, animSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, animSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetScheme.inversePrimary, animSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetScheme.secondary, animSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, animSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, animSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, animSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetScheme.tertiary, animSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, animSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetScheme.tertiaryContainer, animSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetScheme.onTertiaryContainer, animSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetScheme.background, animSpec, label = "background").value,
        onBackground = animateColorAsState(targetScheme.onBackground, animSpec, label = "onBackground").value,
        surface = animateColorAsState(targetScheme.surface, animSpec, label = "surface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, animSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, animSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, animSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetScheme.surfaceTint, animSpec, label = "surfaceTint").value,
        outline = animateColorAsState(targetScheme.outline, animSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetScheme.outlineVariant, animSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetScheme.scrim, animSpec, label = "scrim").value,
        error = animateColorAsState(targetScheme.error, animSpec, label = "error").value,
        onError = animateColorAsState(targetScheme.onError, animSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetScheme.errorContainer, animSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetScheme.onErrorContainer, animSpec, label = "onErrorContainer").value,
        inverseOnSurface = animateColorAsState(targetScheme.inverseOnSurface, animSpec, label = "inverseOnSurface").value,
        inverseSurface = animateColorAsState(targetScheme.inverseSurface, animSpec, label = "inverseSurface").value,
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // disabled to enforce premium hand-crafted styling
    content: @Composable () -> Unit,
) {
  val baseScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val animatedScheme = animateColorScheme(baseScheme)

  MaterialTheme(colorScheme = animatedScheme, typography = Typography, content = content)
}
