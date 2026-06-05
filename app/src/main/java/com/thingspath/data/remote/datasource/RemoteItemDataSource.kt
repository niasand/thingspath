package com.thingspath.data.remote.datasource

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.remote.D1ApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source: encapsulates D1 API operations.
 * Single responsibility: network communication with Cloudflare D1.
 */
@Singleton
class RemoteItemDataSource @Inject constructor(
    private val d1ApiService: D1ApiService
) {
    private val gson = Gson()

    /**
     * Ensure the D1 table exists (idempotent).
     */
    suspend fun ensureTableExists() {
        d1ApiService.createTableIfNotExists()
    }

    /**
     * Insert a new item into D1, returns the generated ID.
     */
    suspend fun insertItem(
        id: Long? = null,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        createdAt: Long,
        updatedAt: Long
    ): Long {
        return d1ApiService.insertItem(
            id = id,
            name = name,
            imagePaths = imagePaths,
            location = location,
            purchaseDate = purchaseDate,
            purchasePrice = purchasePrice,
            usageDays = usageDays,
            note = note,
            tags = tags,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    /**
     * Update an existing item in D1. Returns true if the update matched a row.
     */
    suspend fun updateItem(
        id: Long,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        updatedAt: Long
    ): Boolean {
        return d1ApiService.updateItem(
            id = id,
            name = name,
            imagePaths = imagePaths,
            location = location,
            purchaseDate = purchaseDate,
            purchasePrice = purchasePrice,
            usageDays = usageDays,
            note = note,
            tags = tags,
            updatedAt = updatedAt
        )
    }

    /**
     * Insert-or-replace an item in D1 (upsert).
     */
    suspend fun upsertItem(
        id: Long,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        createdAt: Long,
        updatedAt: Long
    ) {
        d1ApiService.upsertItem(
            id = id,
            name = name,
            imagePaths = imagePaths,
            location = location,
            purchaseDate = purchaseDate,
            purchasePrice = purchasePrice,
            usageDays = usageDays,
            note = note,
            tags = tags,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    suspend fun deleteItemById(itemId: Long) {
        d1ApiService.deleteItemById(itemId)
    }

    suspend fun deleteItemsByIds(ids: List<Long>) {
        d1ApiService.deleteItemsByIds(ids)
    }

    /**
     * Fetch all items from D1, parsed into ItemEntity list.
     */
    suspend fun getAllItems(): List<ItemEntity> {
        val rows = d1ApiService.getAllItems()
        return rows.map { row -> rowToEntity(row) }
    }

    /**
     * Fetch items updated after the given timestamp from D1.
     */
    suspend fun getItemsUpdatedAfter(since: Long): List<ItemEntity> {
        val rows = d1ApiService.getItemsUpdatedAfter(since)
        return rows.map { row -> rowToEntity(row) }
    }

    /**
     * Get the max updated_at timestamp from D1.
     */
    suspend fun getMaxUpdatedAt(): Long = d1ApiService.getMaxUpdatedAt()

    // ========== Internal helpers ==========

    @Suppress("UNCHECKED_CAST")
    private fun rowToEntity(row: Map<String, Any?>): ItemEntity {
        val imagePaths = parseJsonField(row["image_paths"])
        return ItemEntity(
            id = (row["id"] as? Number)?.toLong() ?: 0,
            name = row["name"] as? String ?: "",
            imagePaths = imagePaths,
            imagePath = imagePaths.firstOrNull(),
            location = row["location"] as? String,
            purchaseDate = (row["purchase_date"] as? Number)?.toLong(),
            purchasePrice = (row["purchase_price"] as? Number)?.toDouble() ?: 0.0,
            usageDays = (row["usage_days"] as? Number)?.toInt(),
            note = row["note"] as? String,
            tags = parseJsonField(row["tags"]),
            createdAt = (row["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (row["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJsonField(value: Any?): List<String> {
        if (value == null) return emptyList()
        if (value is List<*>) {
            return value.mapNotNull { it?.toString() }
        }
        if (value is String) {
            if (value.isBlank()) return emptyList()
            return try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(value, type)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse JSON list: $value", e)
                emptyList()
            }
        }
        return emptyList()
    }

    companion object {
        private const val TAG = "RemoteItemDataSource"
    }
}
