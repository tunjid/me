package com.tunjid.me.core.ui

import androidx.compose.ui.layout.ContentScale

data class MediaArgs(
    val url: String?,
    val description: String? = null,
    val contentScale: ContentScale
)