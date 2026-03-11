package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(item: Item) {
        repository.deleteItem(item)
    }

    suspend operator fun invoke(itemId: Long) {
        repository.deleteItemById(itemId)
    }
}
