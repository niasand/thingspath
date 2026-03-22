package com.thingspath.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.thingspath.data.model.Item
import com.thingspath.ui.component.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.filled.Clear

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddItemClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showFabMenu by remember { mutableStateOf(false) }
    var showAIDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val pagedItems = remember(state.items, state.pageSize, state.currentPage) {
        val start = (state.currentPage * state.pageSize).coerceAtLeast(0)
        if (start >= state.items.size) emptyList()
        else state.items.drop(start).take(state.pageSize)
    }

    val listState = rememberLazyListState()

    // 边缘滑动翻页
    val density = LocalDensity.current
    // 20dp：一次滑动到底部后的 overflow 拖拽即可触发，无需第二次滑动
    val overScrollThresholdPx = with(density) { 20.dp.toPx() }
    var overScrollAccumulator by remember { mutableFloatStateOf(0f) }
    val canGoNext = rememberUpdatedState(state.currentPage < state.pageCount - 1)
    val canGoPrev = rememberUpdatedState(state.currentPage > 0)
    val goToNextPage = rememberUpdatedState(viewModel::goToNextPage)
    val goToPrevPage = rememberUpdatedState(viewModel::goToPreviousPage)
    val overScrollNestedScroll = remember {
        object : NestedScrollConnection {
            // 拖拽：到达边界后继续拖动触发翻页
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero

                // 底部：手指上滑（available.y < 0），列表已到底 → 翻下一页
                if (available.y < 0f && !listState.canScrollForward) {
                    overScrollAccumulator += -available.y
                    if (overScrollAccumulator >= overScrollThresholdPx && canGoNext.value) {
                        overScrollAccumulator = 0f
                        goToNextPage.value()
                    }
                    return available // 消费，阻止 overscroll 动画抢走 delta
                }

                // 顶部：手指下滑（available.y > 0），列表已到顶 → 翻上一页
                if (available.y > 0f && !listState.canScrollBackward) {
                    overScrollAccumulator += available.y
                    if (overScrollAccumulator >= overScrollThresholdPx && canGoPrev.value) {
                        overScrollAccumulator = 0f
                        goToPrevPage.value()
                    }
                    return available // 消费
                }

                overScrollAccumulator = 0f
                return Offset.Zero
            }

            // 快速甩动：fling 打到边界后剩余 velocity 直接触发翻页
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y < 0f && !listState.canScrollForward && canGoNext.value) {
                    goToNextPage.value()
                    return available
                }
                if (available.y > 0f && !listState.canScrollBackward && canGoPrev.value) {
                    goToPrevPage.value()
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    // Scroll to top when signal changes
    LaunchedEffect(state.scrollToTopSignal) {
        if (state.scrollToTopSignal > 0) {
            // Add a small delay to ensure the list has been updated before scrolling
            kotlinx.coroutines.delay(100)
            listState.scrollToItem(0)
        }
    }

    // Scroll to top when page changes
    LaunchedEffect(state.currentPage) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            showAIDialog = false
        }
    }

    if (state.showStatistics) {
        StatisticsDialog(
            items = state.items, // Show stats for current filtered list
            onDismiss = { viewModel.toggleStatistics() }
        )
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("${state.selectedItemIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection Mode")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.Check, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog(Item(id = 0, name = "", purchasePrice = 0.0)) /* Dummy item to trigger dialog, logic handled in VM */ }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Logo()
                    },
                    actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("购买日期") },
                            trailingIcon = {
                                if (state.sortField == HomeSortField.PurchaseDate) {
                                    Icon(
                                        if (state.sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSort(HomeSortField.PurchaseDate)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("物品名称") },
                            trailingIcon = {
                                if (state.sortField == HomeSortField.Name) {
                                    Icon(
                                        if (state.sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSort(HomeSortField.Name)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("使用天数") },
                            trailingIcon = {
                                if (state.sortField == HomeSortField.UsageDays) {
                                    Icon(
                                        if (state.sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSort(HomeSortField.UsageDays)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("更新时间") },
                            trailingIcon = {
                                if (state.sortField == HomeSortField.UpdatedAt) {
                                    Icon(
                                        if (state.sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSort(HomeSortField.UpdatedAt)
                                showSortMenu = false
                            }
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
            }
        },
        bottomBar = {
            Column {
                if (state.exportSuccess || state.importSuccess || state.errorMessage != null || state.infoMessage != null) {
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
                                state.infoMessage != null -> state.infoMessage!!
                                state.exportSuccess -> "Export successful"
                                state.importSuccess -> "Import successful"
                                else -> state.errorMessage ?: "Unknown error"
                            }
                        )
                    }
                }

                if (state.items.isNotEmpty() && state.pageCount > 0) {
                    PaginationBar(
                        totalCount = state.totalItemCount,
                        currentPage = state.currentPage,
                        pageCount = state.pageCount,
                        pageSize = state.pageSize,
                        onPrevious = { viewModel.goToPreviousPage() },
                        onNext = { viewModel.goToNextPage() },
                        onPageSelected = { viewModel.goToPage(it) },
                        onPageSizeSelected = { viewModel.setPageSize(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            Box {
                SmallFloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item"
                    )
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Manual Add") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            showFabMenu = false
                            onAddItemClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("AI Smart Add") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                        onClick = {
                            showFabMenu = false
                            showAIDialog = true
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
                    .clickable { viewModel.toggleStatistics() }
            )

            // Search Bar
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            )

            // Tag Quick Filter
            if (state.allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    items(state.allTags) { tag ->
                        FilterChip(
                            selected = tag in state.selectedTags,
                            onClick = { viewModel.toggleTag(tag) },
                            label = { Text(tag) },
                            leadingIcon = if (tag in state.selectedTags) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.weight(1f).nestedScroll(overScrollNestedScroll)) {
                    ListView(
                        items = pagedItems,
                        isSelectionMode = state.isSelectionMode,
                        selectedIds = state.selectedItemIds,
                        onItemClick = { id ->
                            if (state.isSelectionMode) {
                                viewModel.toggleItemSelection(id)
                            } else {
                                onItemClick(id)
                            }
                        },
                        onItemLongClick = { id ->
                            if (!state.isSelectionMode) {
                                viewModel.toggleSelectionMode()
                                viewModel.toggleItemSelection(id)
                            }
                        },
                        onItemDelete = { viewModel.showDeleteDialog(it) },
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    )
                }
            }
        }

        // Delete Confirmation Dialog
        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }

        // AI Input Dialog
        if (showAIDialog) {
            AIInputDialog(
                isProcessing = state.isAIProcessing,
                onDismiss = { showAIDialog = false },
                onSubmit = { text -> viewModel.analyzeText(text) }
            )
        }
    }
}


@Composable
fun PaginationBar(
    totalCount: Int,
    currentPage: Int,
    pageCount: Int,
    pageSize: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPageSelected: (Int) -> Unit,
    onPageSizeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 0) return

    var expanded by remember { mutableStateOf(false) }
    val pageItems = remember(currentPage, pageCount) {
        buildPageItems(currentPage = currentPage, pageCount = pageCount)
    }

    Surface(
        modifier = modifier,
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "共 $totalCount 条",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = onPrevious,
                    enabled = currentPage > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null
                    )
                }

                pageItems.forEach { pageItem ->
                    if (pageItem == null) {
                        Text(
                            text = "…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    } else {
                        val selected = pageItem == currentPage
                        OutlinedButton(
                            onClick = { onPageSelected(pageItem) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "${pageItem + 1}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                OutlinedIconButton(
                    onClick = onNext,
                    enabled = currentPage < pageCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "${pageSize}/页",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(10, 20, 50).forEach { size ->
                        DropdownMenuItem(
                            text = { Text("$size/页") },
                            onClick = {
                                onPageSizeSelected(size)
                                expanded = false
                            },
                            leadingIcon = if (pageSize == size) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

private fun buildPageItems(currentPage: Int, pageCount: Int): List<Int?> {
    if (pageCount <= 0) return emptyList()
    val maxVisible = 5
    val windowStart = (currentPage - 2).coerceIn(0, (pageCount - maxVisible).coerceAtLeast(0))
    val windowEndExclusive = (windowStart + maxVisible).coerceAtMost(pageCount)

    val result = mutableListOf<Int?>()
    if (windowStart > 0) result.add(null)
    for (i in windowStart until windowEndExclusive) result.add(i)
    if (windowEndExclusive < pageCount) result.add(null)
    return result
}

@Composable
fun AIInputDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("AI Smart Add") },
        text = {
            Column {
                Text("Paste or type item details here. The AI will extract name, price, date, and more.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("e.g., Bought a new MacBook Pro for $2000 yesterday at Apple Store.") },
                    enabled = !isProcessing
                )
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("AI is thinking...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank() && !isProcessing
            ) {
                Text("Analyze")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel")
            }
        }
    )
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
            
            VerticalDivider(
                modifier = Modifier.height(40.dp)
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
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onItemClick: (Long) -> Unit,
    onItemLongClick: (Long) -> Unit,
    onItemDelete: (Item) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState()
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = items,
            key = { it.id }
        ) { item ->
            SwipeableItemCard(
                item = item,
                isSelectionMode = isSelectionMode,
                isSelected = item.id in selectedIds,
                onItemClick = { onItemClick(item.id) },
                onItemLongClick = { onItemLongClick(item.id) },
                onDelete = { onItemDelete(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItemCard(
    item: Item,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    if (isSelectionMode) {
        ItemCard(
            item = item,
            isSelectionMode = true,
            isSelected = isSelected,
            onClick = onItemClick,
            onLongClick = onItemLongClick
        )
    } else {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
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
                    isSelectionMode = false,
                    isSelected = false,
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
            }
        )
    }
}


