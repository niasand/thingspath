package com.thingspath.ui.screen.home

import com.thingspath.data.model.Item

data class HomeState(
    val items: List<Item> = emptyList(),
    val isLoading: Boolean = false,
    val isGridView: Boolean = false,
    val searchQuery: String = "",
    val showDeleteDialog: Boolean = false,
    val itemToDelete: Item? = null,
    val totalItemCount: Int = 0,
    val totalPrice: Double = 0.0,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null
)
