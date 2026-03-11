package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals

class AddItemUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: AddItemUseCase

    private val testItem = Item(name = "Laptop", location = "Desk")

    @Before
    fun setup() {
        repository = mockk()
        useCase = AddItemUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should insert item and return id`() = runTest {
        // Given
        coEvery { repository.insertItem(any()) } returns 1L

        // When
        val result = useCase.invoke(testItem)

        // Then
        coVerify { repository.insertItem(any()) }
        assertEquals(1L, result)
    }
}
