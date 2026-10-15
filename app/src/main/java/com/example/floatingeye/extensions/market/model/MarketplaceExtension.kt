package com.example.floatingeye.extensions.market.model

data class MarketplaceExtension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val filename: String,
    val contentType: String,
    val size: Long,
    val downloads: Int,
    val author: String,
    val createdAt: String,
    val updatedAt: String
)

data class MarketplaceDownload(
    val extension: MarketplaceExtension,
    val bytes: ByteArray,
    val filename: String,
    val contentType: String
)

data class MarketplaceUploadFile(
    val bytes: ByteArray,
    val filename: String,
    val contentType: String
)

data class MarketplaceOperationResult(
    val savedLocation: String,
    val filename: String
)
