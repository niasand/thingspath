package com.thingspath.data.local.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun writeString(uri: Uri, content: String) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
        }
    }

    suspend fun readString(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            stringBuilder.toString()
        }
    }

    suspend fun saveBackup(content: String) {
        withContext(Dispatchers.IO) {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val appDir = File(documentsDir, "ThingsPath")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val backupFile = File(appDir, "backup.json")
                FileOutputStream(backupFile).use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                Log.d("FileRepository", "Backup saved to ${backupFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("FileRepository", "Error saving backup", e)
            }
        }
    }

    suspend fun readBackup(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val backupFile = File(documentsDir, "ThingsPath/backup.json")
                if (backupFile.exists()) {
                    val stringBuilder = StringBuilder()
                    FileInputStream(backupFile).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }
                    }
                    Log.d("FileRepository", "Backup read from ${backupFile.absolutePath}")
                    stringBuilder.toString()
                } else {
                    Log.d("FileRepository", "Backup file not found at ${backupFile.absolutePath}")
                    null
                }
            } catch (e: Exception) {
                Log.e("FileRepository", "Error reading backup", e)
                null
            }
        }
    }
}
