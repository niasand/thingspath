package com.thingspath.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY updated_at DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE name LIKE :query OR tags LIKE :query OR location LIKE :query OR note LIKE :query ORDER BY updated_at DESC")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Query("""
        UPDATE items SET name = :name, image_paths = :imagePaths, image_path = :imagePath,
        location = :location, purchase_date = :purchaseDate, purchase_price = :purchasePrice,
        usage_days = :usageDays, note = :note, tags = :tags, updated_at = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateItem(
        id: Long,
        name: String,
        imagePaths: String,
        imagePath: String?,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        updatedAt: Long
    )

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun deleteItemsByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM items")
    suspend fun itemCount(): Int
}
