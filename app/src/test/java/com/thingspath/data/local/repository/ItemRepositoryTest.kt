package com.thingspath.data.local.repository

import android.util.Log
import com.google.gson.Gson
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.local.datasource.LocalItemDataSource
import com.thingspath.data.model.Item
import com.thingspath.data.remote.datasource.RemoteItemDataSource
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ItemRepository focusing on its coordination logic:
 * - When to push/pull data between local and remote
 * - Cache delegation to LocalItemDataSource
 * - Error handling when remote operations fail
 * - Incremental sync watermark management
 *
 * Low-level CRUD is covered by LocalItemDataSource/RemoteItemDataSource tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemRepositoryTest {

    @get:Rule
    val dispatcherRule = com.thingspath.util.MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var localDataSource: LocalItemDataSource
    private lateinit var remoteDataSource: RemoteItemDataSource
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var repository: ItemRepository
    private val gson = Gson()

    private val testItem = Item(
        id = 1L,
        name = "Laptop",
        imagePaths = listOf("https://example.com/laptop.jpg"),
        location = "Desk",
        purchasePrice = 9999.0,
        tags = listOf("electronics", "work"),
        createdAt = 1000L,
        updatedAt = 2000L
    )

    private val testEntity = ItemEntity(
        id = 1L,
        name = "Laptop",
        imagePaths = listOf("https://example.com/laptop.jpg"),
        imagePath = "https://example.com/laptop.jpg",
        location = "Desk",
        purchasePrice = 9999.0,
        tags = listOf("electronics", "work"),
        createdAt = 1000L,
        updatedAt = 2000L
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        localDataSource = mockk(relaxed = true)
        remoteDataSource = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Default: no sync watermarks
        every { settingsRepository.lastPullUpdatedAt } returns flowOf(0L)
        every { settingsRepository.lastPushUpdatedAt } returns flowOf(0L)

        // Default: local returns empty flow for getAllItems
        every { localDataSource.getAllItems() } returns flowOf(emptyList())
        every { localDataSource.items } returns flowOf(emptyList())
        coEvery { localDataSource.itemCount() } returns 1  // non-empty to skip init full pull
        coEvery { localDataSource.getMaxUpdatedAt() } returns 0L
        coEvery { remoteDataSource.ensureTableExists() } just Runs
        coEvery { remoteDataSource.getAllItems() } returns emptyList()

        repository = ItemRepository(localDataSource, remoteDataSource, settingsRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Insert ==========

    @Test
    fun `insertItem delegates to remote first then local`() = runTest {
        // Remote returns the generated ID
        coEvery {
            remoteDataSource.insertItem(
                name = testItem.name,
                imagePaths = gson.toJson(testItem.imagePaths),
                location = testItem.location,
                purchaseDate = testItem.purchaseDate,
                purchasePrice = testItem.purchasePrice,
                usageDays = testItem.usageDays,
                note = testItem.note,
                tags = gson.toJson(testItem.tags),
                createdAt = testItem.createdAt,
                updatedAt = testItem.updatedAt
            )
        } returns 42L

        coEvery { localDataSource.insertItem(any()) } returns 42L

        val result = repository.insertItem(testItem)

        assertEquals(42L, result)
        // Verify remote insert was called first
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            remoteDataSource.insertItem(
                name = testItem.name,
                imagePaths = gson.toJson(testItem.imagePaths),
                location = testItem.location,
                purchaseDate = testItem.purchaseDate,
                purchasePrice = testItem.purchasePrice,
                usageDays = testItem.usageDays,
                note = testItem.note,
                tags = gson.toJson(testItem.tags),
                createdAt = testItem.createdAt,
                updatedAt = testItem.updatedAt
            )
            localDataSource.insertItem(match { it.id == 42L })
        }
    }

    // ========== Update ==========

    @Test
    fun `updateItem updates local first then syncs to remote`() = runTest {
        coEvery { remoteDataSource.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns true

        repository.updateItem(testItem)

        // Local update should be called
        coVerify { localDataSource.updateItem(any()) }
        // Remote update should be called with JSON-serialized fields
        coVerify {
            remoteDataSource.updateItem(
                id = testItem.id,
                name = testItem.name,
                imagePaths = gson.toJson(testItem.imagePaths),
                location = testItem.location,
                purchaseDate = testItem.purchaseDate,
                purchasePrice = testItem.purchasePrice,
                usageDays = testItem.usageDays,
                note = testItem.note,
                tags = gson.toJson(testItem.tags),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateItem falls back to remote insert when update matches 0 rows`() = runTest {
        coEvery {
            remoteDataSource.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns false

        repository.updateItem(testItem)

        // Verify fallback: insert was called after update returned false
        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            remoteDataSource.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            remoteDataSource.insertItem(
                id = testItem.id,
                name = testItem.name,
                imagePaths = any(),
                location = any(),
                purchaseDate = any(),
                purchasePrice = any(),
                usageDays = any(),
                note = any(),
                tags = any(),
                createdAt = any(),
                updatedAt = any()
            )
        }
    }

    @Test
    fun `updateItem handles remote failure gracefully`() = runTest {
        coEvery {
            remoteDataSource.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("Network error")

        // Should not throw — error is caught and logged
        repository.updateItem(testItem)

        // Local update must still have happened
        coVerify { localDataSource.updateItem(any()) }
    }

    // ========== Delete ==========

    @Test
    fun `deleteItem removes locally first then syncs to remote`() = runTest {
        repository.deleteItem(testItem)

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            localDataSource.deleteItemById(testItem.id)
            remoteDataSource.deleteItemById(testItem.id)
        }
    }

    @Test
    fun `deleteItemById handles remote failure gracefully`() = runTest {
        coEvery { remoteDataSource.deleteItemById(any()) } throws RuntimeException("Network error")

        // Should not throw
        repository.deleteItemById(99L)

        // Local delete must still have happened
        coVerify { localDataSource.deleteItemById(99L) }
    }

    @Test
    fun `deleteItemsByIds syncs bulk delete to remote`() = runTest {
        val ids = setOf(1L, 2L, 3L)

        repository.deleteItemsByIds(ids)

        coVerify { localDataSource.deleteItemsByIds(ids.toList()) }
        coVerify { remoteDataSource.deleteItemsByIds(ids.toList()) }
    }

    @Test
    fun `deleteItemsByIds handles remote failure gracefully`() = runTest {
        coEvery { remoteDataSource.deleteItemsByIds(any()) } throws RuntimeException("Network error")

        // Should not throw
        repository.deleteItemsByIds(setOf(1L, 2L))

        coVerify { localDataSource.deleteItemsByIds(any()) }
    }

    // ========== Cache delegation ==========

    @Test
    fun `getCachedItemById delegates to localDataSource`() {
        every { localDataSource.getCachedItemById(1L) } returns testItem

        val result = repository.getCachedItemById(1L)

        assertEquals(testItem, result)
        verify { localDataSource.getCachedItemById(1L) }
    }

    @Test
    fun `clearCache delegates to localDataSource`() {
        repository.clearCache()

        verify { localDataSource.clearCache() }
    }

    // ========== getAllItems Flow ==========

    @Test
    fun `getAllItems returns flow from localDataSource`() = runTest {
        val items = listOf(testItem)
        every { localDataSource.getAllItems() } returns flowOf(items)

        val result = repository.getAllItems().first()

        assertEquals(items, result)
    }

    // ========== Incremental pull ==========

    @Test
    fun `refreshFromRemote calls incrementalSync - pull then push`() = runTest {
        // No remote changes, no local changes
        coEvery { remoteDataSource.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { localDataSource.getItemsUpdatedAfter(any()) } returns emptyList()

        repository.refreshFromRemote()

        // pull: remote getItemsUpdatedAfter called with watermark
        coVerify { remoteDataSource.getItemsUpdatedAfter(0L) }
        // push: local getItemsUpdatedAfter called with watermark
        coVerify { localDataSource.getItemsUpdatedAfter(0L) }
    }

    @Test
    fun `incremental pull inserts remote items locally and advances watermark`() = runTest {
        val remoteEntity = testEntity.copy(id = 10, updatedAt = 5000L)
        coEvery { settingsRepository.lastPullUpdatedAt } returns flowOf(1000L)
        coEvery { remoteDataSource.getItemsUpdatedAfter(1000L) } returns listOf(remoteEntity)
        coEvery { localDataSource.getItemsUpdatedAfter(any()) } returns emptyList()

        repository.refreshFromRemote()

        coVerify { localDataSource.insertItems(listOf(remoteEntity)) }
        coVerify { settingsRepository.setLastPullUpdatedAt(5000L) }
    }

    @Test
    fun `incremental pull skips insert when no remote changes`() = runTest {
        coEvery { settingsRepository.lastPullUpdatedAt } returns flowOf(3000L)
        coEvery { remoteDataSource.getItemsUpdatedAfter(3000L) } returns emptyList()
        coEvery { localDataSource.getItemsUpdatedAfter(any()) } returns emptyList()

        repository.refreshFromRemote()

        // insertItems should NOT be called for pull
        coVerify(exactly = 0) { localDataSource.insertItems(any()) }
    }

    // ========== Incremental push ==========

    @Test
    fun `incremental push upserts local changes to remote and advances watermark`() = runTest {
        val localEntity = testEntity.copy(id = 20, updatedAt = 6000L)
        coEvery { settingsRepository.lastPullUpdatedAt } returns flowOf(0L)
        coEvery { remoteDataSource.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { settingsRepository.lastPushUpdatedAt } returns flowOf(1000L)
        coEvery { localDataSource.getItemsUpdatedAfter(1000L) } returns listOf(localEntity)

        repository.refreshFromRemote()

        coVerify {
            remoteDataSource.upsertItem(
                id = localEntity.id,
                name = localEntity.name,
                imagePaths = gson.toJson(localEntity.imagePaths),
                location = localEntity.location,
                purchaseDate = localEntity.purchaseDate,
                purchasePrice = localEntity.purchasePrice,
                usageDays = localEntity.usageDays,
                note = localEntity.note,
                tags = gson.toJson(localEntity.tags),
                createdAt = localEntity.createdAt,
                updatedAt = localEntity.updatedAt
            )
        }
        coVerify { settingsRepository.setLastPushUpdatedAt(6000L) }
    }

    @Test
    fun `incremental push handles per-item failure without blocking remaining items`() = runTest {
        val entity1 = testEntity.copy(id = 1, updatedAt = 3000L)
        val entity2 = testEntity.copy(id = 2, updatedAt = 4000L)

        coEvery { settingsRepository.lastPullUpdatedAt } returns flowOf(0L)
        coEvery { remoteDataSource.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { settingsRepository.lastPushUpdatedAt } returns flowOf(0L)
        coEvery { localDataSource.getItemsUpdatedAfter(0L) } returns listOf(entity1, entity2)

        // First item fails, second succeeds
        coEvery { remoteDataSource.upsertItem(id = 1, any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("fail")
        coEvery { remoteDataSource.upsertItem(id = 2, any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        repository.refreshFromRemote()

        // Both items attempted
        coVerify { remoteDataSource.upsertItem(id = 1, any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify { remoteDataSource.upsertItem(id = 2, any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        // Watermark still advances to max successful push
        coVerify { settingsRepository.setLastPushUpdatedAt(4000L) }
    }

    @Test
    fun `incremental push skips when no local changes`() = runTest {
        coEvery { settingsRepository.lastPullUpdatedAt } returns flowOf(0L)
        coEvery { remoteDataSource.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { settingsRepository.lastPushUpdatedAt } returns flowOf(5000L)
        coEvery { localDataSource.getItemsUpdatedAfter(5000L) } returns emptyList()

        repository.refreshFromRemote()

        coVerify(exactly = 0) { remoteDataSource.upsertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    // ========== Full pull (pullFromD1) ==========

    @Test
    fun `pullFromD1 inserts all remote items locally and resets watermarks`() = runTest {
        val remoteEntities = listOf(
            testEntity.copy(id = 1, updatedAt = 3000L),
            testEntity.copy(id = 2, updatedAt = 5000L)
        )
        coEvery { remoteDataSource.getAllItems() } returns remoteEntities
        coEvery { localDataSource.getMaxUpdatedAt() } returns 5000L

        repository.pullFromD1()

        coVerify { remoteDataSource.getAllItems() }
        coVerify { localDataSource.insertItems(remoteEntities) }
        coVerify { localDataSource.getMaxUpdatedAt() }
        coVerify { settingsRepository.setLastPullUpdatedAt(5000L) }
        coVerify { settingsRepository.setLastPushUpdatedAt(5000L) }
    }

    @Test
    fun `pullFromD1 handles failure gracefully`() = runTest {
        coEvery { remoteDataSource.getAllItems() } throws RuntimeException("Network down")

        // Should not throw
        repository.pullFromD1()

        // Local insert should not be called
        coVerify(exactly = 0) { localDataSource.insertItems(any()) }
    }

    // ========== Edge cases ==========

    @Test
    fun `getItemById delegates to localDataSource`() = runTest {
        coEvery { localDataSource.getItemById(1L) } returns testItem

        val result = repository.getItemById(1L)

        assertEquals(testItem, result)
    }

    @Test
    fun `getItemById returns null when not found`() = runTest {
        coEvery { localDataSource.getItemById(999L) } returns null

        val result = repository.getItemById(999L)

        assertNull(result)
    }

    @Test
    fun `searchItems delegates to localDataSource`() = runTest {
        val searchResults = listOf(testItem)
        every { localDataSource.searchItems("laptop") } returns flowOf(searchResults)

        val result = repository.searchItems("laptop").first()

        assertEquals(searchResults, result)
    }

    @Test
    fun `insertItems iterates and inserts each item`() = runTest {
        val items = listOf(
            testItem.copy(id = 0, name = "Item A"),
            testItem.copy(id = 0, name = "Item B")
        )
        coEvery { remoteDataSource.insertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 1L
        coEvery { localDataSource.insertItem(any()) } returns 1L

        repository.insertItems(items)

        coVerify(exactly = 2) { remoteDataSource.insertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        coVerify(exactly = 2) { localDataSource.insertItem(any()) }
    }
}
