package com.thingspath.ui.screen.additem

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import android.app.DatePickerDialog
import android.widget.DatePicker
import java.io.File
import java.util.Calendar
import com.thingspath.ui.component.ItemAvatarPlaceholder

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedPath = copyUriToCache(context, it)
            viewModel.onImagePathChange(copiedPath)
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
        } else {
            AddItemForm(
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
                onAiExtract = { viewModel.extractInfo() },
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
                    .consumeWindowInsets(paddingValues)
                    .padding(paddingValues)
                    .imePadding()
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
    onImagePickerClick: () -> Unit,
    onImageDeleteClick: () -> Unit,
    onAiExtract: () -> Unit,
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            EditableImageView(
                itemName = state.name,
                imagePath = state.imagePath,
                onImagePickerClick = onImagePickerClick,
                onImageDeleteClick = onImageDeleteClick
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
            label = { Text("Note (AI 解析源)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            placeholder = { Text("例如：昨天在大地影院买了个50元的充电宝") },
            trailingIcon = {
                IconButton(onClick = onAiExtract) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Extract",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
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

@Composable
fun EditableImageView(
    itemName: String,
    imagePath: String?,
    onImagePickerClick: () -> Unit,
    onImageDeleteClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onImagePickerClick)
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Item image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            ItemAvatarPlaceholder(
                name = itemName,
                modifier = Modifier.fillMaxSize()
            )
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
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
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = if (imagePath != null) Icons.Default.Edit else Icons.Default.AddAPhoto,
                    contentDescription = if (imagePath != null) "Change image" else "Add image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
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
