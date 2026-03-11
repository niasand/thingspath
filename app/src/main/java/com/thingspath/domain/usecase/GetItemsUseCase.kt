package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetItemsUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    operator fun invoke(searchQuery: String? = null): Flow<List<Item>> {
        return if (searchQuery.isNullOrEmpty()) {
            repository.getAllItems()
        } else {
            // Manually add wildcards to ensure compatibility and consistent behavior
            val dbQuery = "%$searchQuery%"
            repository.searchItems(dbQuery)
        }
    }
}
