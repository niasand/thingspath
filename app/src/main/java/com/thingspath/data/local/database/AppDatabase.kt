package com.thingspath.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thingspath.data.local.dao.ItemDao
import com.thingspath.data.local.entity.ItemEntity

@Database(
    entities = [ItemEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
