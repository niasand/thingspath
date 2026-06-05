package com.thingspath.data.local.repository

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.db.ItemDao
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.local.db.toEntity
import com.thingspath.data.local.db.toItem
import com.thingspath.data.model.Item
import com.thingspath.data.remote.D1ApiService
import com.thingspath.domain.model.AppError
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
    private val settingsRepository: SettingsRepository,
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

        // 启动同步：Room 为空 → 全量 pull；否则 → 增量同步
        scope.launch {
            try {
                d1ApiService.createTableIfNotExists()
                val count = itemDao.itemCount()
                if (count == 0) {
                    Log.d(TAG, "Room is empty, doing full pull from D1...")
                    pullFromD1()
                    // 全量 pull 后，设 watermark 到当前最大 updated_at
                    val maxTs = itemDao.getMaxUpdatedAt()
                    settingsRepository.setLastPullUpdatedAt(maxTs)
                    settingsRepository.setLastPushUpdatedAt(maxTs)
                } else {
                    Log.d(TAG, "Room has $count items, doing incremental sync...")
                    incrementalSync()
                }
            } catch (e: AppError) {
                Log.e(TAG, "Failed to initialize", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize", AppError.SyncError("Init sync failed", e))
            }
        }
    }

    // ========== 公开 API ==========

    /**
     * 增量同步（下拉刷新使用）。
     */
    suspend fun refreshFromRemote() {
        incrementalSync()
    }

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
                    Log.w(TAG, "D1 UPDATE matched 0 rows for item ${item.id}, trying INSERT with local id")
                    d1ApiService.insertItem(
                        id = item.id,
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
                Log.e(TAG, "Failed to sync item ${item.id} to D1", AppError.SyncError("D1 update failed for item ${item.id}", e))
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
                Log.e(TAG, "Failed to sync delete to D1 for item ${item.id}", AppError.SyncError("D1 delete failed for item ${item.id}", e))
            }
        }
    }

    suspend fun deleteItemById(itemId: Long) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItemById(itemId)
            try {
                d1ApiService.deleteItemById(itemId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync delete to D1 for item $itemId", AppError.SyncError("D1 delete failed for item $itemId", e))
            }
        }
    }

    suspend fun deleteItemsByIds(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            itemDao.deleteItemsByIds(ids.toList())
            try {
                d1ApiService.deleteItemsByIds(ids.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync bulk delete to D1 for ids $ids", AppError.SyncError("D1 bulk delete failed for ids $ids", e))
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

    // ========== 增量同步 ==========

    /**
     * 双向增量同步：先 pull（D1→Room），再 push（Room→D1）。
     * pull 优先确保 D1 数据覆盖本地冲突。
     */
    private suspend fun incrementalSync() {
        incrementalPull()
        incrementalPush()
    }

    private suspend fun incrementalPull() {
        val lastPullTs = settingsRepository.lastPullUpdatedAt.first()
        val rows = d1ApiService.getItemsUpdatedAfter(lastPullTs)

        if (rows.isEmpty()) {
            Log.d(TAG, "Incremental pull: no new items from D1 since $lastPullTs")
            return
        }

        var maxUpdatedAt = lastPullTs
        val entities = rows.map { row ->
            val updatedAt = (row["updated_at"] as? Number)?.toLong() ?: 0L
            if (updatedAt > maxUpdatedAt) maxUpdatedAt = updatedAt

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
                updatedAt = updatedAt
            )
        }

        itemDao.insertItems(entities)
        settingsRepository.setLastPullUpdatedAt(maxUpdatedAt)
        Log.d(TAG, "Incremental pull: fetched ${entities.size} items, watermark = $maxUpdatedAt")
    }

    private suspend fun incrementalPush() {
        val lastPushTs = settingsRepository.lastPushUpdatedAt.first()
        val localChanges = itemDao.getItemsUpdatedAfter(lastPushTs)

        if (localChanges.isEmpty()) {
            Log.d(TAG, "Incremental push: no local changes since $lastPushTs")
            return
        }

        var maxUpdatedAt = lastPushTs
        for (entity in localChanges) {
            try {
                d1ApiService.upsertItem(
                    id = entity.id,
                    name = entity.name,
                    imagePaths = entity.imagePaths,
                    location = entity.location,
                    purchaseDate = entity.purchaseDate,
                    purchasePrice = entity.purchasePrice,
                    usageDays = entity.usageDays,
                    note = entity.note,
                    tags = entity.tags,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
                if (entity.updatedAt > maxUpdatedAt) maxUpdatedAt = entity.updatedAt
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push item ${entity.id} to D1, will retry next sync", AppError.SyncError("D1 push failed for item ${entity.id}", e))
                // 不推进 watermark，下次同步重试
            }
        }

        settingsRepository.setLastPushUpdatedAt(maxUpdatedAt)
        Log.d(TAG, "Incremental push: sent ${localChanges.size} items, watermark = $maxUpdatedAt")
    }

    // ========== 全量拉取（手动恢复） ==========

    /**
     * 从 D1 拉取全部数据写入 Room（INSERT OR REPLACE）。
     * 供设置页手动恢复和首次启动使用。完成后重置 watermark。
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

            // 全量拉取后重置 watermark，让增量同步从当前点继续
            val maxTs = itemDao.getMaxUpdatedAt()
            settingsRepository.setLastPullUpdatedAt(maxTs)
            settingsRepository.setLastPushUpdatedAt(maxTs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull from D1", AppError.SyncError("Full pull from D1 failed", e))
        }
    }

    // ========== 内部工具方法 ==========

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
