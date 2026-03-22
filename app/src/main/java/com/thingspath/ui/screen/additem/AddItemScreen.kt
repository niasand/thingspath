package com.thingspath.ui.screen.additem

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thingspath.ui.component.MultiImageEditor
import android.app.DatePickerDialog
import android.widget.DatePicker
import java.util.Calendar
import com.thingspath.util.ItemImageStorage
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Gallery picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val storedPath = ItemImageStorage.saveToAppStorage(context, it)
            if (storedPath != null) viewModel.addImage(storedPath)
        }
    }

    // Camera capture launcher
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                // Save captured photo to album and get the album path
                val albumPath = ItemImageStorage.saveToAlbum(context, uri)
                if (albumPath != null) {
                    viewModel.addImage(albumPath)
                }
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create temp file for camera capture
            val tempFile = File.createTempFile("camera_", ".jpg", context.cacheDir)
            photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            photoUri?.let { cameraLauncher.launch(it) }
        } else {
            // Show permission denied message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Item") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveItem(
                                onSuccess = {
                                    viewModel.clearForm()
                                    onBack()
                                },
                                onError = { /* Show error */ }
                            )
                        },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save"
                        )
                    }
                }
            )
        },
        modifier = modifier.imePadding() // Add imePadding to Scaffold
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
        } else {
            AddItemForm(
                state = state,
                onNameChange = viewModel::onNameChange,
                onLocationChange = viewModel::onLocationChange,
                onPurchaseDateChange = viewModel::onPurchaseDateChange,
                onPurchasePriceChange = viewModel::onPurchasePriceChange,
                onUsageDaysChange = viewModel::onUsageDaysChange,
                onNoteChange = viewModel::onNoteChange,
                onTagInputChange = viewModel::onTagInputChange,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onAddFromGallery = { imagePickerLauncher.launch("image/*") },
                onCaptureFromCamera = {
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
                },
                onDeleteImage = viewModel::removeImage,
                onSave = {
                    viewModel.saveItem(
                        onSuccess = {
                            viewModel.clearForm()
                            onBack()
                        },
                        onError = { /* Show error */ }
                    )
                },
                onCancel = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            )
        }
    }
}

@Composable
fun AddItemForm(
    state: AddItemState,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onUsageDaysChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onAddFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onDeleteImage: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            // Month is 0-indexed
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onPurchaseDateChange(selectedDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Photos",
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

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } }
        )

        OutlinedTextField(
            value = state.location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("e.g., Kitchen drawer") }
        )

        Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
            OutlinedTextField(
                value = state.purchaseDate,
                onValueChange = {}, // Read-only
                label = { Text("Purchase Date") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                singleLine = true,
                placeholder = { Text("YYYY-MM-DD") },
                trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = "Select Date")
            }
        )
    }

    OutlinedTextField(
        value = state.purchasePrice,
        onValueChange = onPurchasePriceChange,
        label = { Text("Price") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
        placeholder = { Text("0.00") }
    )

    OutlinedTextField(
        value = state.usageDays,
            onValueChange = onUsageDaysChange,
            label = { Text("Usage Days") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Number of days used") }
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            placeholder = { Text("Add any notes here...") }
        )

        // Tags Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.tagInput,
                        onValueChange = onTagInputChange,
                        label = { Text("Add Tag") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onAddTag() })
                    )
                    IconButton(onClick = onAddTag) {
                        Icon(Icons.Default.Add, contentDescription = "Add Tag")
                    }
                }

            if (state.tags.isNotEmpty()) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove Tag",
                                    modifier = Modifier.size(16.dp).clickable { onRemoveTag(tag) }
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = onSave,
                enabled = state.name.trim().isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

