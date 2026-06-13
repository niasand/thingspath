package com.thingspath.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thingspath.data.model.Item

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "image_paths") val imagePaths: List<String> = emptyList(),
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    val location: String? = null,
    @ColumnInfo(name = "purchase_date") val purchaseDate: Long? = null,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Double = 0.0,
    @ColumnInfo(name = "usage_days") val usageDays: Int? = null,
    @ColumnInfo(name = "reminder_date") val reminderDate: Long? = null,
    @ColumnInfo(name = "reminder_type") val reminderType: String? = null,
    @ColumnInfo(name = "reminder_note") val reminderNote: String? = null,
    @ColumnInfo(name = "set_name") val setName: String? = null,
    @ColumnInfo(name = "set_note") val setNote: String? = null,
    val note: String? = null,
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

fun ItemEntity.toItem(): Item = Item(
    id = id,
    name = name,
    imagePath = imagePath ?: imagePaths.firstOrNull(),
    imagePaths = imagePaths,
    location = location,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    usageDays = usageDays,
    reminderDate = reminderDate,
    reminderType = reminderType,
    reminderNote = reminderNote,
    setName = setName,
    setNote = setNote,
    note = note,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    name = name,
    imagePaths = imagePaths,
    imagePath = imagePath,
    location = location,
    purchaseDate = purchaseDate,
    purchasePrice = purchasePrice,
    usageDays = usageDays,
    reminderDate = reminderDate,
    reminderType = reminderType,
    reminderNote = reminderNote,
    setName = setName,
    setNote = setNote,
    note = note,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt
)
