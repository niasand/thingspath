package com.thingspath.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ItemEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ThingsPathDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
}
