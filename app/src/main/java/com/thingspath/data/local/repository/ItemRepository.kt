package com.thingspath.data.local.repository

import com.thingspath.data.local.dao.ItemDao
import com.thingspath.data.local.entity.ItemEntity
import com.thingspath.data.model.Item
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepository @Inject constructor(
    private val itemDao: ItemDao,
    private val fileRepository: FileRepository
) {
    private val gson = Gson()

    fun getAllItems(): Flow<List<Item>> {
        return itemDao.getAllItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun searchItems(searchQuery: String): Flow<List<Item>> {
        return itemDao.searchItems(searchQuery).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getItemById(itemId: Long): Item? {
        return itemDao.getItemById(itemId)?.toDomainModel()
    }

    suspend fun insertItem(item: Item): Long {
        val entity = item.toEntity()
        val id = itemDao.insertItem(entity)
        backupData()
        return id
    }

    suspend fun insertItems(items: List<Item>) {
        val entities = items.map { it.toEntity() }
        itemDao.insertItems(entities)
        backupData()
    }

    suspend fun updateItem(item: Item) {
        val entity = item.toEntity()
        itemDao.updateItem(entity)
        backupData()
    }

    suspend fun deleteItem(item: Item) {
        val entity = item.toEntity()
        itemDao.deleteItem(entity)
        backupData()
    }

    suspend fun deleteItemById(itemId: Long) {
        itemDao.deleteItemById(itemId)
        backupData()
    }

    suspend fun deleteAllItems() {
        itemDao.deleteAllItems()
        backupData()
    }

    private suspend fun backupData() {
        try {
            val entities = itemDao.getAllItemsSync()
            val items = entities.map { it.toDomainModel() }
            val json = gson.toJson(items)
            fileRepository.saveBackup(json)
        } catch (e: Exception) {
            Log.e("ItemRepository", "Backup failed", e)
        }
    }

    suspend fun restoreDataIfNeeded() {
        try {
            if (itemDao.getAllItemsSync().isEmpty()) {
                val json = fileRepository.readBackup()
                if (json != null) {
                    val type = object : TypeToken<List<Item>>() {}.type
                    val items: List<Item> = gson.fromJson(json, type)
                    if (items.isNotEmpty()) {
                        val entities = items.map { it.toEntity() }
                        itemDao.insertItems(entities)
                        Log.d("ItemRepository", "Restored ${items.size} items from backup")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ItemRepository", "Restore failed", e)
        }
    }
}

// Extension functions for mapping between Entity and Domain model
private fun ItemEntity.toDomainModel(): Item {
    return Item(
        id = id,
        name = name,
        imagePath = imagePath,
        location = location,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        usageDays = usageDays,
        note = note,
        tags = if (tags.isBlank()) emptyList() else tags.split(","),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        name = name,
        imagePath = imagePath,
        location = location,
        purchaseDate = purchaseDate,
        purchasePrice = purchasePrice,
        usageDays = usageDays,
        note = note,
        tags = tags.joinToString(","),
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}
