package com.cboyxd.gitlab.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import coil.request.ImageRequest
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlMotion
import com.cboyxd.gitlab.ui.theme.GlRadius
import com.cboyxd.gitlab.ui.theme.GlSpace
import com.cboyxd.gitlab.ui.theme.glTween

// ---------------------------------------------------------------------------------------------
// Interaction
// ---------------------------------------------------------------------------------------------

/**
 * Clickable with the platform ripple plus a 0.96 scale while pressed. The scale is skipped under
 * reduced motion; the ripple still shows the press.
 */
fun Modifier.pressable(
  onClick: () -> Unit,
  enabled: Boolean = true,
  role: Role = Role.Button,
  onClickLabel: String? = null,
  scaleOnPress: Boolean = true,
): Modifier = composed {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val reduced = Gl.reducedMotion
  val scale by animateFloatAsState(
    targetValue = if (pressed && scaleOnPress && enabled && !reduced) GlMotion.PRESS_SCALE else 1f,
    animationSpec = glTween(GlMotion.FAST, reduced),
    label = "press",
  )
  this
    .graphicsLayer { scaleX = scale; scaleY = scale }
    .clickable(
      interactionSource = interaction,
      indication = LocalIndication.current,
      enabled = enabled,
      role = role,
      onClickLabel = onClickLabel,
      onClick = onClick,
    )
}

// ---------------------------------------------------------------------------------------------
// Headers
// ---------------------------------------------------------------------------------------------

/** Large title used by the top-level tabs. */
@Composable
fun GlLargeHeader(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = GlSpace.margin, end = 4.dp, top = 12.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = Gl.type.screenTitle,
        color = Gl.colors.textPrimary,
        modifier = Modifier.semantics { heading() },
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
  }
}

/**
 * Compact bar for pushed screens: back, a bold title with an optional overline above it (the
 * project path on an issue, say) and a subtitle below, and blue actions. [showTitle] lets a page
 * that shows its own big heading keep the bar empty until that heading scrolls away.
 */
@Composable
fun GlTopBar(
  title: String,
  onBack: (() -> Unit)?,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  overline: String? = null,
  showTitle: Boolean = true,
  background: Color = Gl.colors.bg,
  navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
  navigationLabel: String = "Back",
  actions: @Composable RowScope.() -> Unit = {},
) {
  val reduced = Gl.reducedMotion
  val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, glTween(GlMotion.FAST, reduced), label = "bar-title")
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(background)
      .heightIn(min = 60.dp)
      .padding(start = if (onBack != null) 4.dp else GlSpace.margin, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (onBack != null) {
      IconButton(onClick = onBack) {
        Icon(navigationIcon, contentDescription = navigationLabel, tint = Gl.colors.textPrimary)
      }
      Spacer(Modifier.width(8.dp))
    }
    Column(modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha }) {
      if (!overline.isNullOrBlank()) {
        Text(
          text = overline,
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = title,
        style = Gl.type.topBar,
        color = Gl.colors.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically, content = actions)
  }
}

@Composable
fun GlIconAction(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tint: Color = Gl.colors.accent,
) {
  IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
    Icon(icon, contentDescription = contentDescription, tint = if (enabled) tint else Gl.colors.textTertiary)
  }
}

@Composable
fun GlSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  trailing: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(start = GlSpace.margin, end = GlSpace.margin, top = GlSpace.group, bottom = GlSpace.tight),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = Gl.type.sectionTitle,
      color = Gl.colors.textPrimary,
      modifier = Modifier.weight(1f).semantics { heading() },
    )
    trailing?.invoke()
  }
}

// ---------------------------------------------------------------------------------------------
// Surfaces
// ---------------------------------------------------------------------------------------------

/**
 * Grouped container. Depth comes from the surface step (surface on bg); the hairline border is
 * structure, kept at the low-contrast border token.
 */
@Composable
fun GlCard(
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(0.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(GlRadius.card))
      .background(Gl.colors.surface)
      .border(1.dp, Gl.colors.border, RoundedCornerShape(GlRadius.card))
      .padding(contentPadding),
    content = content,
  )
}

@Composable
fun GlDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
  HorizontalDivider(
    modifier = modifier.padding(start = inset),
    thickness = 1.dp,
    color = Gl.colors.border,
  )
}

/**
 * List row with a 56dp minimum height. Descendants merge into one accessibility node so a screen
 * reader announces the row once with its action.
 */
@Composable
fun GlRow(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  leading: (@Composable () -> Unit)? = null,
  trailing: (@Composable RowScope.() -> Unit)? = null,
  titleMaxLines: Int = 1,
  titleStyle: TextStyle = Gl.type.rowTitle,
  onClick: (() -> Unit)? = null,
  onClickLabel: String? = null,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .then(
        if (onClick != null) Modifier.clickable(onClick = onClick, onClickLabel = onClickLabel, role = Role.Button)
        else Modifier.semantics(mergeDescendants = true) {}
      )
      .defaultMinSize(minHeight = 56.dp)
      .padding(horizontal = GlSpace.margin, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (leading != null) {
      leading()
      Spacer(Modifier.width(16.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = titleStyle,
        color = Gl.colors.textPrimary,
        maxLines = titleMaxLines,
        overflow = TextOverflow.Ellipsis,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = Gl.type.meta,
          color = Gl.colors.textSecondary,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (trailing != null) {
      Spacer(Modifier.width(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
  }
}

/**
 * Full-width band that separates the sections of a page, darker than the page, as GitHub Mobile
 * does between a repository's header, its lists and its README.
 */
@Composable
fun GlSectionBand(modifier: Modifier = Modifier, height: Dp = 12.dp) {
  Box(modifier.fillMaxWidth().height(height).background(Gl.colors.sunken))
}

/** Colored rounded tile with a white icon, in front of navigation rows (Issues, Merge requests…). */
@Composable
fun ColorTile(icon: ImageVector, color: Color, modifier: Modifier = Modifier, size: Dp = 32.dp) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(8.dp))
      .background(color),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.6f))
  }
}

/** Neutral icon tile. Category is carried by the icon shape, not by color. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = Gl.colors.textPrimary, background: Color = Gl.colors.surface2) {
  Box(
    modifier = modifier
      .size(36.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(background),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
  }
}

@Composable
fun CountText(count: Int, modifier: Modifier = Modifier) {
  Text(
    text = count.toString(),
    style = Gl.type.numeric,
    color = Gl.colors.textSecondary,
    modifier = modifier,
  )
}

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

enum class GlButtonKind { Primary, Secondary, Danger, Ghost }

/**
 * Button that keeps its label while busy and shows a spinner beside it. It stays enabled until the
 * request starts; callers validate on submit.
 */
@Composable
fun GlButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  kind: GlButtonKind = GlButtonKind.Primary,
  enabled: Boolean = true,
  loading: Boolean = false,
  icon: ImageVector? = null,
) {
  val c = Gl.colors
  val (bg, fg, border) = when (kind) {
    GlButtonKind.Primary -> Triple(c.primarySolid, c.onPrimarySolid, null)
    GlButtonKind.Secondary -> Triple(c.surface2, c.textPrimary, c.border)
    GlButtonKind.Danger -> Triple(c.dangerSolid, Color.White, null)
    GlButtonKind.Ghost -> Triple(Color.Transparent, c.textPrimary, null)
  }
  val active = enabled && !loading
  val shape = RoundedCornerShape(GlRadius.control)
  Row(
    modifier = modifier
      .heightIn(min = GlSpace.touch)
      .clip(shape)
      .background(if (enabled) bg else c.surface3)
      .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
      .pressable(onClick = onClick, enabled = active)
      .padding(horizontal = 18.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val tint = if (enabled) fg else c.textTertiary
    if (loading) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = tint)
      Spacer(Modifier.width(8.dp))
    } else if (icon != null) {
      Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
    }
    Text(text = text, style = Gl.type.label, color = tint, maxLines = 1)
  }
}

// ---------------------------------------------------------------------------------------------
// Chips, badges, labels
// ---------------------------------------------------------------------------------------------

@Composable
fun <T> GlFilterRow(
  options: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyRow(
    modifier = modifier.fillMaxWidth(),
    contentPadding = PaddingValues(horizontal = GlSpace.margin, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(GlSpace.tight),
  ) {
    items(options, key = { label(it) }) { option ->
      GlChip(text = label(option), selected = option == selected, onClick = { onSelect(option) })
    }
  }
}

/** Selected chips take the accent outline and tint; the selected state is also announced. */
@Composable
fun GlChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val c = Gl.colors
  val shape = RoundedCornerShape(50)
  Box(
    modifier = modifier
      .heightIn(min = GlSpace.touch)
      .pressable(onClick = onClick, role = Role.Tab),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = text,
      style = Gl.type.label,
      color = if (selected) c.accent else c.textPrimary,
      maxLines = 1,
      modifier = Modifier
        .clip(shape)
        .background(if (selected) c.accentBg else c.surface2)
        .border(1.dp, if (selected) c.accent else c.border, shape)
        .padding(horizontal = 14.dp, vertical = 7.dp)
        .semantics { if (selected) contentDescription = "$text, selected" },
    )
  }
}

enum class Tone { Neutral, Success, Danger, Warning, Info, Merged }

@Composable
fun toneColors(tone: Tone): Pair<Color, Color> {
  val c = Gl.colors
  return when (tone) {
    Tone.Neutral -> c.textSecondary to c.surface2
    Tone.Success -> c.success to c.successBg
    Tone.Danger -> c.danger to c.dangerBg
    Tone.Warning -> c.warning to c.warningBg
    Tone.Info -> c.info to c.infoBg
    Tone.Merged -> c.merged to c.mergedBg
  }
}

@Composable
fun GlBadge(text: String, modifier: Modifier = Modifier, tone: Tone = Tone.Neutral, icon: ImageVector? = null) {
  val (fg, bg) = toneColors(tone)
  Row(
    modifier = modifier
      .clip(RoundedCornerShape(GlRadius.badge))
      .background(bg)
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
      Spacer(Modifier.width(4.dp))
    }
    Text(text = text, style = Gl.type.badge, color = fg, maxLines = 1, softWrap = false)
  }
}

@Composable
fun MonoPill(text: String, modifier: Modifier = Modifier) {
  Text(
    text = text,
    style = Gl.type.monoSmall,
    color = Gl.colors.textSecondary,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
    modifier = modifier
      .clip(RoundedCornerShape(GlRadius.badge))
      .background(Gl.colors.surface2)
      .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}

// ---------------------------------------------------------------------------------------------
// States
// ---------------------------------------------------------------------------------------------

/** Says what this place is, how it fills, and offers one next step. */
@Composable
fun EmptyState(
  icon: ImageVector,
  title: String,
  body: String,
  modifier: Modifier = Modifier,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    IconTile(icon = icon, tint = Gl.colors.textSecondary)
    Spacer(Modifier.height(16.dp))
    Text(title, style = Gl.type.sectionTitle, color = Gl.colors.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(6.dp))
    Text(body, style = Gl.type.bodySmall, color = Gl.colors.textSecondary, textAlign = TextAlign.Center)
    if (actionLabel != null && onAction != null) {
      Spacer(Modifier.height(20.dp))
      GlButton(text = actionLabel, onClick = onAction, kind = GlButtonKind.Secondary)
    }
  }
}

/** Error with a stated recovery. */
@Composable
fun ErrorState(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 32.dp, vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    IconTile(icon = Icons.Outlined.ErrorOutline, tint = Gl.colors.danger, background = Gl.colors.dangerBg)
    Spacer(Modifier.height(16.dp))
    Text("Unable to load", style = Gl.type.sectionTitle, color = Gl.colors.textPrimary)
    Spacer(Modifier.height(6.dp))
    Text(message, style = Gl.type.bodySmall, color = Gl.colors.textSecondary, textAlign = TextAlign.Center)
    if (onRetry != null) {
      Spacer(Modifier.height(20.dp))
      GlButton(text = "Try again", onClick = onRetry, kind = GlButtonKind.Secondary)
    }
  }
}

/** Inline error strip for a section that failed while the rest of the screen loaded. */
@Composable
fun InlineError(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(GlRadius.control))
      .background(Gl.colors.dangerBg)
      .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
      .heightIn(min = 44.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Gl.colors.danger, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(message, style = Gl.type.meta, color = Gl.colors.danger, modifier = Modifier.weight(1f))
    if (onRetry != null) {
      GlButton(text = "Retry", onClick = onRetry, kind = GlButtonKind.Ghost)
    }
  }
}

@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
  Box(modifier.width(28.dp).height(12.dp)
    .clip(RoundedCornerShape(4.dp))
    .background(Gl.colors.textTertiary.copy(alpha = 0.10f))
    .semantics { contentDescription = "Loading" })
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "Loading") {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 48.dp)
      .semantics { contentDescription = label },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = GlSpace.margin),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      repeat(4) { index ->
        Box(Modifier.fillMaxWidth(if (index == 3) 0.55f else 1f)
          .height(if (index == 0) 22.dp else 14.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(Gl.colors.textTertiary.copy(alpha = 0.10f)))
      }
    }
  }
}

@Composable
fun FullScreenState(content: @Composable () -> Unit) {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

// ---------------------------------------------------------------------------------------------
// Media
// ---------------------------------------------------------------------------------------------

/**
 * Credentials for images served by the active GitLab server. Private projects and groups only
 * return their avatars to authenticated requests, so same-origin image loads carry the token.
 */
data class ImageAuth(val baseUrl: String, val accountId: String, val headerName: String, val headerValue: String)

val LocalImageAuth = staticCompositionLocalOf<ImageAuth?> { null }

@Composable
fun rememberImageModel(url: String?): Any? {
  val auth = LocalImageAuth.current
  val context = LocalContext.current
  return remember(url, auth) {
    if (url.isNullOrBlank()) return@remember null
    val origin = auth?.baseUrl?.toHttpUrlOrNull()
    val resolved = url.toHttpUrlOrNull() ?: origin?.resolve(url)
    if (resolved == null) return@remember null
    val builder = ImageRequest.Builder(context).data(resolved.toString()).crossfade(180)
    if (auth != null && origin != null && resolved.scheme == origin.scheme &&
      resolved.host == origin.host && resolved.port == origin.port) {
      if (auth.headerValue.isNotBlank() && auth.headerValue != "Bearer ") builder.addHeader(auth.headerName, auth.headerValue)
      val cacheKey = "account:${auth.accountId}:$resolved"
      builder.memoryCacheKey(cacheKey).diskCacheKey(cacheKey)
    }
    builder.build()
  }
}

/** Marks people and project images; screenshot tooling uses it to blur private content. */
const val PERSONAL_IMAGE_TAG = "redact"

/** Avatar with a 1px neutral outline; falls back to an initial on a neutral tile. */
@Composable
fun Avatar(url: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
  val c = Gl.colors
  Box(
    modifier = modifier
      .testTag(PERSONAL_IMAGE_TAG)
      .size(size)
      .clip(CircleShape)
      .background(c.surface3)
      .border(1.dp, c.imageOutline, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    Text(initial, style = Gl.type.label, color = c.textSecondary)
    if (!url.isNullOrBlank()) {
      AvatarImage(url = url, modifier = Modifier.matchParentSize())
    }
  }
}

/** Contextual icon swap: new icon scales 0.25 → 1, fades 0 → 1 and unblurs 4dp → 0. */
@Composable
fun SwapIcon(
  target: Boolean,
  on: ImageVector,
  off: ImageVector,
  contentDescription: String,
  tintOn: Color,
  tintOff: Color,
  modifier: Modifier = Modifier,
) {
  val reduced = Gl.reducedMotion
  AnimatedContent(
    targetState = target,
    transitionSpec = {
      if (reduced) {
        fadeIn(glTween(GlMotion.FAST, false)) togetherWith fadeOut(glTween(GlMotion.FAST, false))
      } else {
        (scaleIn(glTween(GlMotion.MEDIUM, false), initialScale = GlMotion.ICON_ENTER_SCALE) + fadeIn(glTween(GlMotion.MEDIUM, false))) togetherWith
          fadeOut(glTween(GlMotion.FAST, false, GlMotion.EaseOut))
      }
    },
    label = "icon-swap",
    modifier = modifier,
  ) { state ->
    val blur by transition.animateDp(
      transitionSpec = { glTween(GlMotion.MEDIUM, reduced) },
      label = "icon-blur",
    ) { phase -> if (phase == EnterExitState.PreEnter && !reduced) GlMotion.ICON_ENTER_BLUR_DP.dp else 0.dp }
    Icon(
      imageVector = if (state) on else off,
      contentDescription = contentDescription,
      tint = if (state) tintOn else tintOff,
      modifier = Modifier.blur(blur),
    )
  }
}
