package com.cailiangzhe.lexidue.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LexiDueLightColors =
    lightColorScheme(
        primary = Color(0xFF19566B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC0E9F5),
        onPrimaryContainer = Color(0xFF082F3C),
        secondary = Color(0xFF4B5563),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2E8F0),
        onSecondaryContainer = Color(0xFF1F2937),
        tertiary = Color(0xFF6D3FA0),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFECDDFF),
        onTertiaryContainer = Color(0xFF32155A),
        error = Color(0xFFB3261E),
        onError = Color.White,
        background = Color(0xFFF7FAFC),
        onBackground = Color(0xFF182026),
        surface = Color(0xFFF7FAFC),
        onSurface = Color(0xFF182026),
        surfaceVariant = Color(0xFFE7EEF2),
        onSurfaceVariant = Color(0xFF3F484D),
        outline = Color(0xFF68747A),
    )

private val LexiDueDarkColors =
    darkColorScheme(
        primary = Color(0xFF88CFE3),
        onPrimary = Color(0xFF003642),
        primaryContainer = Color(0xFF074C5E),
        onPrimaryContainer = Color(0xFFC0E9F5),
        secondary = Color(0xFFC5CDD7),
        onSecondary = Color(0xFF29313A),
        secondaryContainer = Color(0xFF3A434D),
        onSecondaryContainer = Color(0xFFE2E8F0),
        tertiary = Color(0xFFD7B9FF),
        onTertiary = Color(0xFF3D176A),
        tertiaryContainer = Color(0xFF552887),
        onTertiaryContainer = Color(0xFFECDDFF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        background = Color(0xFF101416),
        onBackground = Color(0xFFE1E6E9),
        surface = Color(0xFF101416),
        onSurface = Color(0xFFE1E6E9),
        surfaceVariant = Color(0xFF3F484D),
        onSurfaceVariant = Color(0xFFBFC8CD),
        outline = Color(0xFF899399),
    )

private val LexiDueTypography =
    Typography(
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
    )

private val LexiDueShapes =
    Shapes(
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
    )

/** Stable, high-contrast Material 3 theme shared by every LexiDue feature. */
@Composable
fun LexiDueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LexiDueDarkColors else LexiDueLightColors,
        typography = LexiDueTypography,
        shapes = LexiDueShapes,
        content = content,
    )
}
