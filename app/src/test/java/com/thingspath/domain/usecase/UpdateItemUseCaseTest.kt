package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class UpdateItemUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: UpdateItemUseCase

    private val testItem = Item(id = 1, name = "Laptop", location = "Desk")

    @Before
    fun setup() {
        repository = mockk()
        useCase = UpdateItemUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should update item`() = runTest {
        // Given
        coEvery { repository.updateItem(any()) } just Runs

        // When
        useCase.invoke(testItem)

        // Then
        coVerify { repository.updateItem(testItem) }
    }
}
