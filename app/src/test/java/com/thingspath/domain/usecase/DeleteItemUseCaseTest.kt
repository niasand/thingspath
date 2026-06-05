package com.thingspath.domain.usecase

import android.util.Log
import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import com.thingspath.data.remote.repository.R2ImageRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DeleteItemUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var r2ImageRepository: R2ImageRepository
    private lateinit var useCase: DeleteItemUseCase

    private val testItem = Item(id = 1, name = "Laptop", location = "Desk")
    private val testItemWithR2Image = Item(
        id = 2,
        name = "Phone",
        imagePaths = listOf("https://pub-f8476d65a2624e369b2c05fe999e8104.r2.dev/items/test-uuid.jpg")
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        repository = mockk(relaxed = true)
        r2ImageRepository = mockk(relaxed = true)
        useCase = DeleteItemUseCase(repository, r2ImageRepository)
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

    @Test
    fun `invoke should delete R2 images when item has R2 URLs`() = runTest {
        // Given
        every { r2ImageRepository.isR2Url(any()) } returns true
        every { r2ImageRepository.extractKeyFromUrl(any()) } returns "items/test-uuid.jpg"
        coEvery { r2ImageRepository.deleteImage(any()) } returns true
        coEvery { repository.deleteItem(any()) } just Runs

        // When
        useCase.invoke(testItemWithR2Image)

        // Then
        coVerify { r2ImageRepository.deleteImage("items/test-uuid.jpg") }
        coVerify { repository.deleteItem(testItemWithR2Image) }
    }

    @Test
    fun `invoke should skip local file paths`() = runTest {
        // Given
        val itemWithLocalPath = Item(id = 3, name = "Tablet", imagePaths = listOf("/data/data/.../image.jpg"))
        every { r2ImageRepository.isR2Url("/data/data/.../image.jpg") } returns false
        coEvery { repository.deleteItem(any()) } just Runs

        // When
        useCase.invoke(itemWithLocalPath)

        // Then
        coVerify(exactly = 0) { r2ImageRepository.deleteImage(any()) }
        coVerify { repository.deleteItem(itemWithLocalPath) }
    }
}
