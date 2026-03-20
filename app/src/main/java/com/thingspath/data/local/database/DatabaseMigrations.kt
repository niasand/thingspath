package com.thingspath.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 所有 Room 数据库显式迁移定义。
 *
 * 迁移策略：采用"幂等补全"——通过 PRAGMA table_info 检查列是否存在，
 * 仅在缺失时才执行 ALTER TABLE，确保无论当前处于哪个升级路径都不会
 * 破坏用户数据。
 *
 * 添加新版本时，请在此文件新增 MIGRATION_X_Y 并在 DatabaseModule 中注册。
 */
object DatabaseMigrations {

    private fun SupportSQLiteDatabase.addColumnIfNotExists(
        table: String,
        column: String,
        definition: String
    ) {
        val cursor = query("PRAGMA table_info($table)")
        val nameIndex = cursor.getColumnIndex("name")
        var exists = false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) {
                exists = true
                break
            }
        }
        cursor.close()
        if (!exists) {
            execSQL("ALTER TABLE $table ADD COLUMN $definition")
        }
    }

    /** v1 → v2：补全可能新增的 usageDays、tags 列 */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.addColumnIfNotExists("items", "usageDays", "usageDays INTEGER")
            database.addColumnIfNotExists("items", "tags", "tags TEXT NOT NULL DEFAULT ''")
        }
    }

    /** v2 → v3：补全可能新增的 note、location 列 */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.addColumnIfNotExists("items", "note", "note TEXT")
            database.addColumnIfNotExists("items", "location", "location TEXT")
        }
    }
}
