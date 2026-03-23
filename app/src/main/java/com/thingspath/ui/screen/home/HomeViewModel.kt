package com.thingspath.ui.screen.home

import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.data.model.Item
import com.thingspath.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.thingspath.data.local.repository.FileRepository
import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.local.datastore.SettingsRepository
import android.net.Uri

import android.util.Log

import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

import com.thingspath.data.remote.repository.SiliconFlowRepository
import com.thingspath.ui.screen.home.HomeSortField.*

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val exportItemsUseCase: ExportItemsUseCase,
    private val importItemsUseCase: ImportItemsUseCase,
    private val fileRepository: FileRepository,
    private val itemRepository: ItemRepository,
    private val siliconFlowRepository: SiliconFlowRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            itemRepository.restoreDataIfNeeded()
        }
        observeSettings()
        loadItems()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.pageSize
                .distinctUntilChanged()
                .collect { size ->
                    val normalized = when (size) {
                        10, 20, 50 -> size
                        else -> 10
                    }
                    _state.update { current ->
                        val pageCount = calculatePageCount(current.items.size, normalized)
                        current.copy(
                            pageSize = normalized,
                            currentPage = 0,
                            pageCount = pageCount
                        )
                    }
                }
        }
        viewModelScope.launch {
            settingsRepository.infiniteScroll
                .distinctUntilChanged()
                .collect { enabled ->
                    _state.update { current ->
                        current.copy(infiniteScroll = enabled)
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun loadItems() {
        var lastSortField: HomeSortField? = null
        var lastSortAscending: Boolean? = null
        var lastQuery: String? = null
        var lastSelectedTags: Set<String>? = null
        var lastTotalCount: Int = 0

        viewModelScope.launch {
            combine(
                getItemsUseCase(), // Fetch all items (no query)
                _state.map { it.searchQuery }.distinctUntilChanged().debounce(300),
                _state.map { it.selectedTags }.distinctUntilChanged(),
                _state.map { it.sortField }.distinctUntilChanged(),
                _state.map { it.sortAscending }.distinctUntilChanged()
            ) { allItems, query, selectedTags, sortField, ascending ->
                // 1. Extract all tags from all items
                val allTags = allItems.flatMap { it.tags }.filter { it.isNotBlank() }.distinct().sorted()

                // 2. Filter by Search Query
                var filtered = if (query.isBlank()) {
                    allItems
                } else {
                    allItems.filter { item ->
                        item.name.contains(query, ignoreCase = true) ||
                        item.note?.contains(query, ignoreCase = true) == true ||
                        item.location?.contains(query, ignoreCase = true) == true
                    }
                }

                // 3. Filter by Selected Tags
                if (selectedTags.isNotEmpty()) {
                    filtered = filtered.filter { item ->
                        item.tags.any { it in selectedTags }
                    }
                }

                // 4. Apply Sorting
                val sorted = applySorting(filtered, sortField, ascending)
                
                // Determine if we should scroll to top
                // Check if any of the controlling parameters have changed since last emission
                val shouldScrollToTop = (lastSortField != null && lastSortField != sortField) ||
                                      (lastSortAscending != null && lastSortAscending != ascending) ||
                                      (lastQuery != null && lastQuery != query) ||
                                      (lastSelectedTags != null && lastSelectedTags != selectedTags) ||
                                      (sorted.size > lastTotalCount) // Item added

                // Update last state
                lastSortField = sortField
                lastSortAscending = ascending
                lastQuery = query
                lastSelectedTags = selectedTags
                lastTotalCount = sorted.size

                DataResult(allTags, sorted, allItems, shouldScrollToTop)
            }
            .collect { (allTags, displayedItems, _, shouldScrollToTop) ->
                 val totalCount = displayedItems.size // Count of filtered items
                 val totalPrice = displayedItems.sumOf { it.purchasePrice }
                 val allTagsSet = allTags.toHashSet()

                 _state.update {
                     val pageCount = calculatePageCount(displayedItems.size, it.pageSize)
                     val clampedPage = clampPage(it.currentPage, pageCount)
                     // 过滤掉已不存在的 tag，防止选中 tag 被删除后列表永久为空
                     val validSelectedTags = it.selectedTags.intersect(allTagsSet)

                     it.copy(
                         items = displayedItems,
                         allTags = allTags,
                         selectedTags = validSelectedTags,
                         isLoading = false,
                         totalItemCount = totalCount,
                         totalPrice = totalPrice,
                         currentPage = clampedPage,
                         pageCount = pageCount,
                         scrollToTopSignal = if (shouldScrollToTop) it.scrollToTopSignal + 1 else it.scrollToTopSignal
                     )
                 }
            }
        }
    }

    private data class DataResult(
        val allTags: List<String>,
        val displayedItems: List<Item>,
        val allItemsSource: List<Item>,
        val shouldScrollToTop: Boolean
    )

    fun toggleTag(tag: String) {
        _state.update { 
            val newTags = if (tag in it.selectedTags) it.selectedTags - tag else it.selectedTags + tag
            it.copy(selectedTags = newTags, currentPage = 0)
        }
    }

    fun toggleSelectionMode() {
        _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedItemIds = emptySet()) }
    }

    fun toggleItemSelection(id: Long) {
        _state.update { 
            val newSelection = if (id in it.selectedItemIds) it.selectedItemIds - id else it.selectedItemIds + id
            it.copy(selectedItemIds = newSelection)
        }
    }

    fun selectAll() {
        _state.update { it.copy(selectedItemIds = it.items.map { item -> item.id }.toSet()) }
    }

    fun deleteSelectedItems() {
        val idsToDelete = state.value.selectedItemIds
        if (idsToDelete.isEmpty()) return

        viewModelScope.launch {
            try {
                deleteItemUseCase(idsToDelete)
                _state.update { it.copy(isSelectionMode = false, selectedItemIds = emptySet()) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = "批量删除失败：${e.message ?: "未知错误"}") }
            }
        }
    }

    fun toggleStatistics() {
        _state.update { it.copy(showStatistics = !it.showStatistics) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, currentPage = 0) }
    }

    fun selectSort(field: HomeSortField) {
        _state.update { current ->
            val nextAscending = if (current.sortField == field) !current.sortAscending else false
            val sorted = applySorting(current.items, field, nextAscending)
            val pageCount = calculatePageCount(sorted.size, current.pageSize)
            current.copy(
                sortField = field,
                sortAscending = nextAscending,
                currentPage = 0,
                pageCount = pageCount,
                items = sorted
            )
        }
    }

    fun setPageSize(size: Int) {
        val normalized = when (size) {
            10, 20, 50 -> size
            else -> 10
        }
        viewModelScope.launch {
            settingsRepository.savePageSize(normalized)
        }
    }

    fun goToPreviousPage() {
        _state.update { current ->
            if (current.pageCount <= 1) return@update current
            current.copy(currentPage = (current.currentPage - 1).coerceAtLeast(0))
        }
    }

    fun goToNextPage() {
        _state.update { current ->
            if (current.pageCount <= 1) return@update current
            current.copy(currentPage = (current.currentPage + 1).coerceAtMost(current.pageCount - 1))
        }
    }

    fun goToPage(pageIndex: Int) {
        _state.update { current ->
            if (current.pageCount <= 0) return@update current
            current.copy(currentPage = pageIndex.coerceIn(0, current.pageCount - 1))
        }
    }

    fun showDeleteDialog(item: Item) {
        _state.update { it.copy(showDeleteDialog = true, itemToDelete = item) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false, itemToDelete = null) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            val state = _state.value
            // 批量删除：处于选择模式且选中了项目
            if (state.isSelectionMode && state.selectedItemIds.isNotEmpty()) {
                deleteSelectedItems()
                dismissDeleteDialog()
            } else if (state.itemToDelete != null) {
                // 单个删除
                deleteItemUseCase(state.itemToDelete)
                dismissDeleteDialog()
            }
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }
                val jsonString = exportItemsUseCase()
                fileRepository.writeString(uri, jsonString)
                _state.update { it.copy(isExporting = false, exportSuccess = true) }
                delay(1000)
                dismissMessage()
            } catch (e: Exception) {
                _state.update { it.copy(isExporting = false, errorMessage = e.message ?: "Export failed") }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isImporting = true, importSuccess = false, errorMessage = null) }
                val jsonString = fileRepository.readString(uri)
                importItemsUseCase(jsonString)
                _state.update { it.copy(isImporting = false, importSuccess = true) }
                delay(1000)
                dismissMessage()
            } catch (e: Exception) {
                _state.update { it.copy(isImporting = false, errorMessage = e.message ?: "Import failed") }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(exportSuccess = false, importSuccess = false, errorMessage = null, infoMessage = null) }
    }

    fun analyzeText(text: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isAIProcessing = true, errorMessage = null) }
                val extractedItems = withTimeout(60_000) {
                    withContext(Dispatchers.IO) {
                        siliconFlowRepository.analyzeText(text)
                    }
                }

                // 过滤无效名称的物品
                val validItems = extractedItems.filter { !it.name.isNullOrBlank() }
                if (validItems.isEmpty()) {
                    throw IllegalStateException("无法识别任何物品名称")
                }

                val addedNames = mutableListOf<String>()
                validItems.forEach { extractedItem ->
                    val name = extractedItem.name!!.trim()
                    val purchaseDate = extractedItem.date?.let { parseDateToMillis(it) }
                    val usageDays = purchaseDate?.let { dateMillis ->
                        val diff = System.currentTimeMillis() - dateMillis
                        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                        if (days >= 0) days.toInt() else null
                    }

                    addItemUseCase(
                        Item(
                            name = name,
                            location = extractedItem.location?.trim()?.takeIf { it.isNotBlank() },
                            purchaseDate = purchaseDate,
                            purchasePrice = extractedItem.price ?: 0.0,
                            usageDays = usageDays,
                            note = extractedItem.note?.trim()?.takeIf { it.isNotBlank() },
                            tags = extractedItem.tags?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                            imagePath = null
                        )
                    )
                    addedNames.add(name)
                }

                val message = if (addedNames.size == 1) {
                    "AI 已添加：${addedNames.first()}"
                } else {
                    "AI 已添加 ${addedNames.size} 个物品：${addedNames.joinToString("、")}"
                }
                _state.update { it.copy(isAIProcessing = false, infoMessage = message) }
                delay(2000)
                dismissMessage()
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _state.update { it.copy(isAIProcessing = false, errorMessage = "AI 分析超时，请稍后重试") }
            } catch (e: Exception) {
                val message = e.message
                if (e is IOException && message == "Canceled") {
                    _state.update { it.copy(isAIProcessing = false, errorMessage = "AI 分析超时，请稍后重试") }
                } else {
                    _state.update { it.copy(isAIProcessing = false, errorMessage = message ?: "AI Analysis failed") }
                }
            }
        }
    }

    private fun parseDateToMillis(dateString: String): Long? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun applySorting(
        items: List<Item>,
        field: HomeSortField,
        ascending: Boolean
    ): List<Item> {
        fun <T : Comparable<T>> List<Item>.sortByKey(
            keySelectorAsc: (Item) -> T,
            keySelectorDesc: (Item) -> T
        ): List<Item> {
            return if (ascending) this.sortedWith(compareBy(keySelectorAsc).thenByDescending { it.updatedAt })
            else this.sortedWith(compareByDescending(keySelectorDesc).thenByDescending { it.updatedAt })
        }

        return when (field) {
            PurchaseDate -> items.sortByKey(
                keySelectorAsc = { it.purchaseDate ?: Long.MAX_VALUE },
                keySelectorDesc = { it.purchaseDate ?: Long.MIN_VALUE }
            )
            Name -> {
                val comparator = compareBy<Item> { it.name.lowercase() }.thenByDescending { it.updatedAt }
                if (ascending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
            }
            UsageDays -> items.sortByKey(
                keySelectorAsc = { it.usageDays ?: Int.MAX_VALUE },
                keySelectorDesc = { it.usageDays ?: Int.MIN_VALUE }
            )
            UpdatedAt -> {
                val comparator = compareByDescending<Item> { it.updatedAt }
                if (ascending) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
            }
            CreatedAt -> {
                val comparator = compareByDescending<Item> { it.createdAt }
                if (ascending) items.sortedWith(comparator.reversed()) else items.sortedWith(comparator)
            }
        }
    }


    private fun calculatePageCount(itemCount: Int, pageSize: Int): Int {
        if (itemCount <= 0) return 0
        val safePageSize = pageSize.coerceAtLeast(1)
        return (itemCount + safePageSize - 1) / safePageSize
    }

    private fun clampPage(currentPage: Int, pageCount: Int): Int {
        if (pageCount <= 0) return 0
        return currentPage.coerceIn(0, pageCount - 1)
    }
}
