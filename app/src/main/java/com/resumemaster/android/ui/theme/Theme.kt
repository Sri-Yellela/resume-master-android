package com.resumemaster.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LightScheme=lightColorScheme(primary=Primary,onPrimary=Color.White,background=Background,onBackground=TextPrimary,surface=Surface,onSurface=TextPrimary,surfaceVariant=SurfaceOffset,outline=Border,error=Error)
private val DarkScheme=darkColorScheme(primary=DarkPrimary,onPrimary=DarkBackground,background=DarkBackground,onBackground=DarkTextPrimary,surface=DarkSurface,onSurface=DarkTextPrimary,surfaceVariant=DarkSurfaceOffset,outline=DarkBorder,error=DarkError)
@Composable fun ResumeMasterTheme(darkTheme:Boolean=isSystemInDarkTheme(),content: @Composable () -> Unit){val c=rememberSystemUiController(); val s=if(darkTheme) DarkScheme else LightScheme; c.setStatusBarColor(s.background,darkIcons=!darkTheme); c.setNavigationBarColor(s.surface,darkIcons=!darkTheme); MaterialTheme(colorScheme=s,typography=AppTypography,shapes=AppShapes,content=content)}


