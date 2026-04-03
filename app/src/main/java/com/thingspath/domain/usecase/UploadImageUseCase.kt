package com.thingspath.domain.usecase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.thingspath.data.remote.repository.R2ImageRepository
import com.thingspath.util.ItemImageStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

class UploadImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val r2ImageRepository: R2ImageRepository
) {
    suspend operator fun invoke(sourceUri: Uri): String? {
        // 1. Save locally first (fallback + cache)
        val localPath = ItemImageStorage.saveToAppStorage(context, sourceUri)
        if (localPath == null) {
            Log.e("UploadImageUseCase", "Failed to save image locally")
            return null
        }

        // 2. Upload to R2
        val file = File(localPath)
        if (!file.exists()) {
            Log.e("UploadImageUseCase", "Local file does not exist: $localPath")
            return localPath
        }

        val extension = file.extension.takeIf { it.isNotBlank() } ?: "jpg"
        val key = "items/${UUID.randomUUID()}.$extension"
        val remoteUrl = r2ImageRepository.uploadImage(file, key)

        return if (remoteUrl != null) {
            Log.d("UploadImageUseCase", "Uploaded to R2: $remoteUrl")
            remoteUrl
        } else {
            Log.w("UploadImageUseCase", "R2 upload failed, falling back to local path: $localPath")
            localPath
        }
    }
}
