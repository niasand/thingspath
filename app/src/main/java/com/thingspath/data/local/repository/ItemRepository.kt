package com.thingspath.data.local.repository

import android.util.Log
import com.google.gson.Gson
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.db.toEntity
import com.thingspath.data.local.datasource.LocalItemDataSource
import com.thingspath.data.model.Item
import com.thingspath.data.remote.datasource.RemoteItemDataSource
import com.thingspath.domain.model.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade / coordinator for item data operations.
 *
 * Delegates to [LocalItemDataSource] for Room + cache operations,
 * and [RemoteItemDataSource] for D1 API operations.
 * Owns the sync coordination logic (when to push/pull between local and remote).
 *
 * Public API is unchanged — all existing use cases and ViewModels work without modification.
 */
@Singleton
class ItemRepository @Inject constructor(
    private val localDataSource: LocalItemDataSource,
    private val remoteDataSource: RemoteItemDataSource,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val items: Flow<List<Item>> = localDataSource.items

    init {
        scope.launch {
            items.collect { itemList ->
                localDataSource.refreshCache(itemList)
                localDataSource.prefetchImages(itemList)
            }
        }

        scope.launch {
            try {
                remoteDataSource.ensureTableExists()
                val count = localDataSource.itemCount()
                if (count == 0) {
                    Log.d(TAG, "Room is empty, doing full pull from D1...")
                    pullFromD1()
                    val maxTs = localDataSource.getMaxUpdatedAt()
                    settingsRepository.setLastPullUpdatedAt(maxTs)
                    settingsRepository.setLastPushUpdatedAt(maxTs)
                } else {
                    Log.d(TAG, "Room has $count items, doing incremental sync...")
                    incrementalSync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize", AppError.SyncError("Init sync failed", e))
            }
        }
    }

    // ========== Public API (unchanged signatures) ==========

    suspend fun refreshFromRemote() = incrementalSync()

    fun getAllItems(): Flow<List<Item>> = localDataSource.getAllItems()

    fun getCachedItemById(itemId: Long): Item? = localDataSource.getCachedItemById(itemId)

    fun searchItems(searchQuery: String): Flow<List<Item>> =
        localDataSource.searchItems(searchQuery)

    suspend fun getItemById(itemId: Long): Item? = localDataSource.getItemById(itemId)

    suspend fun insertItem(item: Item): Long {
        return withContext(Dispatchers.IO) {
            val id = remoteDataSource.insertItem(
                name = item.name,
                imagePaths = gson.toJson(item.imagePaths),
                location = item.location,
                purchaseDate = item.purchaseDate,
                purchasePrice = item.purchasePrice,
                usageDays = item.usageDays,
                reminderDate = item.reminderDate,
                reminderType = item.reminderType,
                reminderNote = item.reminderNote,
                note = item.note,
                tags = gson.toJson(item.tags),
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
            val entity = item.copy(id = id).toEntity()
            localDataSource.insertItem(entity)
            Log.d(TAG, "Inserted item $id to Room + D1")
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
            val now = System.currentTimeMillis()
            val entity = item.copy(updatedAt = now).toEntity()
            localDataSource.updateItem(entity)
            try {
                val updated = remoteDataSource.updateItem(
                    id = item.id,
                    name = item.name,
                    imagePaths = gson.toJson(item.imagePaths),
                    location = item.location,
                    purchaseDate = item.purchaseDate,
                    purchasePrice = item.purchasePrice,
                    usageDays = item.usageDays,
                    reminderDate = item.reminderDate,
                    reminderType = item.reminderType,
                    reminderNote = item.reminderNote,
                    note = item.note,
                    tags = gson.toJson(item.tags),
                    updatedAt = now
                )
                if (!updated) {
                    Log.w(TAG, "D1 UPDATE matched 0 rows for item ${item.id}, trying INSERT")
                    remoteDataSource.insertItem(
                        id = item.id,
                        name = item.name,
                        imagePaths = gson.toJson(item.imagePaths),
                        location = item.location,
                        purchaseDate = item.purchaseDate,
                        purchasePrice = item.purchasePrice,
                        usageDays = item.usageDays,
                        reminderDate = item.reminderDate,
                        reminderType = item.reminderType,
                        reminderNote = item.reminderNote,
                        note = item.note,
                        tags = gson.toJson(item.tags),
                        createdAt = item.createdAt,
                        updatedAt = now
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync item ${item.id} to D1", AppError.SyncError("D1 update failed for item ${item.id}", e))
            }
        }
    }

    suspend fun deleteItem(item: Item) {
        withContext(Dispatchers.IO) {
            localDataSource.deleteItemById(item.id)
            try {
                remoteDataSource.deleteItemById(item.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync delete to D1 for item ${item.id}", AppError.SyncError("D1 delete failed", e))
            }
        }
    }

    suspend fun deleteItemById(itemId: Long) {
        withContext(Dispatchers.IO) {
            localDataSource.deleteItemById(itemId)
            try {
                remoteDataSource.deleteItemById(itemId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync delete to D1 for item $itemId", AppError.SyncError("D1 delete failed", e))
            }
        }
    }

    suspend fun deleteItemsByIds(ids: Set<Long>) {
        withContext(Dispatchers.IO) {
            localDataSource.deleteItemsByIds(ids.toList())
            try {
                remoteDataSource.deleteItemsByIds(ids.toList())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync bulk delete to D1", AppError.SyncError("D1 bulk delete failed", e))
            }
        }
    }

    suspend fun deleteAllItems() {
        withContext(Dispatchers.IO) {
            localDataSource.items.first().forEach { item ->
                localDataSource.deleteItemById(item.id)
            }
        }
    }

    fun clearCache() = localDataSource.clearCache()

    // ========== Incremental sync ==========

    private suspend fun incrementalSync() {
        incrementalPull()
        incrementalPush()
    }

    private suspend fun incrementalPull() {
        val lastPullTs = settingsRepository.lastPullUpdatedAt.first()
        val entities = remoteDataSource.getItemsUpdatedAfter(lastPullTs)

        if (entities.isEmpty()) {
            Log.d(TAG, "Incremental pull: no new items from D1 since $lastPullTs")
            return
        }

        var maxUpdatedAt = lastPullTs
        for (entity in entities) {
            if (entity.updatedAt > maxUpdatedAt) maxUpdatedAt = entity.updatedAt
        }

        localDataSource.insertItems(entities)
        settingsRepository.setLastPullUpdatedAt(maxUpdatedAt)
        Log.d(TAG, "Incremental pull: fetched ${entities.size} items, watermark = $maxUpdatedAt")
    }

    private suspend fun incrementalPush() {
        val lastPushTs = settingsRepository.lastPushUpdatedAt.first()
        val localChanges = localDataSource.getItemsUpdatedAfter(lastPushTs)

        if (localChanges.isEmpty()) {
            Log.d(TAG, "Incremental push: no local changes since $lastPushTs")
            return
        }

        var maxUpdatedAt = lastPushTs
        for (entity in localChanges) {
            try {
                remoteDataSource.upsertItem(
                    id = entity.id,
                    name = entity.name,
                    imagePaths = gson.toJson(entity.imagePaths),
                    location = entity.location,
                    purchaseDate = entity.purchaseDate,
                    purchasePrice = entity.purchasePrice,
                    usageDays = entity.usageDays,
                    reminderDate = entity.reminderDate,
                    reminderType = entity.reminderType,
                    reminderNote = entity.reminderNote,
                    note = entity.note,
                    tags = gson.toJson(entity.tags),
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
                if (entity.updatedAt > maxUpdatedAt) maxUpdatedAt = entity.updatedAt
            } catch (e: Exception) {
                Log.e(TAG, "Failed to push item ${entity.id} to D1", AppError.SyncError("D1 push failed for item ${entity.id}", e))
            }
        }

        settingsRepository.setLastPushUpdatedAt(maxUpdatedAt)
        Log.d(TAG, "Incremental push: sent ${localChanges.size} items, watermark = $maxUpdatedAt")
    }

    // ========== Full pull (manual restore) ==========

    suspend fun pullFromD1() {
        try {
            val entities = remoteDataSource.getAllItems()
            localDataSource.insertItems(entities)
            Log.d(TAG, "Pulled ${entities.size} items from D1 to Room")

            val maxTs = localDataSource.getMaxUpdatedAt()
            settingsRepository.setLastPullUpdatedAt(maxTs)
            settingsRepository.setLastPushUpdatedAt(maxTs)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull from D1", AppError.SyncError("Full pull from D1 failed", e))
        }
    }

    companion object {
        private const val TAG = "ItemRepository"
    }
}
