package com.thingspath.ui.screen.additem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.model.Item
import com.thingspath.data.model.ExtractedItemInfo
import com.thingspath.data.remote.SiliconFlowClient
import com.thingspath.data.local.repository.SettingsRepository
import com.thingspath.domain.usecase.AddItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase,
    private val siliconFlowClient: SiliconFlowClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddItemState())
    val state: StateFlow<AddItemState> = _state.asStateFlow()

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
                    imagePath = _state.value.imagePath
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
        _state.value = AddItemState()
    }

    fun prefillFromAi(
        name: String, 
        date: String?, 
        location: String?, 
        price: Double?,
        autoSave: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        _state.update { currentState ->
            var newState = currentState.copy(
                name = name,
                nameError = null
            )
            if (!date.isNullOrBlank()) {
                newState = newState.copy(purchaseDate = date)
                // Re-calculate usage days
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                try {
                    val purchaseDate = sdf.parse(date)
                    if (purchaseDate != null) {
                        val diff = System.currentTimeMillis() - purchaseDate.time
                        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                        if (days >= 0) {
                            newState = newState.copy(usageDays = days.toString())
                        }
                    }
                } catch (e: Exception) {}
            }
            if (!location.isNullOrBlank()) {
                newState = newState.copy(location = location)
            }
            if (price != null && price > 0) {
                newState = newState.copy(purchasePrice = price.toString())
            }
            newState
        }

        if (autoSave && validateForm()) {
            saveItem(
                onSuccess = onSuccess,
                onError = { /* Error handled via state? or just silent for auto-save */ }
            )
        }
    }

    fun extractInfo() {
        val text = _state.value.note.trim()
        if (text.isEmpty()) {
            _state.update { it.copy(nameError = "请在备注中输入描述文字以便 AI 解析") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val apiKey = settingsRepository.apiKeyFlow.first()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val currentDate = sdf.format(java.util.Date())
            
            val result = siliconFlowClient.extractItemInfo(text, apiKey, currentDate)
            
            result.onSuccess { info ->
                _state.update { it.copy(isLoading = false) }
                prefillFromAi(
                    info.name ?: "",
                    info.purchaseDate,
                    info.location,
                    info.purchasePrice
                )
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        nameError = error.message ?: "解析失败，请检查 API Key"
                    ) 
                }
            }
        }
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
