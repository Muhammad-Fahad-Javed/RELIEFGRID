package com.mohallaops.ui.theme

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

/** RELIEFGRID color system — consistent across light and dark themes. */
object MohallaColors {
    // Core brand colors (same in both themes)
    val Navy = Color(0xFF142B45)
    val Teal = Color(0xFF168C83)

    // Light theme semantic colors
    val Background = Color(0xFFF7F9FC)
    val Surface = Color.White
    val TextPrimary = Color(0xFF1F2933)
    val TextSecondary = Color(0xFF5B6B7F)
    val Border = Color(0xFFDDE3EC)

    // Status tones (same in both themes for accessibility consistency)
    val Success = Color(0xFF1E7B4B);  val SuccessBg = Color(0xFFE6F4EC)
    val Warning = Color(0xFFB45309);  val WarningBg = Color(0xFFFEF3C7)
    val Danger  = Color(0xFFB42318);  val DangerBg  = Color(0xFFFDECEA)
    val Neutral = Color(0xFF475467);  val NeutralBg = Color(0xFFEEF1F6)
    val Info    = Color(0xFF142B45);  val InfoBg    = Color(0xFFE3EBF5)
}

/** Dark theme alternative colors. */
private object DarkColors {
    val Background   = Color(0xFF0D1B2A)
    val Surface      = Color(0xFF1A2B3C)
    val TextPrimary  = Color(0xFFE8EEF5)
    val TextSecondary= Color(0xFF8FA5BC)
    val Border       = Color(0xFF2A3D52)
}

private val LightScheme = lightColorScheme(
    primary           = MohallaColors.Navy,
    onPrimary         = Color.White,
    secondary         = MohallaColors.Teal,
    onSecondary       = Color.White,
    background        = MohallaColors.Background,
    onBackground      = MohallaColors.TextPrimary,
    surface           = MohallaColors.Surface,
    onSurface         = MohallaColors.TextPrimary,
    onSurfaceVariant  = MohallaColors.TextSecondary,
    outline           = MohallaColors.Border,
    error             = MohallaColors.Danger
)

private val DarkScheme = darkColorScheme(
    primary           = Color(0xFF4A9CC8),
    onPrimary         = Color.White,
    secondary         = MohallaColors.Teal,
    onSecondary       = Color.White,
    background        = DarkColors.Background,
    onBackground      = DarkColors.TextPrimary,
    surface           = DarkColors.Surface,
    onSurface         = DarkColors.TextPrimary,
    onSurfaceVariant  = DarkColors.TextSecondary,
    outline           = DarkColors.Border,
    error             = Color(0xFFE57373)
)

private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge     = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge      = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium     = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge     = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium    = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

/** Numeric style with tabular figures, for inventory counts and metrics. */
val NumberStyle = TextStyle(
    fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp,
    fontFeatureSettings = "tnum"
)

@Composable
fun MohallaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        shapes = Shapes(
            small  = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large  = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
