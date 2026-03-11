package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class GetItemsUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: GetItemsUseCase

    private val testItems = listOf(
        Item(id = 1, name = "Laptop", location = "Desk", purchaseDate = 1640000000000L),
        Item(id = 2, name = "Phone", location = "Pocket", purchaseDate = 1650000000000L)
    )

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetItemsUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should return all items when no search query`() = runTest {
        // Given
        every { repository.getAllItems() } returns flowOf(testItems)

        // When
        val result = useCase.invoke(null)

        // Then
        repository.getAllItems().collect { items ->
            assertEquals(testItems, items)
        }
    }

    @Test
    fun `invoke should search items when query provided`() = runTest {
        // Given
        val searchResults = listOf(testItems[0])
        every { repository.searchItems("%Laptop%") } returns flowOf(searchResults)

        // When
        val result = useCase.invoke("Laptop")

        // Then
        repository.searchItems("%Laptop%").collect { items ->
            assertEquals(searchResults, items)
        }
    }
}
