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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thingspath.data.model.Item
import com.thingspath.ui.component.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Analytics
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
import com.thingspath.ui.theme.customColors
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onItemClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var showAIDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val colors = MaterialTheme.customColors

    // Scroll to top when signal changes
    LaunchedEffect(state.scrollToTopSignal) {
        if (state.scrollToTopSignal > 0) {
            kotlinx.coroutines.delay(100)
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(state.infoMessage) {
        if (state.infoMessage != null) {
            showAIDialog = false
        }
    }

    Scaffold(
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = { Text("已选择 ${state.selectedItemIds.size} 项") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭选择模式")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.Check, contentDescription = "全选")
                        }
                        IconButton(onClick = { viewModel.showDeleteDialog(Item(id = 0, name = "", purchasePrice = 0.0)) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除选中")
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
                        Icon(Icons.Default.FilterList, contentDescription = "排序")
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
                        DropdownMenuItem(
                            text = { Text("添加时间") },
                            trailingIcon = {
                                if (state.sortField == HomeSortField.CreatedAt) {
                                    Icon(
                                        if (state.sortAscending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSort(HomeSortField.CreatedAt)
                                showSortMenu = false
                            }
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
            }
        },
        bottomBar = {
            if (state.exportSuccess || state.importSuccess || state.errorMessage != null || state.infoMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.dismissMessage() }) {
                            Text("关闭")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        when {
                            state.infoMessage != null -> state.infoMessage!!
                            state.exportSuccess -> "导出成功"
                            state.importSuccess -> "导入成功"
                            else -> state.errorMessage ?: "未知错误"
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { showAIDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "AI 智能添加"
                )
            }
        },
        modifier = modifier,
        containerColor = colors.homeBackground
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
                    .clickable { onNavigateToStatistics() }
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
                val swipeRefreshState = rememberSwipeRefreshState(state.isRefreshing)
                LaunchedEffect(state.isRefreshing) {
                    swipeRefreshState.isRefreshing = state.isRefreshing
                }
                SwipeRefresh(
                    state = swipeRefreshState,
                    onRefresh = { viewModel.refreshData() },
                    modifier = Modifier.weight(1f)
                ) {
                    ListView(
                        items = state.items,
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
fun AIInputDialog(
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = { Text("AI 智能添加") },
        text = {
            Column {
                Text("粘贴或输入物品信息，AI 将自动提取名称、价格、日期等。")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("例如：昨天在苹果店花 14999 买了台 MacBook Pro") },
                    enabled = !isProcessing
                )
                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("AI 分析中...", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(text) },
                enabled = text.isNotBlank() && !isProcessing
            ) {
                Text("分析")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("取消")
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(
                    text = "物品总数",
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
                    text = "总价值",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = String.format("%.2f", totalPrice),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            VerticalDivider(
                modifier = Modifier.height(40.dp)
            )

            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "查看统计",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
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
        placeholder = { Text("搜索物品...") },
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
                        contentDescription = "清除搜索"
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
                text = "还没有物品",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "点击 + 按钮添加第一个物品",
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
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = androidx.compose.ui.Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
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
