package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DoveLightScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = VeridianMint,
    onSecondary = Color.White,
    secondaryContainer = VeridianMintContainer,
    onSecondaryContainer = OnVeridianMintContainer,
    tertiary = AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = AmberGoldContainer,
    onTertiaryContainer = OnAmberGoldContainer,
    background = PorcelainBg,
    onBackground = EspressoInk,
    surface = PorcelainBg,
    onSurface = EspressoInk,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = MutedStone,
    surfaceContainerLow = WarmSurface,
    surfaceContainer = WarmSurface,
    surfaceContainerHigh = WarmSurfaceHigh,
    outline = WarmOutline,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = Color(0xFF410002)
)

private val DoveDarkScheme = darkColorScheme(
    primary = DarkTerracotta,
    onPrimary = Color(0xFF5D131D),
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = TerracottaContainer,
    secondary = DarkMint,
    onSecondary = Color(0xFF00382C),
    secondaryContainer = DarkMintContainer,
    onSecondaryContainer = VeridianMintContainer,
    tertiary = DarkAmber,
    onTertiary = Color(0xFF442700),
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = AmberGoldContainer,
    background = DarkPlumBg,
    onBackground = LightPorcelainText,
    surface = DarkPlumBg,
    onSurface = LightPorcelainText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = Color(0xFFCBBDBE),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    outline = Color(0xFF524446),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = DangerContainer
)

val DoveShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DoveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DoveDarkScheme
        else -> DoveLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DoveTypography,
        shapes = DoveShapes,
        content = content
    )
}
