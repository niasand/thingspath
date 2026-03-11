package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeleteItemUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: DeleteItemUseCase

    private val testItem = Item(id = 1, name = "Laptop", location = "Desk")

    @Before
    fun setup() {
        repository = mockk()
        useCase = DeleteItemUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should delete item`() = runTest {
        // Given
        coEvery { repository.deleteItem(any()) } just Runs

        // When
        useCase.invoke(testItem)

        // Then
        coVerify { repository.deleteItem(testItem) }
    }

    @Test
    fun `invoke with id should delete item by id`() = runTest {
        // Given
        coEvery { repository.deleteItemById(any()) } just Runs

        // When
        useCase.invoke(1L)

        // Then
        coVerify { repository.deleteItemById(1L) }
    }
}
