package com.thingspath.ui.screen.itemdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.remote.repository.R2ImageRepository
import com.thingspath.data.model.Item
import com.thingspath.domain.model.AppError
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
import android.util.Log
import java.io.File

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getItemByIdUseCase: GetItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val r2ImageRepository: R2ImageRepository
) : ViewModel() {

    private val itemId: Long = savedStateHandle.get<Long>("itemId") ?: 0L
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    init {
        if (itemId > 0) {
            // 先同步读内存缓存，即时填充 UI，无需 loading
            val cached = getItemByIdUseCase.getCached(itemId)
            if (cached != null) {
                populateState(cached)
                validateAndCleanImagePaths(cached)
            }
            // 后台异步刷新，确保数据新鲜
            loadItem(showLoading = cached == null)
        }
    }

    private fun populateState(item: Item) {
        val purchaseDateStr = formatPurchaseDate(item.purchaseDate)
        val usageDaysStr = computeUsageDaysFromPurchaseDateString(purchaseDateStr)
            ?: item.usageDays?.toString()
            ?: ""
        _state.update {
            it.copy(
                item = item,
                isLoading = false,
                name = item.name,
                location = item.location ?: "",
                purchaseDate = purchaseDateStr,
                purchasePrice = item.purchasePrice.takeIf { p -> p > 0 }?.toString() ?: "",
                usageDays = usageDaysStr,
                reminderDate = formatPurchaseDate(item.reminderDate),
                reminderType = item.reminderType ?: "到期提醒",
                reminderNote = item.reminderNote ?: "",
                note = item.note ?: "",
                tags = item.tags,
                imagePaths = item.imagePaths
            )
        }
    }

    private fun loadItem(showLoading: Boolean) {
        viewModelScope.launch {
            try {
                if (showLoading) _state.update { it.copy(isLoading = true) }
                val item = getItemByIdUseCase(itemId)
                item?.let { populateState(it) }
                if (showLoading) _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load item", AppError.StorageError("Load item failed", e))
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

    fun onReminderDateChange(value: String) {
        _state.update { it.copy(reminderDate = value) }
    }

    fun onReminderTypeChange(value: String) {
        _state.update { it.copy(reminderType = value) }
    }

    fun onReminderNoteChange(value: String) {
        _state.update { it.copy(reminderNote = value) }
    }

    fun clearReminder() {
        _state.update { it.copy(reminderDate = "", reminderType = "到期提醒", reminderNote = "") }
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
        val path = _state.value.imagePaths.getOrNull(index)
        if (path != null && r2ImageRepository.isR2Url(path)) {
            val key = r2ImageRepository.extractKeyFromUrl(path)
            if (key != null) {
                viewModelScope.launch {
                    r2ImageRepository.deleteImage(key)
                }
            }
        }
        _state.update { it.copy(imagePaths = it.imagePaths.filterIndexed { idx, _ -> idx != index }) }
    }

    fun uploadImage(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isImageUploading = true, imageUploadError = null) }
            val path = uploadImageUseCase(uri)
            if (path != null) {
                _state.update { it.copy(imagePaths = it.imagePaths + path, isImageUploading = false) }
            } else {
                _state.update { it.copy(isImageUploading = false, imageUploadError = "图片上传失败，请重试") }
            }
        }
    }

    fun uploadImages(uris: List<Uri>) {
        viewModelScope.launch {
            _state.update { it.copy(isImageUploading = true, imageUploadError = null) }
            var failedCount = 0
            uris.forEach { uri ->
                val path = uploadImageUseCase(uri)
                if (path != null) {
                    _state.update { it.copy(imagePaths = it.imagePaths + path) }
                } else {
                    failedCount++
                }
            }
            _state.update {
                it.copy(
                    isImageUploading = false,
                    imageUploadError = if (failedCount > 0) "${failedCount} 张图片上传失败" else null
                )
            }
        }
    }

    fun dismissImageUploadError() {
        _state.update { it.copy(imageUploadError = null) }
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
                        reminderDate = parsePurchaseDate(_state.value.reminderDate),
                        reminderType = _state.value.reminderType.takeIf { _state.value.reminderDate.isNotBlank() },
                        reminderNote = _state.value.reminderNote.trim().takeIf { text ->
                            _state.value.reminderDate.isNotBlank() && text.isNotBlank()
                        },
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

    /**
     * 校验 imagePaths 中的本地路径是否存在，移除已丢失的文件引用。
     * R2 URL（https://）不做校验，只检查本地文件路径。
     */
    private fun validateAndCleanImagePaths(item: Item) {
        val cleaned = item.imagePaths.filter { path ->
            path.startsWith("http://") || path.startsWith("https://") || File(path).exists()
        }
        if (cleaned.size != item.imagePaths.size) {
            _state.update { it.copy(imagePaths = cleaned) }
        }
    }

    companion object {
        private const val TAG = "ItemDetailViewModel"
    }
}
