package com.thingspath.ui.screen.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.domain.usecase.ExportItemsUseCase
import com.thingspath.domain.usecase.ImportItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isRestoring: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportItemsUseCase: ExportItemsUseCase,
    private val importItemsUseCase: ImportItemsUseCase,
    private val itemRepository: ItemRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val apiKey = settingsRepository.apiKey.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    suspend fun saveApiKey(key: String) {
        settingsRepository.saveApiKey(key)
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true, infoMessage = null, errorMessage = null)
                val jsonString = exportItemsUseCase()
                context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) }
                    ?: throw Exception("无法打开输出流")
                _uiState.value = _uiState.value.copy(isExporting = false, infoMessage = "导出成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isExporting = false, errorMessage = e.message ?: "导出失败")
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isImporting = true, infoMessage = null, errorMessage = null)
                val jsonString = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception("无法读取输入流")
                importItemsUseCase(jsonString)
                _uiState.value = _uiState.value.copy(isImporting = false, infoMessage = "导入成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isImporting = false, errorMessage = e.message ?: "导入失败")
            }
        }
    }

    fun restoreFromRemote() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isRestoring = true, infoMessage = null, errorMessage = null)
                itemRepository.pullFromD1()
                _uiState.value = _uiState.value.copy(isRestoring = false, infoMessage = "恢复成功")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRestoring = false, errorMessage = e.message ?: "恢复失败")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var inputKey by remember(apiKey) { mutableStateOf(apiKey ?: "") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage) {
        val msg = uiState.infoMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarIsError = uiState.errorMessage != null
            launch { delay(300); snackbarHostState.currentSnackbarData?.dismiss() }
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Indefinite
            )
            viewModel.dismissMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importData(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                val containerColor = if (!snackbarIsError) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
                val contentColor = if (!snackbarIsError) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
                Snackbar(
                    snackbarData = data,
                    containerColor = containerColor,
                    contentColor = contentColor,
                    shape = SnackbarDefaults.shape
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "AI 配置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                label = { Text("SiliconFlow API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "API Key 仅存储在本设备上",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveApiKey(inputKey)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "数据恢复",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "从云端 D1 数据库恢复数据到本地",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.restoreFromRemote() },
                enabled = !uiState.isRestoring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isRestoring) "恢复中..." else "从云端恢复")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "数据导入/导出",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("thingspath_backup.json") },
                    enabled = !uiState.isExporting && !uiState.isImporting && !uiState.isRestoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isExporting) "导出中..." else "导出")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    enabled = !uiState.isExporting && !uiState.isImporting && !uiState.isRestoring,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (uiState.isImporting) "导入中..." else "导入")
                }
            }
        }
    }
}
