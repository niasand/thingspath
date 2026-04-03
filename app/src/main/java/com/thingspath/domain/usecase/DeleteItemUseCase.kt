package com.thingspath.domain.usecase

import android.util.Log
import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import com.thingspath.data.remote.repository.R2ImageRepository
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository,
    private val r2ImageRepository: R2ImageRepository
) {
    suspend operator fun invoke(item: Item) {
        deleteImagesFromR2(item)
        repository.deleteItem(item)
    }

    suspend operator fun invoke(itemId: Long) {
        val item = repository.getItemById(itemId)
        if (item != null) {
            deleteImagesFromR2(item)
        }
        repository.deleteItemById(itemId)
    }

    suspend operator fun invoke(ids: Set<Long>) {
        ids.forEach { id ->
            val item = repository.getItemById(id)
            if (item != null) {
                deleteImagesFromR2(item)
            }
        }
        repository.deleteItemsByIds(ids)
    }

    /**
     * Delete all images associated with this item from R2 storage
     */
    private suspend fun deleteImagesFromR2(item: Item) {
        val imagePaths = item.imagePaths.filter { it.isNotBlank() }
        if (imagePaths.isEmpty()) return

        imagePaths.forEach { path ->
            if (r2ImageRepository.isR2Url(path)) {
                val key = r2ImageRepository.extractKeyFromUrl(path)
                if (key != null) {
                    val success = r2ImageRepository.deleteImage(key)
                    if (success) {
                        Log.d("DeleteItemUseCase", "Deleted R2 image: $key")
                    } else {
                        Log.w("DeleteItemUseCase", "Failed to delete R2 image: $key")
                    }
                }
            } else {
                Log.d("DeleteItemUseCase", "Skipping local file: $path")
            }
        }
    }
}
