package com.cboyxd.gitlab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import com.cboyxd.gitlab.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Type scale named by use. Headings run tight (≈1.2) with slightly negative tracking; text that
 * wraps runs at 1.4–1.5. Nothing renders below 12sp or lighter than 400.
 */
@Immutable
data class GlType(
  val screenTitle: TextStyle,
  /** Big page headings: a project's name, an issue's title. */
  val headline: TextStyle,
  /** Title in the compact bar of pushed screens. */
  val topBar: TextStyle,
  val title: TextStyle,
  val sectionTitle: TextStyle,
  val rowTitle: TextStyle,
  val body: TextStyle,
  val bodySmall: TextStyle,
  val meta: TextStyle,
  val label: TextStyle,
  val badge: TextStyle,
  val caps: TextStyle,
  val mono: TextStyle,
  val monoSmall: TextStyle,
  /** Counters and durations: tabular figures so values don't shift width as they update. */
  val numeric: TextStyle,
)

/** Bundled so manufacturer and user font replacements cannot change code spacing. */
val CodeFont = FontFamily(
  Font(R.font.liberation_mono_regular, FontWeight.Normal),
  Font(R.font.liberation_mono_bold, FontWeight.Bold),
  Font(R.font.liberation_mono_italic, FontWeight.Normal, FontStyle.Italic),
  Font(R.font.liberation_mono_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

private val Sans = FontFamily.Default
private const val TABULAR = "tnum"

val GlTypography = GlType(
  screenTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
  headline = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.015).em),
  topBar = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
  title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
  sectionTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.005).em),
  rowTitle = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
  body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
  bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
  meta = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
  label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
  badge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
  caps = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.06.em),
  mono = TextStyle(fontFamily = CodeFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
  monoSmall = TextStyle(fontFamily = CodeFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
  numeric = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = TABULAR),
)

val LocalGlType = staticCompositionLocalOf { GlTypography }

/** Material slots mapped onto the same scale so stock components (dialogs, fields, snackbars) match. */
val MaterialTypography = Typography(
  headlineLarge = GlTypography.screenTitle,
  headlineMedium = GlTypography.screenTitle,
  headlineSmall = GlTypography.title,
  titleLarge = GlTypography.title,
  titleMedium = GlTypography.sectionTitle,
  titleSmall = GlTypography.rowTitle,
  bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
  bodyMedium = GlTypography.bodySmall,
  bodySmall = GlTypography.meta,
  labelLarge = GlTypography.label,
  labelMedium = GlTypography.badge,
  labelSmall = GlTypography.badge,
)
