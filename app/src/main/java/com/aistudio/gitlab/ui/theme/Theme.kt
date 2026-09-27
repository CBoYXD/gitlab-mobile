package com.aistudio.gitlab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

/**
 * Radii are concentric: an element nested inside a padded container uses
 * outer radius − padding. Cards (16) hold rows padded by 4 → 12; chips inside rows use 8.
 */
object GlRadius {
  val card = 16.dp
  val inner = 12.dp
  val control = 12.dp
  val chip = 8.dp
  val badge = 6.dp
}

object GlSpace {
  /** Layout margin on phones. */
  val margin = 16.dp
  /** Gap inside a group. */
  val tight = 8.dp
  /** Gap between groups: at least 2× the intra-group gap. */
  val group = 24.dp
  /** Minimum touch target (Android and WCAG AAA). */
  val touch = 48.dp
}

object Gl {
  val colors: GlColors
    @Composable @ReadOnlyComposable get() = LocalGlColors.current
  val type: GlType
    @Composable @ReadOnlyComposable get() = LocalGlType.current
  val reducedMotion: Boolean
    @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}

private fun materialScheme(c: GlColors) = if (c.isDark) {
  darkColorScheme(
    primary = c.primarySolid,
    onPrimary = c.onPrimarySolid,
    primaryContainer = c.surface3,
    onPrimaryContainer = c.textPrimary,
    secondary = c.textSecondary,
    onSecondary = c.surface,
    secondaryContainer = c.surface3,
    onSecondaryContainer = c.textPrimary,
    tertiary = c.info,
    background = c.bg,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surface2,
    onSurfaceVariant = c.textSecondary,
    surfaceContainerLowest = c.bg,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface2,
    surfaceContainerHighest = c.surface3,
    inverseSurface = c.textPrimary,
    inverseOnSurface = c.surface,
    outline = c.borderStrong,
    outlineVariant = c.border,
    error = c.danger,
    onError = c.surface,
    errorContainer = c.dangerBg,
    onErrorContainer = c.danger,
    scrim = c.scrim,
  )
} else {
  lightColorScheme(
    primary = c.primarySolid,
    onPrimary = c.onPrimarySolid,
    primaryContainer = c.surface3,
    onPrimaryContainer = c.textPrimary,
    secondary = c.textSecondary,
    onSecondary = c.surface,
    secondaryContainer = c.surface3,
    onSecondaryContainer = c.textPrimary,
    tertiary = c.info,
    background = c.bg,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surface2,
    onSurfaceVariant = c.textSecondary,
    surfaceContainerLowest = c.surface,
    surfaceContainerLow = c.bg,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface,
    surfaceContainerHighest = c.surface2,
    inverseSurface = c.textPrimary,
    inverseOnSurface = c.surface,
    outline = c.borderStrong,
    outlineVariant = c.border,
    error = c.danger,
    onError = c.surface,
    errorContainer = c.dangerBg,
    onErrorContainer = c.danger,
    scrim = c.scrim,
  )
}

private val MaterialShapes = Shapes(
  extraSmall = RoundedCornerShape(GlRadius.badge),
  small = RoundedCornerShape(GlRadius.chip),
  medium = RoundedCornerShape(GlRadius.control),
  large = RoundedCornerShape(GlRadius.card),
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun GitLabTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  reducedMotion: Boolean = rememberSystemReducedMotion(),
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) DarkGlColors else LightGlColors
  CompositionLocalProvider(
    LocalGlColors provides colors,
    LocalGlType provides GlTypography,
    LocalReducedMotion provides reducedMotion,
  ) {
    MaterialTheme(
      colorScheme = materialScheme(colors),
      typography = MaterialTypography,
      shapes = MaterialShapes,
      content = content,
    )
  }
}
