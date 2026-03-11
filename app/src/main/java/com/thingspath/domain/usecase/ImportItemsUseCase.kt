package com.thingspath.domain.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.model.Item
import com.thingspath.data.local.repository.ItemRepository
import javax.inject.Inject

class ImportItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(jsonString: String) {
        val gson = Gson()
        val itemType = object : TypeToken<List<Item>>() {}.type
        val items: List<Item> = gson.fromJson(jsonString, itemType)
        
        // Reset IDs to 0 to treat them as new items and avoid conflicts
        // Note: This will result in duplicates if importing the same data multiple times
        val itemsToInsert = items.map { it.copy(id = 0) }
        
        itemRepository.insertItems(itemsToInsert)
    }
}
