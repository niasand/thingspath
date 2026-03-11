package com.thingspath.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thingspath.data.local.repository.FileRepository
import com.thingspath.data.local.repository.ItemRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val itemRepository: ItemRepository,
    private val fileRepository: FileRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("BackupWorker", "Starting scheduled backup...")
            
            // Get all items from repository
            val items = itemRepository.getAllItems().first()
            
            if (items.isNotEmpty()) {
                val gson = Gson()
                val json = gson.toJson(items)
                
                // Save to backup file (this overwrites existing backup)
                fileRepository.saveBackup(json)
                
                Log.d("BackupWorker", "Scheduled backup completed successfully. ${items.size} items backed up.")
                Result.success()
            } else {
                Log.d("BackupWorker", "No items to backup.")
                Result.success()
            }
        } catch (e: Exception) {
            Log.e("BackupWorker", "Scheduled backup failed", e)
            Result.retry()
        }
    }
}