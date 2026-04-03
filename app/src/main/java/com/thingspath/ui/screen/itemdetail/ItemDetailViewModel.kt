package com.thingspath.ui.screen.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

import android.net.Uri

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItemByIdUseCase: GetItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val uploadImageUseCase: UploadImageUseCase
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
                    val purchaseDateStr = formatPurchaseDate(loadedItem.purchaseDate)
                    val usageDaysStr = computeUsageDaysFromPurchaseDateString(purchaseDateStr)
                        ?: loadedItem.usageDays?.toString()
                        ?: ""
                    _state.update {
                        it.copy(
                            item = loadedItem,
                            isLoading = false,
                            name = loadedItem.name,
                            location = loadedItem.location ?: "",
                            purchaseDate = purchaseDateStr,
                            purchasePrice = loadedItem.purchasePrice.takeIf { it > 0 }?.toString() ?: "",
                            usageDays = usageDaysStr,
                            note = loadedItem.note ?: "",
                            tags = loadedItem.tags,
                            imagePaths = loadedItem.imagePaths
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
        if (value.isBlank()) {
            _state.update { it.copy(purchaseDate = "", usageDays = "") }
            return
        }

        val usageDaysStr = computeUsageDaysFromPurchaseDateString(value) ?: ""
        _state.update { it.copy(purchaseDate = value, usageDays = usageDaysStr) }
    }

    fun onPurchasePriceChange(value: String) {
        // Allow digits and one decimal point
        if (value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
            _state.update { it.copy(purchasePrice = value) }
        }
    }

    fun onUsageDaysChange(value: String) {
        val digits = value.filter { it.isDigit() }
        if (digits.isBlank()) {
            _state.update { it.copy(usageDays = "", purchaseDate = "") }
            return
        }

        val days = digits.toIntOrNull() ?: 0
        val purchaseDateStr = computePurchaseDateStringFromUsageDays(days)
        _state.update { it.copy(usageDays = days.toString(), purchaseDate = purchaseDateStr) }
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

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isImageUploading = true) }
            val path = uploadImageUseCase(uri)
            if (path != null) {
                _state.update { it.copy(imagePaths = it.imagePaths + path) }
            }
            _state.update { it.copy(isImageUploading = false) }
        }
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

    fun showFullScreenImage(index: Int = 0) {
        _state.update { it.copy(isImageFullScreen = true, fullScreenImageIndex = index) }
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
                        imagePaths = _state.value.imagePaths,
                        imagePath = _state.value.imagePaths.firstOrNull()
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
        return dateFormat.format(java.util.Date(timestamp))
    }

    private fun parsePurchaseDate(dateString: String): Long? {
        if (dateString.isBlank()) return null
        return try {
            dateFormat.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun computeUsageDaysFromPurchaseDateString(purchaseDateStr: String): String? {
        if (purchaseDateStr.isBlank()) return null
        return try {
            val purchaseDate = dateFormat.parse(purchaseDateStr) ?: return null
            val todayStart = todayStartMillis()
            val diff = todayStart - purchaseDate.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days >= 0) days.toString() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun computePurchaseDateStringFromUsageDays(days: Int): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = todayStartMillis()
        cal.add(Calendar.DAY_OF_YEAR, -days.coerceAtLeast(0))
        return dateFormat.format(cal.time)
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
