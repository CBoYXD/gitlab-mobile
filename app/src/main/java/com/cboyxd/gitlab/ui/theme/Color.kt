package com.cboyxd.gitlab.ui.theme

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
 * Status and accent colors follow GitLab's Pajamas design system (confirm blue, GitLab greens,
 * reds and purples) on a near-black dark theme; the structure follows GitHub Mobile: flat lists
 * on [bg], darker [sunken] bands between sections, blue [accent] for toolbar actions and colored
 * tiles (see [GlTile]) in front of navigation rows.
 */
@Immutable
data class GlColors(
  val isDark: Boolean,
  val bg: Color,
  /** Darker band between sections and behind page headers. */
  val sunken: Color,
  val surface: Color,
  val surface2: Color,
  val surface3: Color,
  val border: Color,
  val borderStrong: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  /** Toolbar icons, text buttons and selected navigation. */
  val accent: Color,
  val accentBg: Color,
  val primarySolid: Color,
  val onPrimarySolid: Color,
  val success: Color,
  val successBg: Color,
  val successSolid: Color,
  val danger: Color,
  val dangerBg: Color,
  val dangerSolid: Color,
  val warning: Color,
  val warningBg: Color,
  val info: Color,
  val infoBg: Color,
  val merged: Color,
  val mergedBg: Color,
  val mergedSolid: Color,
  val scrim: Color,
  /** Syntax roles. Own tokens so code colors never read as status. */
  val syntaxKeyword: Color,
  val syntaxString: Color,
  val syntaxLiteral: Color,
  val syntaxComment: Color,
  val syntaxMeta: Color,
  val codeBg: Color,
  /** Selected lines in the file viewer. */
  val lineHighlight: Color,
  val diffAddBg: Color,
  val diffAddWordBg: Color,
  val diffRemoveBg: Color,
  val diffRemoveWordBg: Color,
  val diffHunkBg: Color,
  /** Links inside prose; always underlined too, so color is never the only cue. */
  val link: Color,
  /** 1px outline around images: pure black or white at 10%, never a tinted neutral. */
  val imageOutline: Color,
)

/** Tile fills behind navigation icons, the same in both themes; icons on them are white. */
object GlTile {
  val green = Color(0xFF2DA160)
  val blue = Color(0xFF1F75CB)
  val yellow = Color(0xFFE9A820)
  val orange = Color(0xFFFC6D26)
  val red = Color(0xFFDD2B0E)
  val purple = Color(0xFF7759C2)
  val gray = Color(0xFF737278)
  val slate = Color(0xFF45444B)
}

val LightGlColors = GlColors(
  isDark = false,
  bg = Color(0xFFFFFFFF),
  sunken = Color(0xFFF1F1F3),
  surface = Color(0xFFFBFAFD),
  surface2 = Color(0xFFF0F0F2),
  surface3 = Color(0xFFE4E4E7),
  border = Color(0xFFE4E4E7),
  borderStrong = Color(0xFFBFBFC3),
  textPrimary = Color(0xFF1F1E24),
  textSecondary = Color(0xFF535158),
  textTertiary = Color(0xFF6B6A70),
  accent = Color(0xFF1F75CB),
  accentBg = Color(0xFFE9F3FC),
  primarySolid = Color(0xFF1F75CB),
  onPrimarySolid = Color(0xFFFFFFFF),
  success = Color(0xFF108548),
  successBg = Color(0xFFE6F6EC),
  successSolid = Color(0xFF108548),
  danger = Color(0xFFC91C00),
  dangerBg = Color(0xFFFCF1EF),
  dangerSolid = Color(0xFFDD2B0E),
  warning = Color(0xFF9E5400),
  warningBg = Color(0xFFFDF1DD),
  info = Color(0xFF1F75CB),
  infoBg = Color(0xFFE9F3FC),
  merged = Color(0xFF6F4FC0),
  mergedBg = Color(0xFFF4F0FF),
  mergedSolid = Color(0xFF7759C2),
  scrim = Color(0x66000000),
  syntaxKeyword = Color(0xFFCF222E),
  syntaxString = Color(0xFF0A3069),
  syntaxLiteral = Color(0xFF0550AE),
  syntaxComment = Color(0xFF6E7781),
  syntaxMeta = Color(0xFF953800),
  codeBg = Color(0xFFF6F6F8),
  lineHighlight = Color(0xFFFFF4CC),
  diffAddBg = Color(0xFFE6F6EC),
  diffAddWordBg = Color(0xFFB4E5C5),
  diffRemoveBg = Color(0xFFFCEDEA),
  diffRemoveWordBg = Color(0xFFF7C4BC),
  diffHunkBg = Color(0xFFE9F3FC),
  link = Color(0xFF1F75CB),
  imageOutline = Color(0x1A000000),
)

val DarkGlColors = GlColors(
  isDark = true,
  bg = Color(0xFF0E0E11),
  sunken = Color(0xFF040405),
  surface = Color(0xFF17171A),
  surface2 = Color(0xFF202024),
  surface3 = Color(0xFF2B2B30),
  border = Color(0xFF232327),
  borderStrong = Color(0xFF3F3F45),
  textPrimary = Color(0xFFF3F3F6),
  textSecondary = Color(0xFFB3B3BB),
  textTertiary = Color(0xFF9D9DA6),
  accent = Color(0xFF63A6E9),
  accentBg = Color(0xFF16273C),
  primarySolid = Color(0xFF1F75CB),
  onPrimarySolid = Color(0xFFFFFFFF),
  success = Color(0xFF52B87A),
  successBg = Color(0xFF0E2A1A),
  successSolid = Color(0xFF217645),
  danger = Color(0xFFF57F6C),
  dangerBg = Color(0xFF3A1611),
  dangerSolid = Color(0xFFC91C00),
  warning = Color(0xFFE9BE74),
  warningBg = Color(0xFF332410),
  info = Color(0xFF63A6E9),
  infoBg = Color(0xFF16273C),
  merged = Color(0xFFB9A5F8),
  mergedBg = Color(0xFF2A2140),
  mergedSolid = Color(0xFF7759C2),
  scrim = Color(0x99000000),
  syntaxKeyword = Color(0xFFFF7B72),
  syntaxString = Color(0xFFA5D6FF),
  syntaxLiteral = Color(0xFF79C0FF),
  syntaxComment = Color(0xFF9E9DA3),
  syntaxMeta = Color(0xFFFFA657),
  codeBg = Color(0xFF0E0E11),
  lineHighlight = Color(0xFF3F3719),
  diffAddBg = Color(0xFF082410),
  diffAddWordBg = Color(0xFF1F5A36),
  diffRemoveBg = Color(0xFF351019),
  diffRemoveWordBg = Color(0xFF6E2419),
  diffHunkBg = Color(0xFF14283F),
  link = Color(0xFF63A6E9),
  imageOutline = Color(0x1AFFFFFF),
)

val LocalGlColors = staticCompositionLocalOf { LightGlColors }
