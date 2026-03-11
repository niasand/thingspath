package com.thingspath.data.model

data class Item(
    val id: Long = 0,
    val name: String,
    val imagePath: String? = null,
    val location: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double = 0.0,
    val usageDays: Int? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
