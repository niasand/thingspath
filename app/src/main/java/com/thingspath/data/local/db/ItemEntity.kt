package com.thingspath.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.model.Item

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val imagePaths: String = "[]",
    val imagePath: String? = null,
    val location: String? = null,
    val purchaseDate: Long? = null,
    val purchasePrice: Double = 0.0,
    val usageDays: Int? = null,
    val note: String? = null,
    val tags: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

private val gson = Gson()

fun ItemEntity.toItem(): Item {
    val paths = parseJsonList(imagePaths)
    return Item(
        id = id,
        name = name,
        imagePath = imagePath ?: paths.firstOrNull(),
        imagePaths = paths,
        location = location,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        usageDays = usageDays,
        note = note,
        tags = parseJsonList(tags),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        name = name,
        imagePaths = gson.toJson(imagePaths),
        imagePath = imagePath,
        location = location,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        usageDays = usageDays,
        note = note,
        tags = gson.toJson(tags),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Suppress("UNCHECKED_CAST")
private fun parseJsonList(value: String?): List<String> {
    if (value.isNullOrBlank()) return emptyList()
    return try {
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(value, type) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
