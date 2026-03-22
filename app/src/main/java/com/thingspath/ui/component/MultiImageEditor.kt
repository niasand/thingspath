package com.thingspath.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val THUMB_SIZE = 100.dp

/**
 * Horizontal scrollable list of image thumbnails with add/delete controls.
 * Supports both gallery picker and camera capture.
 * Used in both AddItemScreen (add mode) and ItemDetailScreen (edit mode).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiImageEditor(
    imagePaths: List<String>,
    onAddFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onDeleteImage: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        itemsIndexed(imagePaths) { index, path ->
            ImageThumbnailCell(
                imagePath = path,
                onDelete = { onDeleteImage(index) }
            )
        }
        item {
            AddImageCell(onClick = { showBottomSheet = true })
        }
    }

    // Bottom sheet for selecting image source
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "添加图片",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Gallery option
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        onAddFromGallery()
                    }
                )

                // Camera option
                ListItem(
                    headlineContent = { Text("拍照") },
                    supportingContent = { Text("照片将保存到 ThingsPath 相册") },
                    leadingContent = {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable {
                        showBottomSheet = false
                        onCaptureFromCamera()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ImageThumbnailCell(
    imagePath: String,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(THUMB_SIZE)
            .clip(MaterialTheme.shapes.medium)
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Item image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Delete button overlay (top-right corner)
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.errorContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete image",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun AddImageCell(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(THUMB_SIZE)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
