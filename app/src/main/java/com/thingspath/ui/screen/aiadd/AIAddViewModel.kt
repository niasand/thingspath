package com.thingspath.ui.screen.aiadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.model.ExtractedItemInfo
import com.thingspath.data.remote.SiliconFlowClient
import com.thingspath.data.local.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIAddState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val extractedInfo: ExtractedItemInfo? = null
)

@HiltViewModel
class AIAddViewModel @Inject constructor(
    private val siliconFlowClient: SiliconFlowClient,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AIAddState())
    val state: StateFlow<AIAddState> = _state.asStateFlow()

    fun onInputTextChange(value: String) {
        _state.update { it.copy(inputText = value, error = null) }
    }

    fun extractInfo(onSuccess: (ExtractedItemInfo) -> Unit) {
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) {
            _state.update { it.copy(error = "请输入描述文字") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val apiKey = settingsRepository.apiKeyFlow.first()
            val result = siliconFlowClient.extractItemInfo(text, apiKey)
            
            result.onSuccess { info ->
                _state.update { it.copy(isLoading = false, extractedInfo = info) }
                onSuccess(info)
            }.onFailure { error ->
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = error.message ?: "解析失败，请重试"
                    ) 
                }
            }
        }
    }
}
