package com.thingspath.ui.screen.home

import com.thingspath.data.model.Item

enum class HomeSortField {
    PurchaseDate,
    Name,
    UsageDays,
    UpdatedAt,
    CreatedAt
}

data class HomeState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val sortField: HomeSortField = HomeSortField.UpdatedAt,
    val sortAscending: Boolean = false,
    val pageSize: Int = 10,
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val showDeleteDialog: Boolean = false,
    val itemToDelete: Item? = null,
    val totalItemCount: Int = 0,
    val totalPrice: Double = 0.0,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isAIProcessing: Boolean = false,
    // Tag Filter
    val allTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    // Batch Actions
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    // List Control
    val scrollToTopSignal: Long = 0L, // Increment to trigger scroll to top
    // Infinite Scroll
    val infiniteScroll: Boolean = true,
    // Pull to refresh
    val isRefreshing: Boolean = false
)
