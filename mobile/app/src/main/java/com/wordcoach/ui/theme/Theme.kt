package com.wordcoach.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
  primary = WCPrimary,
  onPrimary = WCOnPrimary,
  secondary = WCSecondary,
  onSecondary = WCOnSecondary,
  surface = WCSurface,
  onSurface = WCOnSurface,
  surfaceVariant = WCSurfaceVariant,
  outline = WCOutline,
  error = WCError
)

private val DarkColors = darkColorScheme(
  primary = WCSecondary,
  onPrimary = WCOnPrimary,
  secondary = WCPrimary,
  onSecondary = WCOnSecondary,
  surface = WCOnSurface,
  onSurface = WCSurface,
  outline = WCOutline,
  error = WCError
)

private val WordCoachShapes = Shapes(
  extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
  small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
  medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
  large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
)

@Composable
fun WordCoachTheme(
  content: @Composable () -> Unit
) {
  val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
  MaterialTheme(
    colorScheme = colorScheme,
    typography = WordCoachTypography,
    shapes = WordCoachShapes,
    content = content
  )
}

