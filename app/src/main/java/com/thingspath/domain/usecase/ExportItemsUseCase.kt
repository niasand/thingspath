package com.thingspath.domain.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thingspath.data.model.Item
import com.thingspath.data.local.repository.ItemRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository
) {
    suspend operator fun invoke(): String {
        val items = itemRepository.getAllItems().first()
        val gson = Gson()
        return gson.toJson(items)
    }
}
