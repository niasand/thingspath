package com.thingspath.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thingspath.data.model.Item
import com.thingspath.ui.component.*

import androidx.compose.material.icons.filled.MoreVert
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Clear

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddItemClick: () -> Unit,
    onAddAIItemClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

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

    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Items") },
                actions = {
                    ViewToggleButtons(
                        isGridView = state.isGridView,
                        onToggleView = { viewModel.toggleView() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Backup") },
                            onClick = {
                                showMenu = false
                                exportLauncher.launch("thingspath_backup.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Backup") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.exportSuccess || state.importSuccess || state.errorMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissMessage() }) {
                            Text("Dismiss")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        when {
                            state.exportSuccess -> "Export successful"
                            state.importSuccess -> "Import successful"
                            else -> state.errorMessage ?: "Unknown error"
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item menu"
                    )
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("AI 模式添加 (AI Mode)") },
                        onClick = {
                            showAddMenu = false
                            onAddAIItemClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("经典模式添加 (Classic Mode)") },
                        onClick = {
                            showAddMenu = false
                            onAddItemClick()
                        }
                    )
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Statistics Header
            StatisticsHeader(
                totalCount = state.totalItemCount,
                totalPrice = state.totalPrice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            // Search Bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            )

            if (state.items.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else if (state.isGridView) {
                GridView(
                    items = state.items,
                    onItemClick = onItemClick,
                    onItemDelete = { viewModel.showDeleteDialog(it) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ListView(
                    items = state.items,
                    onItemClick = onItemClick,
                    onItemDelete = { viewModel.showDeleteDialog(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Delete Confirmation Dialog
        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }
    }
}

@Composable
fun StatisticsHeader(
    totalCount: Int,
    totalPrice: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    text = "Total Items",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Divider(
                modifier = Modifier
                    .height(40.dp)
                    .width(1.dp)
            )
            
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    text = "Total Value",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = String.format("%.2f", totalPrice),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search items...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tap the + button to add your first item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListView(
    items: List<Item>,
    onItemClick: (Long) -> Unit,
    onItemDelete: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            SwipeableItemCard(
                item = item,
                onItemClick = { onItemClick(item.id) },
                onDelete = { onItemDelete(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItemCard(
    item: Item,
    onItemClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                // Return false to prevent the item from disappearing immediately
                // The actual deletion will be handled by the onDelete callback
                // which should trigger a UI update
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = MaterialTheme.colorScheme.errorContainer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = androidx.compose.ui.Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            ItemCard(
                item = item,
                onClick = onItemClick
            )
        }
    )
}

@Composable
fun GridView(
    items: List<Item>,
    onItemClick: (Long) -> Unit,
    @Suppress("UNUSED_PARAMETER") onItemDelete: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            ItemGridItem(
                item = item,
                onClick = { onItemClick(item.id) }
            )
        }
    }
}
