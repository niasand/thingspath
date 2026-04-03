package com.thingspath.ui.screen.itemdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.thingspath.ui.component.DeleteConfirmationDialog
import com.thingspath.ui.component.ItemImagePlaceholder
import com.thingspath.ui.component.MultiImageEditor
import android.app.DatePickerDialog
import android.widget.DatePicker
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent

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

    // Gallery picker launcher (multiple selection)
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
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Images"))
    }

    // Camera capture launcher
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri -> viewModel.uploadImage(uri) }
        }
    }

    // Camera permission launcher
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                onAddFromGallery = { onAddFromGallery() },
                onCaptureFromCamera = { onCaptureFromCamera() },
                onDeleteImage = viewModel::removeImage,
                onImageClick = { index -> viewModel.showFullScreenImage(index) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding()
                    .verticalScroll(scrollState)
            )
        }

        // Full Screen Image Dialog
        if (state.isImageFullScreen && state.imagePaths.isNotEmpty()) {
            FullScreenImageDialog(
                imagePaths = state.imagePaths,
                initialIndex = state.fullScreenImageIndex,
                onDismiss = { viewModel.hideFullScreenImage() }
            )
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
        // Image Section
        if (state.isEditing) {
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

/**
 * View-mode image display: single image or swipeable pager for multiple images.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImageViewer(
    imagePaths: List<String>,
    itemName: String?,
    onImageClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (imagePaths.isEmpty()) {
        ItemImagePlaceholder(
            name = itemName,
            modifier = modifier.clip(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            maxLines = 2
        )
        return
    }

    if (imagePaths.size == 1) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { onImageClick(0) }
        ) {
            AsyncImage(
                model = imagePaths[0],
                contentDescription = "Item image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        return
    }

    // Multiple images: pager with dot indicator
    val pagerState = rememberPagerState(pageCount = { imagePaths.size })
    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imagePaths[page],
                contentDescription = "Item image ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onImageClick(page) },
                contentScale = ContentScale.Crop
            )
        }
        // Dot indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(imagePaths.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        // Image counter (top-right)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${imagePaths.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
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
    val usageDays = remember(item.purchaseDate) {
        item.purchaseDate?.let { calculateUsageDaysFromPurchaseDate(it) }
    } ?: item.usageDays
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

    usageDays?.let { days ->
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

private fun calculateUsageDaysFromPurchaseDate(purchaseDateMillis: Long): Int? {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val diff = todayStart - purchaseDateMillis
    val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
    return if (days >= 0) days.toInt() else null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageDialog(
    imagePaths: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (imagePaths.size - 1).coerceAtLeast(0)),
        pageCount = { imagePaths.size }
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = imagePaths[page],
                    contentDescription = "Full screen image ${page + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
            // Counter overlay (top-right)
            if (imagePaths.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${imagePaths.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                // Dot indicator (bottom)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(imagePaths.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
            }
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
