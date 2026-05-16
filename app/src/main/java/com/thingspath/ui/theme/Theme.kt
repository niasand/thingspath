package com.thingspath.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = Pink80,
    tertiary = Purple80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF3C2F42),
    onTertiary = Color(0xFF491068),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Pink40,
    background = Background,
    surface = Surface,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onTertiary = OnPrimary,
    onBackground = OnBackground,
    onSurface = OnSurface,
)

@Immutable
data class CustomColors(
    val purplePrimary: Color,
    val purpleLight: Color,
    val pinkPrimary: Color,
    val pinkLight: Color,
    val grayText: Color,
    val grayLight: Color,
    val homeBackground: Color
)

val LightCustomColors = CustomColors(
    purplePrimary = LightPurplePrimary,
    purpleLight = LightPurpleLight,
    pinkPrimary = LightPinkPrimary,
    pinkLight = LightPinkLight,
    grayText = LightGrayText,
    grayLight = LightGrayLight,
    homeBackground = LightHomeBackground
)

val DarkCustomColors = CustomColors(
    purplePrimary = DarkPurplePrimary,
    purpleLight = DarkPurpleLight,
    pinkPrimary = DarkPinkPrimary,
    pinkLight = DarkPinkLight,
    grayText = DarkGrayText,
    grayLight = DarkGrayLight,
    homeBackground = DarkHomeBackground
)

val LocalCustomColors = staticCompositionLocalOf { LightCustomColors }

@Composable
fun ThingsPathTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val customColors = when {
        darkTheme -> DarkCustomColors
        else -> LightCustomColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

val MaterialTheme.customColors: CustomColors
    @Composable
    get() = LocalCustomColors.current
