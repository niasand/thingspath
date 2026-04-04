package com.thingspath.data.local.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.model.Item
import com.thingspath.data.remote.D1ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val d1ApiService: D1ApiService
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory cache for reactive Flow support
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: Flow<List<Item>> = _items.asStateFlow()

    init {
        scope.launch {
            try {
                d1ApiService.createTableIfNotExists()
                refreshCache()
            } catch (e: Exception) {
                Log.e("ItemRepository", "Failed to initialize D1", e)
            }
        }
    }

    private suspend fun refreshCache() {
        try {
            val rows = d1ApiService.getAllItems()
            _items.value = rows.map { it.toItem() }
            Log.d("ItemRepository", "Cache refreshed, ${_items.value.size} items")
        } catch (e: Exception) {
            Log.e("ItemRepository", "Failed to refresh cache", e)
        }
    }

    fun getAllItems(): Flow<List<Item>> = items

    fun searchItems(searchQuery: String): Flow<List<Item>> {
        // Search is done client-side from cache; the query already has wildcards from GetItemsUseCase
        return items
    }

    suspend fun getItemById(itemId: Long): Item? {
        return withContext(Dispatchers.IO) {
            try {
                val rows = d1ApiService.getItemById(itemId)
                rows.firstOrNull()?.toItem()
            } catch (e: Exception) {
                Log.e("ItemRepository", "getItemById failed", e)
                null
            }
        }
    }

    suspend fun insertItem(item: Item): Long {
        return withContext(Dispatchers.IO) {
            val id = d1ApiService.insertItem(
                name = item.name,
                imagePaths = gson.toJson(item.imagePaths),
                location = item.location,
                purchaseDate = item.purchaseDate,
                purchasePrice = item.purchasePrice,
                usageDays = item.usageDays,
                note = item.note,
                tags = gson.toJson(item.tags),
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
            refreshCache()
            id
        }
    }

    suspend fun insertItems(items: List<Item>) {
        withContext(Dispatchers.IO) {
            items.forEach { item -> insertItem(item) }
        }
    }

    suspend fun updateItem(item: Item) {
        withContext(Dispatchers.IO) {
            d1ApiService.updateItem(
                id = item.id,
                name = item.name,
                imagePaths = gson.toJson(item.imagePaths),
                location = item.location,
                purchaseDate = item.purchaseDate,
                purchasePrice = item.purchasePrice,
                usageDays = item.usageDays,
                note = item.note,
                tags = gson.toJson(item.tags),
                updatedAt = System.currentTimeMillis()
            )
            refreshCache()
        }
    }

    suspend fun deleteItem(item: Item) {
        withContext(Dispatchers.IO) {
            d1ApiService.deleteItemById(item.id)
            refreshCache()
        }
    }

    suspend fun deleteItemById(itemId: Long) {
        withContext(Dispatchers.IO) {
            d1ApiService.deleteItemById(itemId)
            refreshCache()
        }
    }

    suspend fun deleteItemsByIds(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            d1ApiService.deleteItemsByIds(ids.toList())
            refreshCache()
        }
    }

    suspend fun deleteAllItems() {
        withContext(Dispatchers.IO) {
            d1ApiService.executeQuery("DELETE FROM items")
            refreshCache()
        }
    }

    private fun Map<String, Any?>.toItem(): Item {
        return Item(
            id = (this["id"] as? Number)?.toLong() ?: 0,
            name = this["name"] as? String ?: "",
            imagePath = null,
            imagePaths = parseJsonList(this["image_paths"] as? String),
            location = this["location"] as? String,
            purchaseDate = (this["purchase_date"] as? Number)?.toLong(),
            purchasePrice = (this["purchase_price"] as? Number)?.toDouble() ?: 0.0,
            usageDays = (this["usage_days"] as? Number)?.toInt(),
            note = this["note"] as? String,
            tags = parseJsonList(this["tags"] as? String),
            createdAt = (this["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (this["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun parseJsonList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(json, type)
        } catch (e: Exception) {
            Log.w("ItemRepository", "Failed to parse JSON list: $json", e)
            emptyList()
        }
    }
}
