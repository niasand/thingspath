package com.thingspath.ui.screen.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItemByIdUseCase: GetItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0L

    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    init {
        if (itemId > 0) {
            loadItem()
        }
    }

    private fun loadItem() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                val item = getItemByIdUseCase(itemId)
                item?.let { loadedItem ->
                    _state.update {
                        it.copy(
                            item = loadedItem,
                            isLoading = false,
                            name = loadedItem.name,
                            location = loadedItem.location ?: "",
                            purchaseDate = formatPurchaseDate(loadedItem.purchaseDate),
                            purchasePrice = loadedItem.purchasePrice.takeIf { it > 0 }?.toString() ?: "",
                            usageDays = loadedItem.usageDays?.toString() ?: "",
                            note = loadedItem.note ?: "",
                            tags = loadedItem.tags,
                            imagePath = loadedItem.imagePath
                        )
                    }
                } ?: run {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value) }
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
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val purchaseCal = java.util.Calendar.getInstance().apply {
                    time = purchaseDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                val diff = today.timeInMillis - purchaseCal.timeInMillis
                val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                if (days >= 0) {
                    _state.update { it.copy(usageDays = days.toString()) }
                }
            }
        } catch (e: Exception) {}
    }

    fun onUsageDaysChange(value: String) {
        val daysStr = value.filter { it.isDigit() }
        val days = daysStr.toLongOrNull()
        
        _state.update { currentState ->
            var newState = currentState.copy(usageDays = daysStr)
            if (days != null) {
                try {
                    val calendar = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                        add(java.util.Calendar.DAY_OF_YEAR, -days.toInt())
                    }
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    newState = newState.copy(purchaseDate = sdf.format(calendar.time))
                } catch (e: Exception) {}
            }
            newState
        }
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

    fun onImagePathChange(path: String?) {
        _state.update { it.copy(imagePath = path) }
    }

    fun toggleEditMode() {
        _state.update { it.copy(isEditing = !it.isEditing) }
    }

    fun showDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun showFullScreenImage() {
        _state.update { it.copy(isImageFullScreen = true) }
    }

    fun hideFullScreenImage() {
        _state.update { it.copy(isImageFullScreen = false) }
    }

    fun saveItem(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val name = _state.value.name.trim()
        if (name.isEmpty()) {
            onError("Name is required")
            return
        }

        viewModelScope.launch {
            try {
                _state.value.item?.let { currentItem ->
                    val updatedItem = currentItem.copy(
                        name = name,
                        location = _state.value.location.trim().takeIf { it.isNotBlank() },
                        purchaseDate = parsePurchaseDate(_state.value.purchaseDate),
                        purchasePrice = _state.value.purchasePrice.toDoubleOrNull() ?: 0.0,
                        usageDays = _state.value.usageDays.toIntOrNull(),
                        note = _state.value.note.trim().takeIf { it.isNotBlank() },
                        tags = _state.value.tags,
                        imagePath = _state.value.imagePath
                    )
                    updateItemUseCase(updatedItem)
                    _state.update { it.copy(isEditing = false, item = updatedItem) }
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to save item")
            }
        }
    }

    fun confirmDelete(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value.item?.let { item ->
                deleteItemUseCase(item)
                onSuccess()
            }
        }
    }

    private fun formatPurchaseDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
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
