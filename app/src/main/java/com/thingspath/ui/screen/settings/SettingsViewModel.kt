package com.thingspath.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.local.repository.SettingsRepository
import com.thingspath.data.local.repository.FileRepository
import com.thingspath.domain.usecase.ExportItemsUseCase
import com.thingspath.domain.usecase.ImportItemsUseCase
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

data class SettingsState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportItemsUseCase: ExportItemsUseCase,
    private val importItemsUseCase: ImportItemsUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val apiKey: StateFlow<String> = settingsRepository.apiKeyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(key)
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }
                val jsonString = exportItemsUseCase()
                fileRepository.writeString(uri, jsonString)
                _state.update { it.copy(isExporting = false, exportSuccess = true) }
                delay(2000)
                dismissMessage()
            } catch (e: Exception) {
                _state.update { it.copy(isExporting = false, errorMessage = e.message ?: "Export failed") }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isImporting = true, importSuccess = false, errorMessage = null) }
                val jsonString = fileRepository.readString(uri)
                importItemsUseCase(jsonString)
                _state.update { it.copy(isImporting = false, importSuccess = true) }
                delay(2000)
                dismissMessage()
            } catch (e: Exception) {
                _state.update { it.copy(isImporting = false, errorMessage = e.message ?: "Import failed") }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(exportSuccess = false, importSuccess = false, errorMessage = null) }
    }
}
