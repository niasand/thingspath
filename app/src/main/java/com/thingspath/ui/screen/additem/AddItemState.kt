package com.thingspath.ui.screen.additem

data class AddItemState(
    val name: String = "",
    val location: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val usageDays: String = "",
    val note: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val imagePath: String? = null,
    val isLoading: Boolean = false,
    val nameError: String? = null
)
