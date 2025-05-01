package com.example.healthapp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

@Composable
fun HealthAppTheme(
    content: @Composable () -> Unit
) {
    val typography = Typography(
        display1 = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        ),
        title1 = TextStyle(
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        ),
        // Add other text styles as needed
    )

    MaterialTheme(
        colors = wearColorPalette,
        typography = typography,
        content = content
    )
}

private val wearColorPalette = androidx.wear.compose.material.Colors(
    primary = androidx.compose.ui.graphics.Color(0xFF4285F4),
    primaryVariant = androidx.compose.ui.graphics.Color(0xFF3367D6),
    secondary = androidx.compose.ui.graphics.Color(0xFF34A853),
    secondaryVariant = androidx.compose.ui.graphics.Color(0xFF1E8E3E),
    error = androidx.compose.ui.graphics.Color(0xFFEA4335),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onError = androidx.compose.ui.graphics.Color.White
)