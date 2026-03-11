package com.thingspath.ui.screen.itemdetail

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thingspath.ui.component.DeleteConfirmationDialog
import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import androidx.compose.ui.input.nestedscroll.nestedScroll

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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior() // Use pinned scroll behavior

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToCache(context, it)
            viewModel.onImagePathChange(copiedPath)
        }
    }
    
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Handle camera result
        }
    }

    Scaffold(
        modifier = modifier.imePadding().nestedScroll(scrollBehavior.nestedScrollConnection), // Add imePadding
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (state.item != null) {
                        if (state.isEditing) {
                            IconButton(onClick = {
                                viewModel.saveItem(
                                    onSuccess = onBack,
                                    onError = { /* Show error */ }
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save"
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleEditMode() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit"
                                )
                            }
                            IconButton(onClick = { viewModel.showDeleteDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        }
                    }
                },
                scrollBehavior = scrollBehavior // Set scroll behavior to keep it pinned/visible
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
                    text = "Item not found",
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
                onNoteChange = { viewModel.onNoteChange(it) },
                onTagInputChange = { viewModel.onTagInputChange(it) },
                onAddTag = { viewModel.addTag() },
                onRemoveTag = { viewModel.removeTag(it) },
                onImagePickerClick = { imagePickerLauncher.launch("image/*") },
                onImageDeleteClick = { viewModel.onImagePathChange(null) },
                onImageClick = { viewModel.showFullScreenImage() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            )
        }

        // Full Screen Image Dialog
        if (state.isImageFullScreen) {
            state.imagePath?.let { imagePath ->
                FullScreenImageDialog(
                    imagePath = imagePath,
                    onDismiss = { viewModel.hideFullScreenImage() }
                )
            }
        }

        // Delete Confirmation Dialog
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
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit,
    onImagePickerClick: () -> Unit,
    onImageDeleteClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isEditing) {
                EditableImageView(
                    imagePath = state.imagePath,
                    onImagePickerClick = onImagePickerClick,
                    onImageDeleteClick = onImageDeleteClick
                )
            } else {
                ItemImageDisplay(
                    imagePath = state.imagePath,
                    onClick = onImageClick
                )
            }
        }

        if (state.isEditing) {
            EditModeContent(
                state = state,
                onNameChange = onNameChange,
                onLocationChange = onLocationChange,
                onPurchaseDateChange = onPurchaseDateChange,
                onPurchasePriceChange = onPurchasePriceChange,
                onUsageDaysChange = onUsageDaysChange,
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

@Composable
fun EditableImageView(
    imagePath: String?,
    onImagePickerClick: () -> Unit,
    onImageDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Item image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Action buttons overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imagePath != null) {
                IconButton(
                    onClick = onImageDeleteClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete image",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(
                onClick = onImagePickerClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = if (imagePath != null) Icons.Default.Edit else Icons.Default.AddAPhoto,
                    contentDescription = if (imagePath != null) "Change image" else "Add image"
                )
            }
        }
    }
}

@Composable
fun ItemImageDisplay(
    imagePath: String?,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = imagePath != null, onClick = onClick)
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Item image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No image",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun EditModeContent(
    state: ItemDetailState,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onPurchaseDateChange: (String) -> Unit,
    onPurchasePriceChange: (String) -> Unit,
    onUsageDaysChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTagInputChange: (String) -> Unit,
    onAddTag: () -> Unit,
    onRemoveTag: (String) -> Unit
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

    OutlinedTextField(
        value = state.name,
        onValueChange = onNameChange,
        label = { Text("Name *") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = state.name.trim().isEmpty()
    )

    OutlinedTextField(
        value = state.location,
        onValueChange = onLocationChange,
        label = { Text("Location") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Box(modifier = Modifier.clickable { datePickerDialog.show() }) {
        OutlinedTextField(
            value = state.purchaseDate,
            onValueChange = {}, // Read-only via picker
            label = { Text("Purchase Date (YYYY-MM-DD)") },
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
            placeholder = { Text("e.g., 2024-01-15") },
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
        placeholder = { Text("e.g., 30") }
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
}

@Composable
fun ViewModeContent(item: com.thingspath.data.model.Item?) {
    item ?: return
    DetailField(
        label = "Name",
        value = item.name
    )

    item.location?.let {
        DetailField(label = "Location", value = it)
    }

    item.purchaseDate?.let {
        DetailField(
            label = "Purchase Date",
            value = formatDate(it)
        )
    }

    if (item.purchasePrice > 0) {
        DetailField(
            label = "Price",
            value = String.format("%.2f", item.purchasePrice)
        )
    }

    item.usageDays?.let { days ->
        DetailField(
            label = "Usage Days",
            value = "$days days"
        )
        
        if (item.purchasePrice > 0 && days > 0) {
            val dailyCost = item.purchasePrice / days
            DetailField(
                label = "Daily Cost",
                value = String.format("%.2f / day", dailyCost)
            )
        }
    }

    if (!item.note.isNullOrBlank()) {
        DetailField(
            label = "Note",
            value = item.note
        )
    }

    if (item.tags.isNotEmpty()) {
        Column {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }
        }
    }

    DetailField(
        label = "Added Date",
        value = formatDate(item.createdAt)
    )
}

@Composable
fun FullScreenImageDialog(
    imagePath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        // Use a Box to cover the full screen with a semi-transparent background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    return format.format(date)
}

private fun copyUriToCache(context: Context, uri: Uri): String {
    val cacheDir = File(context.cacheDir, "item_images")
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    val destFile = File(cacheDir, "${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri).use { input ->
        destFile.outputStream().use { output ->
            input?.copyTo(output)
        }
    }
    return destFile.absolutePath
}
