package com.aistudio.gitlab.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand primitives. Only the Tanuki mark uses them; interface color comes from GlColors below.
val TanukiOrange = Color(0xFFFC6D26)
val TanukiLightOrange = Color(0xFFFCA326)
val TanukiRed = Color(0xFFE24329)

/**
 * Semantic color roles. Components reference these, never raw hex values.
 *
 * Ramps were generated in OKLCH (neutral hue 285) and every text pair was measured against the
 * surfaces it renders on: text ≥ 14:1, textSecondary ≥ 6.3:1, textTertiary ≥ 4.8:1, status
 * foregrounds ≥ 4.8:1 on bg, surface, surface2 and their own tinted background, in both themes.
 *
 * Color carries status only. The primary action is an inverted neutral fill (textPrimary as the
 * background), so no hue competes with the status palette.
 */
@Immutable
data class GlColors(
  val isDark: Boolean,
  val bg: Color,
  val surface: Color,
  val surface2: Color,
  val surface3: Color,
  val border: Color,
  val borderStrong: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val primarySolid: Color,
  val onPrimarySolid: Color,
  val success: Color,
  val successBg: Color,
  val danger: Color,
  val dangerBg: Color,
  val dangerSolid: Color,
  val warning: Color,
  val warningBg: Color,
  val info: Color,
  val infoBg: Color,
  val merged: Color,
  val mergedBg: Color,
  val scrim: Color,
  /** Syntax roles. Own tokens so code colors never read as status. */
  val syntaxKeyword: Color,
  val syntaxString: Color,
  val syntaxLiteral: Color,
  val syntaxComment: Color,
  val syntaxMeta: Color,
  val codeBg: Color,
  /** Links inside prose; always underlined too, so color is never the only cue. */
  val link: Color,
  /** 1px outline around images: pure black or white at 10%, never a tinted neutral. */
  val imageOutline: Color,
)

val LightGlColors = GlColors(
  isDark = false,
  bg = Color(0xFFFAFAFC),
  surface = Color(0xFFFFFFFF),
  surface2 = Color(0xFFF3F3F6),
  surface3 = Color(0xFFEBEBEE),
  border = Color(0xFFE1E1E5),
  borderStrong = Color(0xFFCACAD0),
  textPrimary = Color(0xFF1A1A20),
  textSecondary = Color(0xFF54545D),
  textTertiary = Color(0xFF65656D),
  primarySolid = Color(0xFF1A1A20),
  onPrimarySolid = Color(0xFFFFFFFF),
  success = Color(0xFF1D7D3E),
  successBg = Color(0xFFE6FAE9),
  danger = Color(0xFFC2272D),
  dangerBg = Color(0xFFFEF0EE),
  dangerSolid = Color(0xFFC2272D),
  warning = Color(0xFF955E02),
  warningBg = Color(0xFFFFF3E6),
  info = Color(0xFF1F68BC),
  infoBg = Color(0xFFECF4FE),
  merged = Color(0xFF734DBE),
  mergedBg = Color(0xFFF4F1FE),
  scrim = Color(0x66000000),
  syntaxKeyword = Color(0xFF8A3FB8),
  syntaxString = Color(0xFF1B7A4B),
  syntaxLiteral = Color(0xFF1F63B0),
  syntaxComment = Color(0xFF6E6E78),
  syntaxMeta = Color(0xFF9A5B00),
  codeBg = Color(0xFFF6F6F9),
  link = Color(0xFF1F68BC),
  imageOutline = Color(0x1A000000),
)

val DarkGlColors = GlColors(
  isDark = true,
  bg = Color(0xFF0E0E11),
  surface = Color(0xFF17171A),
  surface2 = Color(0xFF202024),
  surface3 = Color(0xFF292A2E),
  border = Color(0xFF2B2B30),
  borderStrong = Color(0xFF3F3F45),
  textPrimary = Color(0xFFF3F3F6),
  textSecondary = Color(0xFFB3B3BB),
  textTertiary = Color(0xFF9D9DA6),
  primarySolid = Color(0xFFF3F3F6),
  onPrimarySolid = Color(0xFF17171A),
  success = Color(0xFF6FD087),
  successBg = Color(0xFF152D1A),
  danger = Color(0xFFFB817A),
  dangerBg = Color(0xFF3B1C1A),
  dangerSolid = Color(0xFFC92F33),
  warning = Color(0xFFFAB45F),
  warningBg = Color(0xFF37250E),
  info = Color(0xFF7CB4FC),
  infoBg = Color(0xFF15273E),
  merged = Color(0xFFB7A0FB),
  mergedBg = Color(0xFF29203E),
  scrim = Color(0x99000000),
  syntaxKeyword = Color(0xFFC9A2FF),
  syntaxString = Color(0xFF8DDBA6),
  syntaxLiteral = Color(0xFF8EC2FF),
  syntaxComment = Color(0xFF8C8C96),
  syntaxMeta = Color(0xFFF2C07A),
  codeBg = Color(0xFF131316),
  link = Color(0xFF7CB4FC),
  imageOutline = Color(0x1AFFFFFF),
)

val LocalGlColors = staticCompositionLocalOf { LightGlColors }
