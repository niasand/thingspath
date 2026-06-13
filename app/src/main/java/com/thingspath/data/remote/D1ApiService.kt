package com.thingspath.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.thingspath.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class D1QueryRequest(
    val sql: String,
    val params: List<Any?>? = null
)

data class D1Result<T>(
    val results: List<T>,
    val success: Boolean,
    val errors: List<D1Error>?,
    val meta: D1Meta?
)

data class D1Error(
    val code: Int,
    val message: String
)

data class D1Meta(
    val changed_db: Boolean,
    val changes: Long,
    val duration: Long,
    val last_row_id: Long,
    val rows_read: Long,
    val rows_written: Long,
    val size_after: Long
)

@Singleton
class D1ApiService @Inject constructor() {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://api.cloudflare.com/client/v4/accounts/${BuildConfig.D1_ACCOUNT_ID}/d1/database/${BuildConfig.D1_DATABASE_ID}/query"
    private val token = BuildConfig.D1_API_TOKEN

    suspend fun executeQuery(sql: String, params: List<Any?>? = null): String {
        return withContext(Dispatchers.IO) {
            val body = D1QueryRequest(sql, params)
            val json = gson.toJson(body)
            Log.d(TAG, "D1 SQL: $sql | params: $params")

            val request = Request.Builder()
                .url(baseUrl)
                .post(json.toRequestBody("application/json".toMediaType()))
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    Log.e(TAG, "D1 query failed: ${response.code} - $responseBody")
                    throw RuntimeException("D1 query failed: ${response.code}")
                }
                Log.d(TAG, "D1 response: $responseBody")
                responseBody
            }
        }
    }

    suspend fun getAllItems(): List<Map<String, Any?>> {
        val responseBody = executeQuery("SELECT * FROM items ORDER BY updated_at DESC")
        return parseResults(responseBody)
    }

    suspend fun getItemsUpdatedAfter(since: Long): List<Map<String, Any?>> {
        val responseBody = executeQuery(
            "SELECT * FROM items WHERE updated_at > ? ORDER BY updated_at ASC",
            listOf(since)
        )
        return parseResults(responseBody)
    }

    suspend fun getMaxUpdatedAt(): Long {
        val responseBody = executeQuery("SELECT COALESCE(MAX(updated_at), 0) as max_ts FROM items")
        val results = parseResults(responseBody)
        return (results.firstOrNull()?.get("max_ts") as? Number)?.toLong() ?: 0L
    }

    suspend fun upsertItem(
        id: Long,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        createdAt: Long,
        updatedAt: Long,
        reminderDate: Long? = null,
        reminderType: String? = null,
        reminderNote: String? = null
    ) {
        executeQuery(
            """INSERT OR REPLACE INTO items (id, name, image_paths, location, purchase_date,
               purchase_price, usage_days, reminder_date, reminder_type, reminder_note, note, tags, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            listOf(id, name, imagePaths, location, purchaseDate, purchasePrice, usageDays, reminderDate, reminderType, reminderNote, note, tags, createdAt, updatedAt)
        )
    }

    suspend fun getItemById(itemId: Long): List<Map<String, Any?>> {
        val responseBody = executeQuery("SELECT * FROM items WHERE id = ?", listOf(itemId))
        return parseResults(responseBody)
    }

    suspend fun searchItems(query: String): List<Map<String, Any?>> {
        val searchPattern = "%$query%"
        val responseBody = executeQuery(
            "SELECT * FROM items WHERE name LIKE ? OR tags LIKE ? OR location LIKE ? OR note LIKE ? ORDER BY updated_at DESC",
            listOf(searchPattern, searchPattern, searchPattern, searchPattern)
        )
        return parseResults(responseBody)
    }

    suspend fun insertItem(
        id: Long? = null,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        createdAt: Long,
        updatedAt: Long,
        reminderDate: Long? = null,
        reminderType: String? = null,
        reminderNote: String? = null
    ): Long {
        val sql = if (id != null) {
            """INSERT INTO items (id, name, image_paths, location, purchase_date, purchase_price, usage_days, reminder_date, reminder_type, reminder_note, note, tags, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        } else {
            """INSERT INTO items (name, image_paths, location, purchase_date, purchase_price, usage_days, reminder_date, reminder_type, reminder_note, note, tags, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
        }
        val params = if (id != null) {
            listOf(id, name, imagePaths, location, purchaseDate, purchasePrice, usageDays, reminderDate, reminderType, reminderNote, note, tags, createdAt, updatedAt)
        } else {
            listOf(name, imagePaths, location, purchaseDate, purchasePrice, usageDays, reminderDate, reminderType, reminderNote, note, tags, createdAt, updatedAt)
        }
        val responseBody = executeQuery(sql, params)
        val result = parseD1Result(responseBody)
        return id ?: result?.meta?.last_row_id ?: 0
    }

    suspend fun updateItem(
        id: Long,
        name: String,
        imagePaths: String,
        location: String?,
        purchaseDate: Long?,
        purchasePrice: Double,
        usageDays: Int?,
        note: String?,
        tags: String,
        updatedAt: Long,
        reminderDate: Long? = null,
        reminderType: String? = null,
        reminderNote: String? = null
    ): Boolean {
        val responseBody = executeQuery(
            """UPDATE items SET name = ?, image_paths = ?, location = ?, purchase_date = ?,
               purchase_price = ?, usage_days = ?, reminder_date = ?, reminder_type = ?, reminder_note = ?,
               note = ?, tags = ?, updated_at = ? WHERE id = ?""",
            listOf(name, imagePaths, location, purchaseDate, purchasePrice, usageDays, reminderDate, reminderType, reminderNote, note, tags, updatedAt, id)
        )
        val result = parseD1Result(responseBody)
        return result?.meta?.changes ?: 0 > 0
    }

    suspend fun deleteItemById(itemId: Long) {
        executeQuery("DELETE FROM items WHERE id = ?", listOf(itemId))
    }

    suspend fun deleteItemsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        executeQuery("DELETE FROM items WHERE id IN ($placeholders)", ids)
    }

    suspend fun deleteAllItems() {
        executeQuery("DELETE FROM items")
    }

    suspend fun createTableIfNotExists() {
        executeQuery(
            """CREATE TABLE IF NOT EXISTS items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                image_paths TEXT DEFAULT '[]',
                location TEXT,
                purchase_date INTEGER,
                purchase_price REAL DEFAULT 0,
                usage_days INTEGER,
                reminder_date INTEGER,
                reminder_type TEXT,
                reminder_note TEXT,
                note TEXT,
                tags TEXT DEFAULT '[]',
                created_at INTEGER,
                updated_at INTEGER
            )"""
        )
        addColumnIfMissing("reminder_date", "INTEGER")
        addColumnIfMissing("reminder_type", "TEXT")
        addColumnIfMissing("reminder_note", "TEXT")
    }

    private suspend fun addColumnIfMissing(name: String, type: String) {
        try {
            executeQuery("ALTER TABLE items ADD COLUMN $name $type")
        } catch (e: Exception) {
            Log.d(TAG, "D1 column $name already exists or cannot be added")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseResults(responseBody: String): List<Map<String, Any?>> {
        return try {
            val root = gson.fromJson(responseBody, Map::class.java) as Map<String, Any?>
            val resultArray = root["result"] as? List<Map<String, Any?>>
            val firstResult = resultArray?.firstOrNull() ?: return emptyList()
            (firstResult["results"] as? List<Map<String, Any?>>) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse D1 results", e)
            emptyList()
        }
    }

    private fun parseD1Result(responseBody: String): D1Result<*>? {
        return try {
            val root = gson.fromJson(responseBody, Map::class.java) as Map<String, Any?>
            val resultArray = root["result"] as? List<Map<String, Any?>>
            val firstResult = resultArray?.firstOrNull() ?: return null
            val results = firstResult["results"] as? List<*>
            val success = firstResult["success"] as? Boolean ?: false
            val meta = firstResult["meta"] as? Map<String, Any?>

            D1Result(
                results = results ?: emptyList(),
                success = success,
                errors = null,
                meta = meta?.let {
                    D1Meta(
                        changed_db = it["changed_db"] as? Boolean ?: false,
                        changes = (it["changes"] as? Number)?.toLong() ?: 0,
                        duration = (it["duration"] as? Number)?.toLong() ?: 0,
                        last_row_id = (it["last_row_id"] as? Number)?.toLong() ?: 0,
                        rows_read = (it["rows_read"] as? Number)?.toLong() ?: 0,
                        rows_written = (it["rows_written"] as? Number)?.toLong() ?: 0,
                        size_after = (it["size_after"] as? Number)?.toLong() ?: 0
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse D1 response", e)
            null
        }
    }

    companion object {
        private const val TAG = "D1ApiService"
    }
}
