package com.thingspath.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ViewToggleButtons(
    isGridView: Boolean,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilterChip(
            selected = !isGridView,
            onClick = { if (isGridView) onToggleView() },
            label = { Text("List") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.ViewList,
                    contentDescription = "List View"
                )
            }
        )
        FilterChip(
            selected = isGridView,
            onClick = { if (!isGridView) onToggleView() },
            label = { Text("Grid") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Grid View"
                )
            }
        )
    }
}
