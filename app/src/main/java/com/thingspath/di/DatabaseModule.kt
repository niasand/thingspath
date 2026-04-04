package com.thingspath.di

import android.content.Context
import androidx.room.Room
import com.thingspath.data.local.db.ItemDao
import com.thingspath.data.local.db.ThingsPathDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ThingsPathDatabase {
        return Room.databaseBuilder(
            context,
            ThingsPathDatabase::class.java,
            "thingspath.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideItemDao(database: ThingsPathDatabase): ItemDao {
        return database.itemDao()
    }
}
