package com.thingspath.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class], version = 1, exportSchema = false)
abstract class ThingsPathDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
