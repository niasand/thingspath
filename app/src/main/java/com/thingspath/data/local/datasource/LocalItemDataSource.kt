package com.thingspath.data.local.datasource

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.thingspath.data.local.db.ItemDao
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.local.db.toItem
import com.thingspath.data.model.Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local data source: encapsulates Room database operations and in-memory LRU cache.
 * Single responsibility: local data persistence + cache management.
 */
@Singleton
class LocalItemDataSource @Inject constructor(
    private val itemDao: ItemDao,
    private val application: Application,
    private val imageLoader: ImageLoader
) {
    // LRU cache backed by access-order LinkedHashMap, thread-safe via synchronizedMap
    private val cache = object : LinkedHashMap<Long, Item>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Item>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val cachedItems = Collections.synchronizedMap(cache)

    /**
     * Observe all items from Room, mapped to domain models.
     */
    val items: Flow<List<Item>> = itemDao.getAllItems().map { entities ->
        entities.map { it.toItem() }
    }

    /**
     * Update the in-memory cache from the given item list.
     * Called by the repository when Room Flow emits new data.
     */
    fun refreshCache(itemList: List<Item>) {
        synchronized(cachedItems) {
            cachedItems.clear()
            itemList.forEach { cachedItems[it.id] = it }
        }
    }

    fun getCachedItemById(itemId: Long): Item? = cachedItems[itemId]

    fun clearCache() {
        cachedItems.clear()
    }

    fun getAllItems(): Flow<List<Item>> = items

    fun searchItems(searchQuery: String): Flow<List<Item>> {
        return itemDao.searchItems(searchQuery).map { entities ->
            entities.map { it.toItem() }
        }
    }

    suspend fun getItemById(itemId: Long): Item? {
        return cachedItems[itemId] ?: itemDao.getItemById(itemId)?.toItem()
    }

    suspend fun insertItem(entity: ItemEntity): Long {
        val id = itemDao.insertItem(entity)
        // Invalidate cache for this item so next read picks up fresh data from Room Flow
        cachedItems.remove(id)
        Log.d(TAG, "Inserted item $id to Room")
        return id
    }

    suspend fun insertItems(entities: List<ItemEntity>) {
        itemDao.insertItems(entities)
        // Batch insert: clear entire cache to ensure consistency
        cachedItems.clear()
    }

    suspend fun updateItem(entity: ItemEntity) {
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
        // Invalidate cache entry for updated item
        cachedItems.remove(entity.id)
    }

    suspend fun deleteItemById(id: Long) {
        itemDao.deleteItemById(id)
        cachedItems.remove(id)
    }

    suspend fun deleteItemsByIds(ids: List<Long>) {
        itemDao.deleteItemsByIds(ids)
        ids.forEach { cachedItems.remove(it) }
    }

    suspend fun itemCount(): Int = itemDao.itemCount()

    suspend fun getItemsUpdatedAfter(since: Long): List<ItemEntity> =
        itemDao.getItemsUpdatedAfter(since)

    suspend fun getMaxUpdatedAt(): Long = itemDao.getMaxUpdatedAt()

    /**
     * Pre-fetch remote image URLs into Coil's disk cache for faster UI loading.
     */
    fun prefetchImages(items: List<Item>) {
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

    companion object {
        private const val TAG = "LocalItemDataSource"
        private const val MAX_CACHE_SIZE = 100
    }
}
