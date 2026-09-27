package com.cboyxd.gitlab.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

object GlMotion {
  /** cubic-bezier(0.2, 0, 0, 1): enter and state changes. */
  val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

  /** ease-out: exits, softer than enters. */
  val EaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)

  /** High-frequency feedback (press, color, selection) stays at or under 150ms. */
  const val FAST = 120
  const val MEDIUM = 220

  const val PRESS_SCALE = 0.96f

  /** Contextual icon swap: scale 0.25 → 1, opacity 0 → 1, blur 4dp → 0. */
  const val ICON_ENTER_SCALE = 0.25f
  const val ICON_ENTER_BLUR_DP = 4f

  /** Exits move a small fixed distance, never their full height. */
  const val EXIT_OFFSET_DP = 8
}

/** True when the user disabled animations (Developer options or Accessibility → Remove animations). */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun rememberSystemReducedMotion(): Boolean {
  val context = LocalContext.current
  return remember(context) {
    val scale = runCatching {
      Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }.getOrDefault(1f)
    scale == 0f
  }
}

fun <T> glTween(durationMillis: Int, reducedMotion: Boolean, easing: Easing = GlMotion.Standard): FiniteAnimationSpec<T> =
  if (reducedMotion) snap() else tween(durationMillis, easing = easing)
