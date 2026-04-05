package com.thingspath.data.local.repository

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.local.db.ItemDao
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.local.db.toEntity
import com.thingspath.data.local.db.toItem
import com.thingspath.data.model.Item
import com.thingspath.data.remote.D1ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val d1ApiService: D1ApiService,
    private val itemDao: ItemDao,
    private val application: Application,
    private val imageLoader: ImageLoader
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 内存索引：由 Room Flow 驱动更新，支持 getCachedItemById() 同步读取
    private val cachedItems = ConcurrentHashMap<Long, Item>()

    // Room Flow 自动在表变更时发射，替代手动 StateFlow
    val items: Flow<List<Item>> = itemDao.getAllItems().map { entities ->
        entities.map { it.toItem() }
    }

    init {
        // 观察 Room Flow，同步更新内存索引 + 预取图片
        scope.launch {
            items.collect { itemList ->
                cachedItems.clear()
                itemList.forEach { cachedItems[it.id] = it }
                prefetchImages(itemList)
            }
        }

        // 首次启动：Room 为空时从 D1 拉取数据
        scope.launch {
            try {
                d1ApiService.createTableIfNotExists()
                val count = itemDao.itemCount()
                if (count == 0) {
                    Log.d(TAG, "Room is empty, pulling from D1...")
                    pullFromD1()
                } else {
                    Log.d(TAG, "Room has $count items, skipping D1 pull")
                    // 启动后仍然预取图片
                    items.first().let { prefetchImages(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize", e)
            }
        }
    }

    // ========== 公开 API（接口与改造前一致）==========

    suspend fun refreshFromRemote() = pullFromD1()

    fun getAllItems(): Flow<List<Item>> = items

    /**
     * 同步读取内存缓存，用于详情页即时展示。
     */
    fun getCachedItemById(itemId: Long): Item? = cachedItems[itemId]

    fun searchItems(searchQuery: String): Flow<List<Item>> {
        return itemDao.searchItems(searchQuery).map { entities ->
            entities.map { it.toItem() }
        }
    }

    suspend fun getItemById(itemId: Long): Item? {
        return cachedItems[itemId] ?: itemDao.getItemById(itemId)?.toItem()
    }

    /**
     * 插入物品：D1 优先获取真实 ID，然后写入 Room。
     */
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
            val entity = item.copy(id = id).toEntity()
            itemDao.insertItem(entity)
            Log.d(TAG, "Inserted item $id to Room + D1")
            id
        }
    }

    suspend fun insertItems(items: List<Item>) {
        withContext(Dispatchers.IO) {
            items.forEach { item -> insertItem(item) }
        }
    }

    /**
     * 更新物品：Room 先写，然后同步到 D1。
     */
    suspend fun updateItem(item: Item) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val entity = item.copy(updatedAt = now).toEntity()
            itemDao.updateItem(
                id = entity.id,
                name = entity.name,
                imagePaths = entity.imagePaths,
                imagePath = entity.imagePath,
                location = entity.location,
                purchaseDate = entity.purchaseDate,
                purchasePrice = entity.purchasePrice,
                usageDays = entity.usageDays,
                note = entity.note,
                tags = entity.tags,
                updatedAt = entity.updatedAt
            )
            try {
                val updated = d1ApiService.updateItem(
                    id = item.id,
                    name = item.name,
                    imagePaths = gson.toJson(item.imagePaths),
                    location = item.location,
                    purchaseDate = item.purchaseDate,
                    purchasePrice = item.purchasePrice,
                    usageDays = item.usageDays,
                    note = item.note,
                    tags = gson.toJson(item.tags),
                    updatedAt = now
                )
                if (!updated) {
                    Log.w(TAG, "D1 UPDATE matched 0 rows for item ${item.id}, trying INSERT")
                    d1ApiService.insertItem(
                        name = item.name,
                        imagePaths = gson.toJson(item.imagePaths),
                        location = item.location,
                        purchaseDate = item.purchaseDate,
                        purchasePrice = item.purchasePrice,
                        usageDays = item.usageDays,
                        note = item.note,
                        tags = gson.toJson(item.tags),
                        createdAt = item.createdAt,
                        updatedAt = now
                    )
                } else {
                    Log.d(TAG, "D1 UPDATE succeeded for item ${item.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync item ${item.id} to D1", e)
            }
        }
    }

    /**
     * 删除物品：Room 先删，然后同步到 D1。
     */
    suspend fun deleteItem(item: Item) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItemById(item.id)
            try {
                d1ApiService.deleteItemById(item.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync delete to D1 for item ${item.id}", e)
            }
        }
    }

    suspend fun deleteItemById(itemId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItemById(itemId)
            try {
                d1ApiService.deleteItemById(itemId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync delete to D1 for item $itemId", e)
            }
        }
    }

    suspend fun deleteItemsByIds(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItemsByIds(ids.toList())
            try {
                d1ApiService.deleteItemsByIds(ids.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync bulk delete to D1 for ids $ids", e)
            }
        }
    }

    suspend fun deleteAllItems() {
        withContext(Dispatchers.IO) {
            // Room 本地全量删除，D1 暂不处理（避免误删远端数据）
            itemDao.getAllItems().first().forEach { entity ->
                itemDao.deleteItemById(entity.id)
            }
        }
    }

    // ========== 内部方法 ==========

    /**
     * 从 D1 拉取全部数据写入 Room（INSERT OR REPLACE）。
     * 供设置页手动恢复和首次启动自动恢复使用。
     */
    suspend fun pullFromD1() {
        try {
            val rows = d1ApiService.getAllItems()
            val entities = rows.map { row ->
                ItemEntity(
                    id = (row["id"] as? Number)?.toLong() ?: 0,
                    name = row["name"] as? String ?: "",
                    imagePaths = row["image_paths"] as? String ?: "[]",
                    imagePath = parseJsonField(row["image_paths"]).firstOrNull(),
                    location = row["location"] as? String,
                    purchaseDate = (row["purchase_date"] as? Number)?.toLong(),
                    purchasePrice = (row["purchase_price"] as? Number)?.toDouble() ?: 0.0,
                    usageDays = (row["usage_days"] as? Number)?.toInt(),
                    note = row["note"] as? String,
                    tags = row["tags"] as? String ?: "[]",
                    createdAt = (row["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (row["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
            itemDao.insertItems(entities)
            Log.d(TAG, "Pulled ${entities.size} items from D1 to Room")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull from D1", e)
        }
    }

    private fun prefetchImages(items: List<Item>) {
        val urls = items
            .flatMap { it.imagePaths }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()

        if (urls.isEmpty()) return

        Log.d(TAG, "Pre-fetching ${urls.size} remote images to disk cache")
        urls.forEach { url ->
            val request = ImageRequest.Builder(application)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            imageLoader.enqueue(request)
        }
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
        private const val TAG = "ItemRepository"
    }
}
