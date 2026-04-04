package com.thingspath.data.local.repository

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val d1ApiService: D1ApiService,
    private val application: Application,
    private val imageLoader: ImageLoader
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cacheFile = File(application.filesDir, "items_cache.json")

    // In-memory cache for reactive Flow support
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: Flow<List<Item>> = _items.asStateFlow()

    init {
        scope.launch {
            try {
                d1ApiService.createTableIfNotExists()
                // 1. Load from local cache first (instant UI)
                loadFromLocalCache()
                // 2. Then refresh from D1 (background update)
                refreshCache()
            } catch (e: Exception) {
                Log.e("ItemRepository", "Failed to initialize", e)
            }
        }
    }

    private fun loadFromLocalCache() {
        try {
            if (!cacheFile.exists()) return
            val json = cacheFile.readText()
            if (json.isBlank()) return
            val type = object : TypeToken<List<Item>>() {}.type
            val items: List<Item> = gson.fromJson(json, type) ?: return
            _items.value = items
            Log.d("ItemRepository", "Loaded ${items.size} items from local cache")
        } catch (e: Exception) {
            Log.w("ItemRepository", "Failed to load local cache", e)
        }
    }

    private fun saveToLocalCache(items: List<Item>) {
        try {
            cacheFile.writeText(gson.toJson(items))
        } catch (e: Exception) {
            Log.w("ItemRepository", "Failed to save local cache", e)
        }
    }

    suspend fun refreshFromRemote() = refreshCache()

    private suspend fun refreshCache() {
        try {
            val rows = d1ApiService.getAllItems()
            val items = rows.map { it.toItem() }
            _items.value = items
            saveToLocalCache(items)
            Log.d("ItemRepository", "Cache refreshed from D1, ${items.size} items")
            // 预取所有 R2 图片到 Coil 磁盘缓存，下次打开详情页无需再从 R2 下载
            prefetchImages(items)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Failed to refresh from D1", e)
        }
    }

    /**
     * 将所有 R2 远程图片入队预缓存到 Coil 的磁盘缓存。
     * 这是 fire-and-forget 操作，不阻塞 UI。
     */
    private fun prefetchImages(items: List<Item>) {
        val urls = items
            .flatMap { it.imagePaths }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()

        if (urls.isEmpty()) return

        Log.d("ItemRepository", "Pre-fetching ${urls.size} remote images to disk cache")
        urls.forEach { url ->
            val request = ImageRequest.Builder(application)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
    }

    fun getAllItems(): Flow<List<Item>> = items

    /**
     * 同步读取内存缓存中的物品，用于详情页即时展示，无需 loading 状态。
     * 缓存未命中时返回 null，调用方应回退到 suspend 版本的 getItemById()。
     */
    fun getCachedItemById(itemId: Long): Item? {
        return _items.value.find { it.id == itemId }
    }

    fun searchItems(searchQuery: String): Flow<List<Item>> {
        return items
    }

    suspend fun getItemById(itemId: Long): Item? {
        // 优先从内存缓存读取，与列表页一致实现即时加载
        _items.value.find { it.id == itemId }?.let { return it }

        // 缓存未命中时回退到 D1 网络请求
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
            imagePaths = parseJsonField(this["image_paths"]),
            imagePath = parseJsonField(this["image_paths"]).firstOrNull(),
            location = this["location"] as? String,
            purchaseDate = (this["purchase_date"] as? Number)?.toLong(),
            purchasePrice = (this["purchase_price"] as? Number)?.toDouble() ?: 0.0,
            usageDays = (this["usage_days"] as? Number)?.toInt(),
            note = this["note"] as? String,
            tags = parseJsonField(this["tags"]),
            createdAt = (this["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (this["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
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
                Log.w("ItemRepository", "Failed to parse JSON list: $value", e)
                emptyList()
            }
        }
        return emptyList()
    }
}
