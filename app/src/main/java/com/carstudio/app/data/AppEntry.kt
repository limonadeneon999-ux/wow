package com.carstudio.app.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppEntry(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap,
    val color: Long
)
