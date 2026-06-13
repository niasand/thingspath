package com.thingspath.data.model

data class Item(
    val id: Long = 0,
    val name: String,
    val imagePath: String? = null,       // primary image (backward compat, derived from imagePaths[0])
    val imagePaths: List<String> = emptyList(), // all images
    val location: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double = 0.0,
    val usageDays: Int? = null,
    val reminderDate: Long? = null,
    val reminderType: String? = null,
    val reminderNote: String? = null,
    val setName: String? = null,
    val setNote: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
