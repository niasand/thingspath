package com.thingspath.ui.screen.itemdetail

import com.thingspath.data.model.Item

data class ItemDetailState(
    val item: Item? = null,
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val name: String = "",
    val location: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val usageDays: String = "",
    val note: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val imagePath: String? = null,
    val showDeleteDialog: Boolean = false,
    val isImageFullScreen: Boolean = false
)
