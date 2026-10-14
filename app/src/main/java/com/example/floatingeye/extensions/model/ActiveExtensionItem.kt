package com.example.floatingeye.extensions.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ActiveExtensionItem(
    val id: String,
    val name: String,
    val iconKey: String,
    val icon: ImageVector,
    val isSystemFeature: Boolean = false,
    val badgeText: String? = null
)
