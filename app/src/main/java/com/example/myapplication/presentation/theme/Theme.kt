package com.example.myapplication.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun MyApplicationTheme(
        content: @Composable () -> Unit
) {
    MaterialTheme(
            colors = wearColorPalette,
            typography = Typography,
            content = content
    )
}