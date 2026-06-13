package com.thingspath.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE items ADD COLUMN reminder_date INTEGER")
            db.execSQL("ALTER TABLE items ADD COLUMN reminder_type TEXT")
            db.execSQL("ALTER TABLE items ADD COLUMN reminder_note TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ThingsPathDatabase {
        return Room.databaseBuilder(
            context,
            ThingsPathDatabase::class.java,
            "thingspath.db"
        ).addMigrations(MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideItemDao(database: ThingsPathDatabase): ItemDao {
        return database.itemDao()
    }
}
