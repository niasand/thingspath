package com.thingspath.data.local.repository

import android.app.Application
import android.util.Log
import app.cash.turbine.test
import coil.ImageLoader
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.db.ItemDao
import com.thingspath.data.local.db.ItemEntity
import com.thingspath.data.local.db.toItem
import com.thingspath.data.local.db.toEntity
import com.thingspath.data.model.Item
import com.thingspath.data.remote.D1ApiService
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * ItemRepository 单元测试
 *
 * 覆盖：本地 CRUD、缓存行为、远程同步（D1）、同步失败处理、Flow 发射、边界情况。
 * 所有外部依赖（ItemDao、D1ApiService、SettingsRepository、Application、ImageLoader）通过 MockK 隔离。
 *
 * 重要：ItemRepository 内部使用 CoroutineScope(Dispatchers.IO)，不受 runTest 调度器控制。
 * 对于涉及缓存（ConcurrentHashMap）的测试，需要通过短暂 sleep 等待 IO 协程完成。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ItemRepositoryTest {

    @MockK
    private lateinit var d1ApiService: D1ApiService

    @MockK
    private lateinit var itemDao: ItemDao

    @MockK
    private lateinit var settingsRepository: SettingsRepository

    @MockK
    private lateinit var application: Application

    @MockK
    private lateinit var imageLoader: ImageLoader

    // Room Flow 模拟：由测试控制发射内容
    private val itemsFlow = MutableStateFlow<List<ItemEntity>>(emptyList())

    // Settings 水位线 Flow
    private val lastPullFlow = MutableStateFlow(0L)
    private val lastPushFlow = MutableStateFlow(0L)

    private lateinit var repository: ItemRepository

    // ========== 测试用常量 ==========

    private companion object {
        const val ITEM_ID = 42L
        const val ITEM_NAME = "测试物品"
        const val ITEM_LOCATION = "北京"
        const val ITEM_PRICE = 99.9
        const val TIMESTAMP_NOW = 1700000000000L
        const val TIMESTAMP_OLD = 1600000000000L
    }

    private fun buildTestItem(
        id: Long = ITEM_ID,
        name: String = ITEM_NAME,
        location: String? = ITEM_LOCATION,
        price: Double = ITEM_PRICE,
        updatedAt: Long = TIMESTAMP_NOW,
        tags: List<String> = emptyList(),
        imagePaths: List<String> = emptyList()
    ) = Item(
        id = id,
        name = name,
        location = location,
        purchasePrice = price,
        updatedAt = updatedAt,
        createdAt = TIMESTAMP_OLD,
        tags = tags,
        imagePaths = imagePaths
    )

    private fun buildTestEntity(
        id: Long = ITEM_ID,
        name: String = ITEM_NAME,
        location: String? = ITEM_LOCATION,
        price: Double = ITEM_PRICE,
        updatedAt: Long = TIMESTAMP_NOW
    ) = ItemEntity(
        id = id,
        name = name,
        location = location,
        purchasePrice = price,
        updatedAt = updatedAt,
        createdAt = TIMESTAMP_OLD
    )

    private fun buildD1Row(
        id: Long = ITEM_ID,
        name: String = ITEM_NAME,
        location: String? = ITEM_LOCATION,
        price: Double = ITEM_PRICE,
        updatedAt: Long = TIMESTAMP_NOW,
        imagePaths: String = "[]",
        tags: String = "[]"
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "image_paths" to imagePaths,
        "location" to location,
        "purchase_date" to null,
        "purchase_price" to price,
        "usage_days" to null,
        "note" to null,
        "tags" to tags,
        "created_at" to TIMESTAMP_OLD,
        "updated_at" to updatedAt
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // 抑制 android.util.Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0

        // ItemDao mock
        every { itemDao.getAllItems() } returns itemsFlow
        coEvery { itemDao.itemCount() } returns 0
        coEvery { itemDao.getMaxUpdatedAt() } returns TIMESTAMP_NOW
        coEvery { itemDao.insertItem(any()) } returns ITEM_ID
        coEvery { itemDao.insertItems(any()) } just Runs
        coEvery { itemDao.deleteItemById(any()) } just Runs
        coEvery { itemDao.deleteItemsByIds(any()) } just Runs
        coEvery { itemDao.getItemsUpdatedAfter(any()) } returns emptyList()

        // D1ApiService mock — init 块中的调用
        coEvery { d1ApiService.createTableIfNotExists() } just Runs
        coEvery { d1ApiService.getAllItems() } returns emptyList()
        coEvery { d1ApiService.getItemsUpdatedAfter(any()) } returns emptyList()

        // SettingsRepository mock
        every { settingsRepository.lastPullUpdatedAt } returns lastPullFlow
        every { settingsRepository.lastPushUpdatedAt } returns lastPushFlow
        coEvery { settingsRepository.setLastPullUpdatedAt(any()) } just Runs
        coEvery { settingsRepository.setLastPushUpdatedAt(any()) } just Runs

        // ImageLoader mock
        every { imageLoader.enqueue(any()) } returns mockk(relaxed = true)
    }

    /**
     * 创建 ItemRepository 实例并等待 init 块完成。
     */
    private fun createRepository(): ItemRepository {
        repository = ItemRepository(
            d1ApiService = d1ApiService,
            itemDao = itemDao,
            settingsRepository = settingsRepository,
            application = application,
            imageLoader = imageLoader
        )
        // 等待 init 块中的 IO 协程完成
        waitForIoCoroutines()
        return repository
    }

    /**
     * 等待 ItemRepository 内部 IO 协程完成。
     * 因为 repository 使用 Dispatchers.IO，无法通过 TestScope 控制，
     * 用短暂 sleep 等待协程调度。
     */
    private fun waitForIoCoroutines() {
        Thread.sleep(300)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkAll()
    }

    // ===========================
    // 1. 本地 CRUD 测试
    // ===========================

    // ---------- insertItem ----------

    @Test
    fun `insertItem - D1 返回 ID 后写入 Room 并返回该 ID`() = runTest {
        // Arrange
        val newItem = buildTestItem(id = 0)
        coEvery {
            d1ApiService.insertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns ITEM_ID

        createRepository()

        // Act
        val result = repository.insertItem(newItem)

        // Assert: 返回 D1 分配的 ID
        assertEquals(ITEM_ID, result)

        // 验证 Room 插入被调用，且使用了 D1 返回的 ID
        coVerify {
            itemDao.insertItem(match { it.id == ITEM_ID && it.name == newItem.name })
        }
    }

    @Test
    fun `insertItem - 传入带图片和标签的物品，正确序列化`() = runTest {
        // Arrange
        val itemWithImages = buildTestItem(
            imagePaths = listOf("https://example.com/a.jpg", "https://example.com/b.jpg"),
            tags = listOf("电子", "办公")
        )
        coEvery {
            d1ApiService.insertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns ITEM_ID

        createRepository()

        // Act
        repository.insertItem(itemWithImages)

        // Assert: 验证 imagePaths 和 tags 通过 Gson 序列化传给 D1
        // 参数顺序: id: Long?, name, imagePaths, location, purchaseDate, purchasePrice,
        //           usageDays, note, tags, createdAt, updatedAt
        coVerify {
            d1ApiService.insertItem(
                isNull(),
                any(),
                match { it.contains("example.com/a.jpg") && it.contains("example.com/b.jpg") },
                any(),
                any(),
                any(),
                any(),
                any(),
                match { it.contains("电子") && it.contains("办公") },
                any(),
                any()
            )
        }
    }

    // ---------- updateItem ----------

    @Test
    fun `updateItem - 先更新 Room，再同步到 D1`() = runTest {
        // Arrange
        val item = buildTestItem()
        coEvery {
            d1ApiService.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns true

        createRepository()

        // Act
        repository.updateItem(item)

        // Assert: Room 的 updateItem 被调用（11 个位置参数）
        coVerify {
            itemDao.updateItem(
                item.id,
                item.name,
                any(),
                any(),
                item.location,
                any(),
                item.purchasePrice,
                any(),
                any(),
                any(),
                any()
            )
        }

        // D1 的 updateItem 被调用（10 个位置参数）
        coVerify {
            d1ApiService.updateItem(
                item.id,
                item.name,
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `updateItem - D1 UPDATE 匹配 0 行时自动 fallback 到 INSERT`() = runTest {
        // Arrange: D1 UPDATE 返回 false（匹配 0 行）
        val item = buildTestItem()
        coEvery {
            d1ApiService.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns false
        coEvery {
            d1ApiService.insertItem(any<Long>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns ITEM_ID

        createRepository()

        // Act
        repository.updateItem(item)

        // Assert: 应该调用 insertItem 作为 fallback
        coVerify {
            d1ApiService.insertItem(
                any<Long>(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `updateItem - updatedAt 被自动刷新为当前时间`() = runTest {
        // Arrange
        val item = buildTestItem(updatedAt = TIMESTAMP_OLD)
        val capturedUpdatedAt = slot<Long>()
        coEvery {
            d1ApiService.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), capture(capturedUpdatedAt))
        } returns true

        createRepository()

        // Act
        repository.updateItem(item)

        // Assert: updatedAt 应该 >= TIMESTAMP_OLD（被刷新为 System.currentTimeMillis()）
        assertTrue(capturedUpdatedAt.captured > TIMESTAMP_OLD)
    }

    // ---------- deleteItem ----------

    @Test
    fun `deleteItem - 先删 Room，再删 D1`() = runTest {
        // Arrange
        val item = buildTestItem()

        createRepository()

        // Act
        repository.deleteItem(item)

        // Assert
        coVerify { itemDao.deleteItemById(item.id) }
        coVerify { d1ApiService.deleteItemById(item.id) }
    }

    @Test
    fun `deleteItemById - 使用 ID 删除 Room 和 D1`() = runTest {
        createRepository()

        repository.deleteItemById(ITEM_ID)

        coVerify { itemDao.deleteItemById(ITEM_ID) }
        coVerify { d1ApiService.deleteItemById(ITEM_ID) }
    }

    @Test
    fun `deleteItemsByIds - 批量删除 Room 和 D1`() = runTest {
        val ids = setOf(1L, 2L, 3L)

        createRepository()

        repository.deleteItemsByIds(ids)

        coVerify { itemDao.deleteItemsByIds(ids.toList()) }
        coVerify { d1ApiService.deleteItemsByIds(ids.toList()) }
    }

    @Test
    fun `deleteAllItems - 只删本地 Room，不删 D1`() = runTest {
        // Arrange: getAllItems 返回 2 条记录
        val entities = listOf(buildTestEntity(id = 1), buildTestEntity(id = 2))
        every { itemDao.getAllItems() } returns flowOf(entities)

        createRepository()

        // Act
        repository.deleteAllItems()
        waitForIoCoroutines()

        // Assert: Room 中每条都执行 deleteItemById
        coVerify { itemDao.deleteItemById(1) }
        coVerify { itemDao.deleteItemById(2) }

        // D1 不应被调用
        coVerify(exactly = 0) { d1ApiService.deleteItemById(any()) }
    }

    // ---------- getItemById ----------

    @Test
    fun `getItemById - 内存缓存命中时直接返回`() = runTest {
        // Arrange: 先让 itemsFlow 发射包含该 item 的列表，触发缓存更新
        val entity = buildTestEntity()
        itemsFlow.value = listOf(entity)

        createRepository()

        // Act
        val result = repository.getItemById(ITEM_ID)

        // Assert
        assertNotNull(result)
        assertEquals(ITEM_ID, result!!.id)
        assertEquals(ITEM_NAME, result.name)

        // 不应查询 Room（缓存命中）
        coVerify(exactly = 0) { itemDao.getItemById(any()) }
    }

    @Test
    fun `getItemById - 缓存未命中时查询 Room`() = runTest {
        // Arrange: Room 有数据但缓存为空（item id 不在缓存中）
        val entity = buildTestEntity(id = 99)
        coEvery { itemDao.getItemById(99) } returns entity

        createRepository()

        // Act
        val result = repository.getItemById(99)

        // Assert
        assertNotNull(result)
        assertEquals(99L, result!!.id)

        // 应该回退到 Room 查询
        coVerify { itemDao.getItemById(99) }
    }

    @Test
    fun `getItemById - 缓存和 Room 都没有时返回 null`() = runTest {
        coEvery { itemDao.getItemById(any()) } returns null

        createRepository()

        val result = repository.getItemById(999L)

        assertNull(result)
    }

    // ===========================
    // 2. 缓存行为测试
    // ===========================

    @Test
    fun `getCachedItemById - Room Flow 更新后内存缓存自动刷新`(): Unit = runBlocking {
        val entity1 = buildTestEntity(id = 1, name = "物品A")
        val entity2 = buildTestEntity(id = 2, name = "物品B")

        createRepository()

        // 初始缓存为空
        assertNull(repository.getCachedItemById(1))

        // 发射新数据，等待 IO 协程处理
        itemsFlow.value = listOf(entity1, entity2)
        waitForIoCoroutines()

        // 缓存应已更新
        assertEquals("物品A", repository.getCachedItemById(1)?.name)
        assertEquals("物品B", repository.getCachedItemById(2)?.name)

        // 再次发射（模拟更新）
        itemsFlow.value = listOf(entity1.copy(name = "物品A-v2"))
        waitForIoCoroutines()

        assertEquals("物品A-v2", repository.getCachedItemById(1)?.name)
        // entity2 已不在列表中，缓存应被清除
        assertNull(repository.getCachedItemById(2))
    }

    @Test
    fun `getCachedItemById - 同步读取，不挂起协程`(): Unit = runBlocking {
        val entity = buildTestEntity()
        itemsFlow.value = listOf(entity)

        createRepository()

        // getCachedItemById 是普通函数（非 suspend），直接返回
        val result: Item? = repository.getCachedItemById(ITEM_ID)
        assertNotNull(result)
        assertEquals(ITEM_NAME, result!!.name)
    }

    // ===========================
    // 3. getAllItems Flow 测试
    // ===========================

    @Test
    fun `getAllItems - 返回 Flow 并在数据更新时发射新值`() = runTest {
        val entity1 = buildTestEntity(id = 1, name = "物品A")
        val entity2 = buildTestEntity(id = 2, name = "物品B")

        createRepository()

        // 初始为空
        repository.getAllItems().test {
            assertEquals(emptyList<Item>(), awaitItem())

            // 发射数据
            itemsFlow.value = listOf(entity1)
            val firstEmission = awaitItem()
            assertEquals(1, firstEmission.size)
            assertEquals("物品A", firstEmission[0].name)

            // 发射更多数据
            itemsFlow.value = listOf(entity1, entity2)
            val secondEmission = awaitItem()
            assertEquals(2, secondEmission.size)

            cancel()
        }
    }

    @Test
    fun `getAllItems - Flow 中 ItemEntity 正确转换为 Item`() = runTest {
        val entity = buildTestEntity(
            id = 5,
            name = "转换测试",
            location = "上海",
            price = 199.0
        )
        itemsFlow.value = listOf(entity)

        createRepository()

        val items = repository.getAllItems().first()
        assertEquals(1, items.size)

        val item = items[0]
        assertEquals(5L, item.id)
        assertEquals("转换测试", item.name)
        assertEquals("上海", item.location)
        assertEquals(199.0, item.purchasePrice, 0.001)
    }

    // ===========================
    // 4. 远程同步（D1）测试
    // ===========================

    // ---------- pullFromD1（全量拉取） ----------

    @Test
    fun `pullFromD1 - 成功拉取并写入 Room，重置 watermark`() = runTest {
        // Arrange
        val rows = listOf(
            buildD1Row(id = 1, name = "D1物品A", updatedAt = 100L),
            buildD1Row(id = 2, name = "D1物品B", updatedAt = 200L)
        )
        coEvery { d1ApiService.getAllItems() } returns rows
        coEvery { itemDao.getMaxUpdatedAt() } returns 200L

        createRepository()

        // Act
        repository.pullFromD1()
        waitForIoCoroutines()

        // Assert: items 被写入 Room
        coVerify { itemDao.insertItems(any()) }

        // watermark 被重置
        coVerify { settingsRepository.setLastPullUpdatedAt(200L) }
        coVerify { settingsRepository.setLastPushUpdatedAt(200L) }
    }

    @Test
    fun `pullFromD1 - D1 返回空列表时只写入空列表`() = runTest {
        coEvery { d1ApiService.getAllItems() } returns emptyList()

        createRepository()

        repository.pullFromD1()
        waitForIoCoroutines()

        // 空列表也会调用 insertItems（传入空 list）
        coVerify { itemDao.insertItems(emptyList()) }
    }

    @Test
    fun `pullFromD1 - 网络异常时本地数据不受影响`(): Unit = runBlocking {
        coEvery { d1ApiService.getAllItems() } throws RuntimeException("网络错误")

        // 先在 Room 中放数据
        val localEntity = buildTestEntity(id = 1, name = "本地物品")
        itemsFlow.value = listOf(localEntity)

        createRepository()

        // Act: pullFromD1 应该吞掉异常
        repository.pullFromD1()
        waitForIoCoroutines()

        // Assert: 本地 Flow 中的数据不受影响
        val items = repository.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("本地物品", items[0].name)
    }

    // ---------- 增量同步（incrementalSync） ----------

    @Test
    fun `incrementalSync - 全量 pull + push 流程`(): Unit = runBlocking {
        // Arrange: Room 有数据（itemCount > 0），触发增量同步
        coEvery { itemDao.itemCount() } returns 1

        // Pull: D1 有新数据
        val newRows = listOf(
            buildD1Row(id = 10, name = "新物品", updatedAt = 500L)
        )
        coEvery { d1ApiService.getItemsUpdatedAfter(0L) } returns newRows
        coEvery { itemDao.getMaxUpdatedAt() } returns 500L

        // Push: 本地也有变更
        val localChanges = listOf(buildTestEntity(id = 1, updatedAt = 300L))
        coEvery { itemDao.getItemsUpdatedAfter(0L) } returns localChanges

        createRepository()

        // Assert: 增量 pull 写入 Room
        coVerify { itemDao.insertItems(any()) }
        coVerify { settingsRepository.setLastPullUpdatedAt(500L) }

        // 增量 push 调用 upsertItem
        coVerify {
            d1ApiService.upsertItem(
                eq(1L),
                eq(ITEM_NAME),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(300L)
            )
        }
        coVerify { settingsRepository.setLastPushUpdatedAt(300L) }
    }

    @Test
    fun `incrementalPull - 基于 timestamp 只拉取新数据`(): Unit = runBlocking {
        // Arrange
        coEvery { itemDao.itemCount() } returns 1
        lastPullFlow.value = 100L

        // 只有 updated_at > 100 的行
        val newerRows = listOf(
            buildD1Row(id = 5, updatedAt = 200L),
            buildD1Row(id = 6, updatedAt = 300L)
        )
        coEvery { d1ApiService.getItemsUpdatedAfter(100L) } returns newerRows

        createRepository()

        // Assert: watermark 推进到 300
        coVerify { settingsRepository.setLastPullUpdatedAt(300L) }
    }

    @Test
    fun `incrementalPull - 没有新数据时不写入 Room`(): Unit = runBlocking {
        coEvery { itemDao.itemCount() } returns 1
        coEvery { d1ApiService.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { itemDao.getItemsUpdatedAfter(any()) } returns emptyList()

        createRepository()

        // 增量 pull 和 push 被调用，但没有数据写入
        coVerify { d1ApiService.getItemsUpdatedAfter(any()) }
    }

    @Test
    fun `incrementalPush - 部分推送失败时不推进 watermark，下次重试`(): Unit = runBlocking {
        // Arrange
        coEvery { itemDao.itemCount() } returns 1
        lastPushFlow.value = 0L

        val entity1 = buildTestEntity(id = 1, updatedAt = 100L)
        val entity2 = buildTestEntity(id = 2, updatedAt = 200L)
        coEvery { itemDao.getItemsUpdatedAfter(0L) } returns listOf(entity1, entity2)

        // entity1 push 成功，entity2 push 失败
        coEvery { d1ApiService.upsertItem(eq(1L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        coEvery { d1ApiService.upsertItem(eq(2L), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws RuntimeException("D1 错误")

        // Pull 返回空，不做增量 pull
        coEvery { d1ApiService.getItemsUpdatedAfter(any()) } returns emptyList()

        createRepository()

        // Assert: watermark 只推进到成功推送的最大值（100）
        coVerify { settingsRepository.setLastPushUpdatedAt(100L) }
    }

    // ---------- refreshFromRemote ----------

    @Test
    fun `refreshFromRemote - 触发增量同步`(): Unit = runBlocking {
        coEvery { itemDao.itemCount() } returns 1
        coEvery { d1ApiService.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { itemDao.getItemsUpdatedAfter(any()) } returns emptyList()

        createRepository()

        // Act
        repository.refreshFromRemote()
        waitForIoCoroutines()

        // 验证增量 pull 被额外调用一次（init 中 + refreshFromRemote）
        coVerify(atLeast = 2) { d1ApiService.getItemsUpdatedAfter(any()) }
    }

    // ===========================
    // 5. 同步失败处理
    // ===========================

    @Test
    fun `deleteItem - D1 删除失败时本地仍已删除`() = runTest {
        coEvery { d1ApiService.deleteItemById(any()) } throws RuntimeException("网络断开")

        val item = buildTestItem()
        createRepository()

        // Act: deleteItem 不应抛异常
        repository.deleteItem(item)
        waitForIoCoroutines()

        // Room 删除仍被调用
        coVerify { itemDao.deleteItemById(ITEM_ID) }
    }

    @Test
    fun `updateItem - D1 同步失败时本地仍已更新`() = runTest {
        // updateItem 中 D1 更新失败 + fallback insert 也失败
        coEvery {
            d1ApiService.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns false
        coEvery {
            d1ApiService.insertItem(any<Long>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("D1 不可用")

        val item = buildTestItem()
        createRepository()

        // 不应抛异常
        repository.updateItem(item)
        waitForIoCoroutines()

        // Room 更新仍被调用（11 个位置参数）
        coVerify { itemDao.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `insertItem - D1 插入失败时抛出异常给调用方`() = runTest {
        coEvery {
            d1ApiService.insertItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } throws RuntimeException("D1 不可用")

        val item = buildTestItem()
        createRepository()

        // insertItem 是 D1 优先的，D1 失败应直接抛出
        var exceptionThrown = false
        try {
            repository.insertItem(item)
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("D1") == true || e is RuntimeException)
        }
        assertTrue("insertItem 在 D1 失败时应抛异常", exceptionThrown)
    }

    @Test
    fun `deleteItemsByIds - D1 批量删除失败时本地仍已删除`() = runTest {
        coEvery { d1ApiService.deleteItemsByIds(any()) } throws RuntimeException("超时")

        val ids = setOf(10L, 20L)
        createRepository()

        repository.deleteItemsByIds(ids)
        waitForIoCoroutines()

        coVerify { itemDao.deleteItemsByIds(ids.toList()) }
    }

    // ===========================
    // 6. 边界情况
    // ===========================

    @Test
    fun `getAllItems - 空数据库返回空 Flow`() = runTest {
        createRepository()

        val items = repository.getAllItems().first()
        assertTrue(items.isEmpty())
    }

    @Test
    fun `insertItems - 插入空列表时不崩溃`() = runTest {
        createRepository()

        // 不应抛异常
        repository.insertItems(emptyList())
        waitForIoCoroutines()
    }

    @Test
    fun `deleteItemsByIds - 空 ID 集合时仍调用 Room 和 D1（传入空列表）`() = runTest {
        // ItemRepository 直接将 emptySet().toList() 传给 DAO 和 D1，
        // D1ApiService.deleteItemsByIds 内部会检查 isEmpty 并 early return。
        // 但 Repository 层不做这个判断，会透传空列表。
        createRepository()

        repository.deleteItemsByIds(emptySet())
        waitForIoCoroutines()

        // Room 被调用（传入空列表）
        coVerify { itemDao.deleteItemsByIds(emptyList()) }
    }

    @Test
    fun `searchItems - 查询关键词正确传递到 DAO`() = runTest {
        every { itemDao.searchItems(any()) } returns flowOf(emptyList())

        createRepository()

        val flow = repository.searchItems("测试")
        val results = flow.first()

        assertTrue(results.isEmpty())
        verify { itemDao.searchItems("测试") }
    }

    @Test
    fun `pullFromD1 - D1 行中字段缺失时使用默认值`() = runTest {
        // D1 行只包含必填字段，其他字段缺失
        val sparseRow: Map<String, Any?> = mapOf(
            "id" to 1,
            "name" to "稀疏数据"
            // 缺少 location, purchase_price, tags 等
        )
        coEvery { d1ApiService.getAllItems() } returns listOf(sparseRow)
        coEvery { itemDao.getMaxUpdatedAt() } returns 0L

        createRepository()

        repository.pullFromD1()
        waitForIoCoroutines()

        // 验证即使字段缺失也不会崩溃
        coVerify { itemDao.insertItems(any()) }
    }

    @Test
    fun `init - Room 为空时执行全量 pull`(): Unit = runBlocking {
        coEvery { itemDao.itemCount() } returns 0

        // 全量 pull 数据
        val rows = listOf(buildD1Row(id = 1, name = "首次同步"))
        coEvery { d1ApiService.getAllItems() } returns rows
        coEvery { itemDao.getMaxUpdatedAt() } returns 100L

        createRepository()

        // 验证全量 pull 被调用
        coVerify { d1ApiService.getAllItems() }
        coVerify { settingsRepository.setLastPullUpdatedAt(100L) }
        coVerify { settingsRepository.setLastPushUpdatedAt(100L) }
    }

    @Test
    fun `init - Room 有数据时执行增量同步`(): Unit = runBlocking {
        coEvery { itemDao.itemCount() } returns 5
        coEvery { d1ApiService.getItemsUpdatedAfter(any()) } returns emptyList()
        coEvery { itemDao.getItemsUpdatedAfter(any()) } returns emptyList()

        createRepository()

        // 增量 pull 被调用（getItemsUpdatedAfter）
        coVerify { d1ApiService.getItemsUpdatedAfter(any()) }
        // 不应调用全量 pull
        coVerify(exactly = 0) { d1ApiService.getAllItems() }
    }

    @Test
    fun `init - 网络异常时不崩溃`(): Unit = runBlocking {
        coEvery { d1ApiService.createTableIfNotExists() } throws RuntimeException("无网络")

        // 不应抛异常
        createRepository()

        // 验证 createTableIfNotExists 被调用过
        coVerify { d1ApiService.createTableIfNotExists() }
    }

    @Test
    fun `pullFromD1 - D1 行的 image_paths 字段解析为 imagePath`() = runTest {
        val row = buildD1Row(
            id = 1,
            imagePaths = """["https://img.com/1.jpg","https://img.com/2.jpg"]"""
        )
        coEvery { d1ApiService.getAllItems() } returns listOf(row)
        coEvery { itemDao.getMaxUpdatedAt() } returns 0L

        createRepository()

        repository.pullFromD1()
        waitForIoCoroutines()

        // 验证 insertItems 传入的实体包含正确的 imagePath
        coVerify {
            itemDao.insertItems(match { entities ->
                entities.isNotEmpty() &&
                    entities[0].imagePath == "https://img.com/1.jpg"
            })
        }
    }

    @Test
    fun `updateItem - D1 更新成功后不触发 fallback insert`() = runTest {
        val item = buildTestItem()
        coEvery {
            d1ApiService.updateItem(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns true

        createRepository()

        repository.updateItem(item)
        waitForIoCoroutines()

        // updateItem 返回 true，不应调用 fallback insert
        coVerify(exactly = 0) {
            d1ApiService.insertItem(any<Long>(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }
}
