package com.recipegrabber.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    primaryContainer = Color(0xFF254E3E),
    onPrimaryContainer = Color(0xFFB1F1CF),
    secondaryContainer = Color(0xFF56352F),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFD7CE92),
    tertiaryContainer = Color(0xFF494521),
    onTertiaryContainer = Color(0xFFF4EBAC),
    surfaceVariant = Color(0xFF303A35),
    onSurfaceVariant = Color(0xFFBAC9C0),
    outline = Color(0xFF89978E),
    outlineVariant = Color(0xFF3B4841),
    background = BackgroundDark,
    surface = SurfaceDark,
    error = ErrorDark,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    onTertiary = androidx.compose.ui.graphics.Color.Black,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onError = androidx.compose.ui.graphics.Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    primaryContainer = Color(0xFFD0F2DF),
    onPrimaryContainer = Color(0xFF053A27),
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF71352A),
    tertiary = Color(0xFF696126),
    tertiaryContainer = Color(0xFFF3EBB0),
    onTertiaryContainer = Color(0xFF393308),
    surfaceVariant = Color(0xFFE5ECE7),
    onSurfaceVariant = Color(0xFF46544C),
    outline = Color(0xFF748178),
    outlineVariant = Color(0xFFCED9D1),
    background = BackgroundLight,
    surface = SurfaceLight,
    error = ErrorLight,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = Color(0xFF18231D),
    onSurface = Color(0xFF18231D),
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun RecipeGrabberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
