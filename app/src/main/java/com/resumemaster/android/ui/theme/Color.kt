package com.resumemaster.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The palette — the WEB's tokens (../draft/client/src/styles/colorTokens.js, C_LIGHT / C_DARK),
 * re-pointed 09-30 (follow-up 17). It was a teal Material palette of its own.
 *
 * THEME-AWARE AT THE SOURCE. Screens use these names directly (TextMuted, Primary, Border…), and
 * they were light-only constants, so dark mode was half-applied: Material's surfaces flipped and
 * every direct use stayed light. Each name is now a composable getter that resolves the system
 * theme, so every existing call site follows dark mode with no edit.
 *
 * Primary is the orange INK (readable as text on paper and on charcoal), not the logo orange: the
 * fill-only rule from the web holds here too — #FD4600 is 2.96:1 on the cream.
 */
internal class Palette(
  val background: Color, val surface: Color, val surfaceOffset: Color, val surfaceDynamic: Color,
  val textPrimary: Color, val textMuted: Color, val textFaint: Color,
  val primary: Color, val primaryHover: Color, val border: Color, val divider: Color,
  val success: Color, val warning: Color, val error: Color, val gold: Color,
  val glassTop: Color, val glassBody: Color, val glassRim: Color, val glassSpecular: Color,
)

internal val Light = Palette(
  background = Color(0xFFF3ECE0), surface = Color(0xFFFBF7F0), surfaceOffset = Color(0xFFEFE7DA), surfaceDynamic = Color(0xFFDDD2C3),
  textPrimary = Color(0xFF1E1915), textMuted = Color(0xFF4A423B), textFaint = Color(0xFF62574D),
  primary = Color(0xFFA33200), primaryHover = Color(0xFF8A2A00), border = Color(0xFFDDD2C3), divider = Color(0xFFE4DACB),
  success = Color(0xFF1A6B33), warning = Color(0xFF855000), error = Color(0xFFB3261E), gold = Color(0xFFA86A00),
  glassTop = Color(0xB3FFFDF9), glassBody = Color(0x5CFFFBF5), glassRim = Color(0x80FFFFFF), glassSpecular = Color(0xF2FFFFFF),
)

internal val Dark = Palette(
  background = Color(0xFF1C1815), surface = Color(0xFF26201C), surfaceOffset = Color(0xFF2E2722), surfaceDynamic = Color(0xFF3B332C),
  textPrimary = Color(0xFFF2EADF), textMuted = Color(0xFFCFC2B3), textFaint = Color(0xFFADA090),
  primary = Color(0xFFFF7D4A), primaryHover = Color(0xFFFF9466), border = Color(0xFF3B332C), divider = Color(0xFF302A25),
  success = Color(0xFF5FCB8C), warning = Color(0xFFE6AA4E), error = Color(0xFFF27F72), gold = Color(0xFFE5BC3D),
  glassTop = Color(0xBD3A312A), glassBody = Color(0x80261F1C), glassRim = Color(0x1CFFF0E1), glassSpecular = Color(0x38FFF0E1),
)

private val palette: Palette @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) Dark else Light

/** The ground under everything: the leather's base colour (system bars, behind the tile). */
val Canvas: Color @Composable @ReadOnlyComposable get() = palette.background
val Background: Color @Composable @ReadOnlyComposable get() = palette.background
val Surface: Color @Composable @ReadOnlyComposable get() = palette.surface
val SurfaceOffset: Color @Composable @ReadOnlyComposable get() = palette.surfaceOffset
val SurfaceDynamic: Color @Composable @ReadOnlyComposable get() = palette.surfaceDynamic
val TextPrimary: Color @Composable @ReadOnlyComposable get() = palette.textPrimary
val TextMuted: Color @Composable @ReadOnlyComposable get() = palette.textMuted
val TextFaint: Color @Composable @ReadOnlyComposable get() = palette.textFaint
val Primary: Color @Composable @ReadOnlyComposable get() = palette.primary
val PrimaryHover: Color @Composable @ReadOnlyComposable get() = palette.primaryHover
val Border: Color @Composable @ReadOnlyComposable get() = palette.border
val Divider: Color @Composable @ReadOnlyComposable get() = palette.divider
val Success: Color @Composable @ReadOnlyComposable get() = palette.success
val Warning: Color @Composable @ReadOnlyComposable get() = palette.warning
val Error: Color @Composable @ReadOnlyComposable get() = palette.error
val Gold: Color @Composable @ReadOnlyComposable get() = palette.gold
// The glass material (web styles/glass.js): a body denser at the top, a light rim, a lit top edge.
val GlassTop: Color @Composable @ReadOnlyComposable get() = palette.glassTop
val GlassBody: Color @Composable @ReadOnlyComposable get() = palette.glassBody
val GlassRim: Color @Composable @ReadOnlyComposable get() = palette.glassRim
val GlassSpecular: Color @Composable @ReadOnlyComposable get() = palette.glassSpecular
