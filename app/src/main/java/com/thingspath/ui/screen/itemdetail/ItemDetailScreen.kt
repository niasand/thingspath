package com.thingspath.ui.screen.itemdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thingspath.ui.component.DeleteConfirmationDialog
import com.thingspath.ui.component.MultiImageEditor
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    viewModel: ItemDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                uris.add(uri)
            }
            if (uris.isNotEmpty()) {
                viewModel.uploadImages(uris)
            }
        }
    }

    val onAddFromGallery = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(Intent.createChooser(intent, "选择图片"))
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri -> viewModel.uploadImage(uri) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            photoUri?.let { cameraLauncher.launch(it) }
        }
    }

    val onCaptureFromCamera = {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
                photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                photoUri?.let { cameraLauncher.launch(it) }
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show upload error snackbar
    LaunchedEffect(state.imageUploadError) {
        state.imageUploadError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissImageUploadError()
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("物品详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (state.item != null) {
                        if (state.isEditing) {
                            IconButton(onClick = {
                                viewModel.saveItem(
                                    onSuccess = onBack,
                                    onError = { }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "保存"
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "编辑"
                                )
                            }
                            IconButton(onClick = { viewModel.showDeleteDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除"
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "未找到物品",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            ItemDetailContent(
                state = state,
                onNameChange = { viewModel.onNameChange(it) },
                onLocationChange = { viewModel.onLocationChange(it) },
                onPurchaseDateChange = { viewModel.onPurchaseDateChange(it) },
                onPurchasePriceChange = { viewModel.onPurchasePriceChange(it) },
                onUsageDaysChange = { viewModel.onUsageDaysChange(it) },
                onReminderDateChange = { viewModel.onReminderDateChange(it) },
                onReminderTypeChange = { viewModel.onReminderTypeChange(it) },
                onReminderNoteChange = { viewModel.onReminderNoteChange(it) },
                onClearReminder = { viewModel.clearReminder() },
                onNoteChange = { viewModel.onNoteChange(it) },
                onTagInputChange = { viewModel.onTagInputChange(it) },
                onAddTag = { viewModel.addTag() },
                onRemoveTag = { viewModel.removeTag(it) },
                onAddFromGallery = { onAddFromGallery() },
                onCaptureFromCamera = { onCaptureFromCamera() },
                onDeleteImage = viewModel::removeImage,
                onImageClick = { index -> viewModel.showFullScreenImage(index) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .imePadding()
            )
        }

        if (state.isImageFullScreen && state.imagePaths.isNotEmpty()) {
            FullScreenImageDialog(
                imagePaths = state.imagePaths,
                initialIndex = state.fullScreenImageIndex,
                onDismiss = { viewModel.hideFullScreenImage() }
            )
        }

        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = {
                    viewModel.confirmDelete {
                        viewModel.dismissDeleteDialog()
                        onBack()
                    }
                },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }
    }
}

@Composable
fun ItemDetailContent(
    state: ItemDetailState,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onUsageDaysChange: (String) -> Unit,
    onReminderDateChange: (String) -> Unit,
    onReminderTypeChange: (String) -> Unit,
    onReminderNoteChange: (String) -> Unit,
    onClearReminder: () -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAddFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onDeleteImage: (Int) -> Unit,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.isEditing) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "照片",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                MultiImageEditor(
                    imagePaths = state.imagePaths,
                    onAddFromGallery = onAddFromGallery,
                    onCaptureFromCamera = onCaptureFromCamera,
                    onDeleteImage = onDeleteImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp)
                )
            }
        } else {
            MultiImageViewer(
                imagePaths = state.imagePaths,
                itemName = state.item?.name,
                onImageClick = onImageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        if (state.isEditing) {
            EditModeContent(
                state = state,
                onNameChange = onNameChange,
                onLocationChange = onLocationChange,
                onPurchaseDateChange = onPurchaseDateChange,
                onPurchasePriceChange = onPurchasePriceChange,
                onUsageDaysChange = onUsageDaysChange,
                onReminderDateChange = onReminderDateChange,
                onReminderTypeChange = onReminderTypeChange,
                onReminderNoteChange = onReminderNoteChange,
                onClearReminder = onClearReminder,
                onNoteChange = onNoteChange,
                onTagInputChange = onTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag
            )
        } else {
            ViewModeContent(item = state.item)
        }
    }
}
