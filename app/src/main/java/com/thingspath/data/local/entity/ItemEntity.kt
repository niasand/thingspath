package com.thingspath.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imagePath: String? = null,
    val location: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double = 0.0,
    val usageDays: Int? = null,
    val note: String? = null,
    val tags: String = "", // Stored as comma-separated string
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
