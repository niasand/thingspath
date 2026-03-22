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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private val THUMB_SIZE = 100.dp

/**
 * Horizontal scrollable list of image thumbnails with add/delete controls.
 * Used in both AddItemScreen (add mode) and ItemDetailScreen (edit mode).
 */
@Composable
fun MultiImageEditor(
    imagePaths: List<String>,
    onAddImage: () -> Unit,
    onDeleteImage: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
            AddImageCell(onClick = onAddImage)
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
