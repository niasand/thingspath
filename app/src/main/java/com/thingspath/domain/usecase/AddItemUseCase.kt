package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import javax.inject.Inject

class AddItemUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(item: Item): Long {
        return repository.insertItem(item)
    }
}
