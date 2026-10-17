package com.cozyhome.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val scheme = lightColorScheme(
    primary = Mint,
    onPrimary = White,
    secondary = Sky,
    onSecondary = White,
    tertiary = Coral,
    background = Cream,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
)

private val typography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Ink),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp),
)

@Composable
fun GameTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
