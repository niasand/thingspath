package com.thingspath.ui.screen.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.model.Item
import com.thingspath.domain.usecase.AddItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.lifecycle.SavedStateHandle

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(AddItemState())
    val state: StateFlow<AddItemState> = _state.asStateFlow()

    init {
        val name = savedStateHandle.get<String>("name")
        val price = savedStateHandle.get<String>("price")
        val date = savedStateHandle.get<String>("date")
        val location = savedStateHandle.get<String>("location")
        val note = savedStateHandle.get<String>("note")
        val tagsString = savedStateHandle.get<String>("tags")

        if (name != null || price != null || date != null) {
            _state.update {
                it.copy(
                    name = name ?: "",
                    purchasePrice = price ?: "",
                    purchaseDate = date ?: "",
                    location = location ?: "",
                    note = note ?: "",
                    tags = tagsString?.split(",")?.filter { tag -> tag.isNotBlank() } ?: emptyList()
                )
            }
            // Recalculate usage days if date is present
            if (date != null) {
                viewModelScope.launch {
                    calculateUsageDays(date)
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, nameError = null) }
    }

    fun onLocationChange(value: String) {
        _state.update { it.copy(location = value) }
    }

    fun onPurchaseDateChange(value: String) {
        _state.update { it.copy(purchaseDate = value) }
        calculateUsageDays(value)
    }

    fun onPurchasePriceChange(value: String) {
        // Allow digits and one decimal point
        if (value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
            _state.update { it.copy(purchasePrice = value) }
        }
    }

    private fun calculateUsageDays(purchaseDateStr: String) {
        if (purchaseDateStr.isBlank()) return
        
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val purchaseDate = sdf.parse(purchaseDateStr)
            if (purchaseDate != null) {
                val diff = System.currentTimeMillis() - purchaseDate.time
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                if (days >= 0) {
                    _state.update { it.copy(usageDays = days.toString()) }
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors while typing
        }
    }

    fun onUsageDaysChange(value: String) {
        _state.update { it.copy(usageDays = value.filter { it.isDigit() }) }
    }

    fun onNoteChange(value: String) {
        _state.update { it.copy(note = value) }
    }

    fun onTagInputChange(value: String) {
        _state.update { it.copy(tagInput = value) }
    }

    fun addTag() {
        val tag = _state.value.tagInput.trim()
        if (tag.isNotEmpty() && !state.value.tags.contains(tag)) {
            _state.update { 
                it.copy(
                    tags = it.tags + tag,
                    tagInput = ""
                ) 
            }
        }
    }

    fun removeTag(tag: String) {
        _state.update { it.copy(tags = it.tags - tag) }
    }

    fun addImage(path: String) {
        _state.update { it.copy(imagePaths = it.imagePaths + path) }
    }

    fun removeImage(index: Int) {
        _state.update { it.copy(imagePaths = it.imagePaths.filterIndexed { idx, _ -> idx != index }) }
    }

    fun validateForm(): Boolean {
        val name = _state.value.name.trim()
        if (name.isEmpty()) {
            _state.update { it.copy(nameError = "Name is required") }
            return false
        }
        return true
    }

    fun saveItem(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!validateForm()) {
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val item = Item(
                    name = _state.value.name.trim(),
                    location = _state.value.location.trim().takeIf { it.isNotBlank() },
                    purchaseDate = parsePurchaseDate(_state.value.purchaseDate),
                    purchasePrice = _state.value.purchasePrice.toDoubleOrNull() ?: 0.0,
                    usageDays = _state.value.usageDays.toIntOrNull(),
                    note = _state.value.note.trim().takeIf { it.isNotBlank() },
                    tags = _state.value.tags,
                    imagePaths = _state.value.imagePaths,
                    imagePath = _state.value.imagePaths.firstOrNull()
                )
                addItemUseCase(item)
                _state.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                onError(e.message ?: "Failed to save item")
            }
        }
    }

    fun clearForm() {
        _state.update { AddItemState() }
    }

    private fun parsePurchaseDate(dateString: String): Long? {
        if (dateString.isBlank()) return null
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
}
