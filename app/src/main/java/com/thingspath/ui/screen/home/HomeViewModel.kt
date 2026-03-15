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

import com.thingspath.data.local.repository.ItemRepository
import android.net.Uri

import android.util.Log

import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.delay

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val itemRepository: ItemRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            itemRepository.restoreDataIfNeeded()
        }
        loadItems()
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun loadItems() {
        viewModelScope.launch {
            // Re-collect items whenever searchQuery changes
            _state.map { it.searchQuery }
                .distinctUntilChanged()
                .debounce(300)
                .flatMapLatest { query ->
                    Log.d("HomeViewModel", "Searching for: '$query'")
                    getItemsUseCase(query)
                        .catch { e ->
                            Log.e("HomeViewModel", "Error getting items", e)
                            emit(emptyList())
                        }
                }
                .collect { items ->
                    Log.d("HomeViewModel", "Found ${items.size} items")
                    val totalCount = items.size
                    val totalPrice = items.sumOf { it.purchasePrice }
                    _state.update { 
                        it.copy(
                            items = items, 
                            isLoading = false,
                            totalItemCount = totalCount,
                            totalPrice = totalPrice
                        ) 
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleView() {
        _state.update { it.copy(isGridView = !it.isGridView) }
    }

    fun showDeleteDialog(item: Item) {
        _state.update { it.copy(showDeleteDialog = true, itemToDelete = item) }
    }

    fun dismissDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false, itemToDelete = null) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _state.value.itemToDelete?.let { item ->
                deleteItemUseCase(item)
                dismissDeleteDialog()
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(errorMessage = null) }
    }
}
