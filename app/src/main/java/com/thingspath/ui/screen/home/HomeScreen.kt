package com.thingspath.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thingspath.data.model.Item
import com.thingspath.ui.component.DeleteConfirmationDialog
import com.thingspath.ui.component.Logo
import com.thingspath.ui.theme.customColors
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

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
                        Box(
                            modifier = Modifier.clickable {
                                android.util.Log.d("ThingsPath", "TP Logo clicked! isSelectionMode=${state.isSelectionMode}, items=${state.items.size}")
                            }
                        ) {
                            Logo()
                        }
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
            StatisticsHeader(
                totalCount = state.totalItemCount,
                totalPrice = state.totalPrice,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { onNavigateToStatistics() }
            )

            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            AnimatedVisibility(visible = state.allTags.isNotEmpty()) {
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
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
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

        if (state.showDeleteDialog) {
            DeleteConfirmationDialog(
                onConfirm = { viewModel.confirmDelete() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }

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
        shape = MaterialTheme.shapes.medium
    )
}
