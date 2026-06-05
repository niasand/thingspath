package com.thingspath.ui.screen.home

import com.thingspath.data.model.Item

enum class HomeSortField {
    PurchaseDate,
    Name,
    UsageDays,
    UpdatedAt,
    CreatedAt
}

data class ListState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val totalItemCount: Int = 0,
    val totalPrice: Double = 0.0
)

data class FilterState(
    val searchQuery: String = "",
    val sortField: HomeSortField = HomeSortField.UpdatedAt,
    val sortAscending: Boolean = false,
    val allTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet()
)

data class HomeState(
    val listState: ListState = ListState(),
    val filterState: FilterState = FilterState(),
    // Dialog
    val showDeleteDialog: Boolean = false,
    val itemToDelete: Item? = null,
    // Import / Export
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    // Messages
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    // AI
    val isAIProcessing: Boolean = false,
    // Batch Actions
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    // List Control
    val scrollToTopSignal: Long = 0L,
    // Infinite Scroll
    val infiniteScroll: Boolean = true,
    // Pull to refresh
    val isRefreshing: Boolean = false,
    // Convenience: page size stored at top level for settings sync
    val pageSize: Int = 10
)
