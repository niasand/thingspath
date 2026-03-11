package com.thingspath.ui.screen.itemdetail

import androidx.lifecycle.SavedStateHandle
import com.thingspath.domain.usecase.*
import com.thingspath.util.MainDispatcherRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getItemByIdUseCase: GetItemByIdUseCase = mockk(relaxed = true)
    private val updateItemUseCase: UpdateItemUseCase = mockk(relaxed = true)
    private val deleteItemUseCase: DeleteItemUseCase = mockk(relaxed = true)

    @Test
    fun `viewModel initialization with valid itemId should not crash`() {
        // Simulate navigation argument passing a Long
        val savedStateHandle = SavedStateHandle(mapOf("itemId" to 123L))
        
        // This should not throw ClassCastException
        val viewModel = ItemDetailViewModel(
            savedStateHandle = savedStateHandle,
            getItemByIdUseCase = getItemByIdUseCase,
            updateItemUseCase = updateItemUseCase,
            deleteItemUseCase = deleteItemUseCase
        )
        
        // If we reach here, the test passes
    }
}
