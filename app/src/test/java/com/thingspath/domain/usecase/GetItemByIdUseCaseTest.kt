package com.thingspath.domain.usecase

import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class GetItemByIdUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: GetItemByIdUseCase

    private val testItem = Item(id = 1, name = "Laptop", location = "Desk")

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetItemByIdUseCase(repository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `invoke should return item when found`() = runTest {
        // Given
        coEvery { repository.getItemById(1) } returns testItem

        // When
        val result = useCase.invoke(1)

        // Then
        assertEquals(testItem, result)
    }

    @Test
    fun `invoke should return null when not found`() = runTest {
        // Given
        coEvery { repository.getItemById(999) } returns null

        // When
        val result = useCase.invoke(999)

        // Then
        assertNull(result)
    }
}
