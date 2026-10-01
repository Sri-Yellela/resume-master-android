package com.resumemaster.android.ui.theme

import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.resumemaster.android.R

// The schemes are the web's tokens (Color.kt). ⛔ BACKGROUND IS TRANSPARENT: every screen paints
// `colorScheme.background`, and the leather (Leather, below) is drawn once behind them all — so the
// texture shows through exactly as the web's substrate does. The system bars take the canvas colour.
private val LightScheme = lightColorScheme(primary = Light.primary, onPrimary = Light.surface, background = Color.Transparent,
  onBackground = Light.textPrimary, surface = Light.surface, onSurface = Light.textPrimary, surfaceVariant = Light.surfaceOffset,
  outline = Light.border, error = Light.error)
private val DarkScheme = darkColorScheme(primary = Dark.primary, onPrimary = Dark.background, background = Color.Transparent,
  onBackground = Dark.textPrimary, surface = Dark.surface, onSurface = Dark.textPrimary, surfaceVariant = Dark.surfaceOffset,
  outline = Dark.border, error = Dark.error)

/**
 * The leather, tiled: the WEB's own tile (the same feTurbulence layers and per-theme settings as
 * client/src/components/PaperBackground.jsx, rendered to res/drawable-nodpi/leather_*.png), so the
 * phone shows the same hide, at the same pebble size — one 320dp tile, as the web's is 320px.
 */
@Composable fun Leather(dark: Boolean, content: @Composable () -> Unit) {
  val ctx = LocalContext.current
  val density = LocalDensity.current.density
  val brush = remember(dark, density) {
    val bmp = BitmapFactory.decodeResource(ctx.resources, if (dark) R.drawable.leather_dark else R.drawable.leather_light)
    val shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    // The bitmap is 640px for a 320dp tile: scale it to this screen's density.
    shader.setLocalMatrix(Matrix().apply { setScale(320f * density / bmp.width, 320f * density / bmp.height) })
    ShaderBrush(shader)
  }
  Box(Modifier.fillMaxSize().background(if (dark) Dark.background else Light.background).background(brush)) { content() }
}

@Composable fun ResumeMasterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val c = rememberSystemUiController()
  val s = if (darkTheme) DarkScheme else LightScheme
  val canvas = if (darkTheme) Dark.background else Light.background
  c.setStatusBarColor(canvas, darkIcons = !darkTheme)
  c.setNavigationBarColor(canvas, darkIcons = !darkTheme)
  MaterialTheme(colorScheme = s, typography = AppTypography, shapes = AppShapes) { Leather(darkTheme, content) }
}
