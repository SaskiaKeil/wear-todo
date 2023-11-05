package com.example.myapplication.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val White = Color(0xFFFFEBEE)
val Teal200 = Color(0xFF03DAC5)
val Red400 = Color(0xFFCF6679)

internal val wearColorPalette: Colors = Colors(
        primary = White,
        primaryVariant = White,
        secondary = Teal200,
        secondaryVariant = Teal200,
        error = Red400,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onError = Color.Black
)