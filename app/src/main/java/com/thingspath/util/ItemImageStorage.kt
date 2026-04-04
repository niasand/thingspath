package com.thingspath.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object ItemImageStorage {
    private const val ALBUM_NAME = "ThingsPath"

    /**
     * 保存图片到应用私有目录，避免在相册中创建重复图片
     */
    fun saveToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            // 优先保存到应用私有目录，不污染相册
            saveToPrivateDir(context, sourceUri)
        } catch (e: Exception) {
            try {
                saveToCache(context, sourceUri)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 保存到应用私有 Pictures 目录（推荐）
     * 图片只在应用内部可见，不会出现在相册中
     */
    private fun saveToPrivateDir(context: Context, sourceUri: Uri): String? {
        val privatePicsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "items").apply { mkdirs() }
        val destFile = File(privatePicsDir, "${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input) ?: return null
            destFile.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }
            bitmap.recycle()
        } ?: return null

        return destFile.absolutePath
    }

    /**
     * 保存到相册（Public Pictures）- 仅用于导出/分享功能
     */
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

