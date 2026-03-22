package com.thingspath.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object ItemImageStorage {
    private const val ALBUM_NAME = "ThingsPath"

    /**
     * Creates a temporary content URI for the camera app to write a captured photo into.
     * Uses FileProvider so the camera app is granted write access to app's cache directory.
     */
    fun createCameraImageUri(context: Context): Uri {
        val cacheDir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val imageFile = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    fun saveToAlbum(context: Context, sourceUri: Uri): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToAlbumQ(context, sourceUri)
            } else {
                saveToAlbumLegacy(context, sourceUri)
            }
        } catch (e: Exception) {
            try {
                saveToCache(context, sourceUri)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun saveToAlbumQ(context: Context, sourceUri: Uri): String? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri) ?: "image/jpeg"
        val extension = mimeTypeToExtension(mimeType)
        val displayName = "thingspath_${System.currentTimeMillis()}.$extension"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destUri = resolver.insert(collection, values) ?: return null

        resolver.openInputStream(sourceUri)?.use { input ->
            resolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output)
            } ?: return null
        } ?: return null

        val doneValues = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(destUri, doneValues, null, null)
        return destUri.toString()
    }

    private fun saveToAlbumLegacy(context: Context, sourceUri: Uri): String? {
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
        val extension = mimeTypeToExtension(mimeType)
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, ALBUM_NAME).apply { mkdirs() }
        val destFile = File(albumDir, "thingspath_${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        MediaScannerConnection.scanFile(
            context,
            arrayOf(destFile.absolutePath),
            arrayOf(mimeType),
            null
        )
        return destFile.absolutePath
    }

    private fun saveToCache(context: Context, sourceUri: Uri): String? {
        val cacheDir = File(context.cacheDir, "item_images").apply { mkdirs() }
        val mimeType = context.contentResolver.getType(sourceUri) ?: "image/jpeg"
        val extension = mimeTypeToExtension(mimeType)
        val destFile = File(cacheDir, "${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        return destFile.absolutePath
    }

    private fun mimeTypeToExtension(mimeType: String): String {
        return when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic", "image/heif" -> "heic"
            else -> "jpg"
        }
    }
}

