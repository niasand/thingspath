package com.thingspath.ui.screen.home

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.local.repository.ItemRepository
import com.thingspath.data.model.Item
import com.thingspath.data.remote.model.AIExtractedItem
import com.thingspath.data.remote.repository.SiliconFlowRepository
import com.thingspath.domain.usecase.*
import com.thingspath.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.OutputStream

/**
 * HomeViewModel 全面单元测试
 *
 * 覆盖：初始化状态、加载 items、搜索过滤、排序切换、删除、导出、导入、AI 分析、错误处理
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // === Mock 依赖（在 @Before 中每次重新创建，避免测试间状态泄露）===
    private lateinit var getItemsUseCase: GetItemsUseCase
    private lateinit var addItemUseCase: AddItemUseCase
    private lateinit var deleteItemUseCase: DeleteItemUseCase
    private lateinit var exportItemsUseCase: ExportItemsUseCase
    private lateinit var importItemsUseCase: ImportItemsUseCase
    private lateinit var itemRepository: ItemRepository
    private lateinit var siliconFlowRepository: SiliconFlowRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var context: Context

    // Settings Flow — 默认值
    private val pageSizeFlow = MutableStateFlow(10)
    private val infiniteScrollFlow = MutableStateFlow(true)

    // Items Flow — 由 getItemsUseCase 返回
    private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())

    // 测试数据
    private val testItems = listOf(
        Item(
            id = 1,
            name = "MacBook Pro",
            location = "办公室",
            purchasePrice = 14999.0,
            purchaseDate = 1700000000000L,  // 2023-11-14
            usageDays = 30,
            tags = listOf("数码产品", "电脑办公"),
            createdAt = 1700000000000L,
            updatedAt = 1700100000000L
        ),
        Item(
            id = 2,
            name = "AirPods Pro",
            location = "家里",
            purchasePrice = 1899.0,
            purchaseDate = 1701000000000L,
            usageDays = 10,
            tags = listOf("数码产品"),
            note = "降噪很好",
            createdAt = 1701000000000L,
            updatedAt = 1701100000000L
        ),
        Item(
            id = 3,
            name = "Herman Miller 椅子",
            location = "办公室",
            purchasePrice = 9999.0,
            purchaseDate = 1699000000000L,
            tags = listOf("家具家居"),
            createdAt = 1699000000000L,
            updatedAt = 1699100000000L
        )
    )

    @Before
    fun setup() {
        // Mock android.util.Log，避免在纯 JVM 测试中 crash
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0

        // 每次测试重新创建 mock，避免测试间状态泄露
        getItemsUseCase = mockk(relaxed = true)
        addItemUseCase = mockk(relaxed = true)
        deleteItemUseCase = mockk(relaxed = true)
        exportItemsUseCase = mockk(relaxed = true)
        importItemsUseCase = mockk(relaxed = true)
        itemRepository = mockk(relaxed = true)
        siliconFlowRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // 重置 Flow 到默认值
        pageSizeFlow.value = 10
        infiniteScrollFlow.value = true
        itemsFlow.value = emptyList()

        // Settings 默认返回
        every { settingsRepository.pageSize } returns pageSizeFlow
        every { settingsRepository.infiniteScroll } returns infiniteScrollFlow

        // getItemsUseCase 返回 itemsFlow
        every { getItemsUseCase() } returns itemsFlow

        // addItemUseCase 默认返回 id
        coEvery { addItemUseCase(any()) } returns 1L
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建 ViewModel 并等待初始化完成（loadItems + observeSettings）
     */
    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            getItemsUseCase = getItemsUseCase,
            addItemUseCase = addItemUseCase,
            deleteItemUseCase = deleteItemUseCase,
            exportItemsUseCase = exportItemsUseCase,
            importItemsUseCase = importItemsUseCase,
            itemRepository = itemRepository,
            siliconFlowRepository = siliconFlowRepository,
            settingsRepository = settingsRepository,
            context = context
        )
    }

    /**
     * 等待异步操作完成（主要用于 analyzeText 等使用 Dispatchers.IO 的方法）
     * 因为 withContext(Dispatchers.IO) 在测试调度器外执行，advanceUntilIdle 无法等待
     */
    private fun waitForAsyncCompletion(viewModel: HomeViewModel, timeoutMs: Long = 2000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (viewModel.state.value.isAIProcessing && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
    }

    // ==================== 1. 初始化状态 ====================

    @Test
    fun `初始化状态 - 默认 HomeState 正确`() = runTest {
        // Given: 空数据
        itemsFlow.value = emptyList()

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: 初始状态的默认值
        val state = viewModel.state.value
        assertFalse("isLoading 应为 false", state.listState.isLoading)
        assertTrue("items 应为空列表", state.listState.items.isEmpty())
        assertEquals("searchQuery 应为空字符串", "", state.filterState.searchQuery)
        assertEquals("sortField 默认为 UpdatedAt", HomeSortField.UpdatedAt, state.filterState.sortField)
        assertFalse("sortAscending 默认为 false", state.filterState.sortAscending)
        assertEquals("pageSize 默认为 10", 10, state.pageSize)
        assertEquals("currentPage 默认为 0", 0, state.listState.currentPage)
        assertFalse("isExporting 应为 false", state.isExporting)
        assertFalse("isImporting 应为 false", state.isImporting)
        assertNull("errorMessage 应为 null", state.errorMessage)
        assertFalse("isAIProcessing 应为 false", state.isAIProcessing)
        assertFalse("isSelectionMode 应为 false", state.isSelectionMode)
        assertTrue("selectedItemIds 应为空", state.selectedItemIds.isEmpty())
        assertTrue("selectedTags 应为空", state.filterState.selectedTags.isEmpty())
        assertTrue("infiniteScroll 默认为 true", state.infiniteScroll)
    }

    // ==================== 2. 加载 items ====================

    @Test
    fun `加载 items - 从 getItemsUseCase 获取数据后更新 state`() = runTest {
        // Given
        itemsFlow.value = testItems

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.state.value
        assertEquals("items 数量应匹配", 3, state.listState.items.size)
        assertFalse("加载完成后 isLoading 应为 false", state.listState.isLoading)
        assertEquals("totalItemCount 应等于 3", 3, state.listState.totalItemCount)
    }

    @Test
    fun `加载 items - totalPrice 正确计算`() = runTest {
        // Given
        itemsFlow.value = testItems

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: 14999 + 1899 + 9999 = 26897.0
        assertEquals(26897.0, state(viewModel).listState.totalPrice, 0.01)
    }

    @Test
    fun `加载 items - allTags 正确提取和排序`() = runTest {
        // Given
        itemsFlow.value = testItems

        // When
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Then: tags 应去重并排序
        val tags = state(viewModel).filterState.allTags
        assertTrue("应包含 '数码产品'", tags.contains("数码产品"))
        assertTrue("应包含 '电脑办公'", tags.contains("电脑办公"))
        assertTrue("应包含 '家具家居'", tags.contains("家具家居"))
        // 验证排序（按 Unicode 代码点排序）
        assertEquals(listOf("家具家居", "数码产品", "电脑办公"), tags)
    }

    @Test
    fun `加载 items - 数据变化时自动刷新`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue("初始 items 为空", state(viewModel).listState.items.isEmpty())

        // When: 数据源更新
        itemsFlow.value = testItems
        advanceUntilIdle()

        // Then
        assertEquals("items 应更新为 3 条", 3, state(viewModel).listState.items.size)
    }

    // ==================== 3. 搜索过滤 ====================

    @Test
    fun `搜索过滤 - 按名称过滤`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("初始应有 3 条", 3, state(viewModel).listState.items.size)

        // When: 搜索 "MacBook"
        viewModel.onSearchQueryChange("MacBook")
        advanceUntilIdle()

        // Then: 只有 MacBook Pro 匹配
        val state = state(viewModel)
        assertEquals("应只匹配 1 条", 1, state.listState.items.size)
        assertEquals("MacBook Pro", state.listState.items[0].name)
    }

    @Test
    fun `搜索过滤 - 按 note 过滤`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 搜索 note 中的内容
        viewModel.onSearchQueryChange("降噪")
        advanceUntilIdle()

        // Then: 只有 AirPods Pro 的 note 包含"降噪"
        val state = state(viewModel)
        assertEquals(1, state.listState.items.size)
        assertEquals("AirPods Pro", state.listState.items[0].name)
    }

    @Test
    fun `搜索过滤 - 按 location 过滤`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 搜索 location "办公室"
        viewModel.onSearchQueryChange("办公室")
        advanceUntilIdle()

        // Then: MacBook Pro 和 Herman Miller 在办公室
        assertEquals(2, state(viewModel).listState.items.size)
    }

    @Test
    fun `搜索过滤 - 清空搜索词恢复全部`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 先搜索
        viewModel.onSearchQueryChange("MacBook")
        advanceUntilIdle()
        assertEquals(1, state(viewModel).listState.items.size)

        // When: 清空搜索词
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        // Then: 恢复全部
        assertEquals(3, state(viewModel).listState.items.size)
    }

    @Test
    fun `搜索过滤 - 大小写不敏感`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.onSearchQueryChange("macbook")
        advanceUntilIdle()

        // Then
        assertEquals(1, state(viewModel).listState.items.size)
    }

    // ==================== 4. 排序切换 ====================

    @Test
    fun `排序 - 按 PurchaseDate 升序`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 第一次切换到 PurchaseDate → 降序（默认），再切一次 → 升序
        viewModel.selectSort(HomeSortField.PurchaseDate) // 切到 PurchaseDate, ascending=false
        advanceUntilIdle()
        viewModel.selectSort(HomeSortField.PurchaseDate) // 同字段再切, ascending=true
        advanceUntilIdle()

        // Then: 升序 → 最早的在前
        val items = state(viewModel).listState.items
        // 1699000000000(Herman) < 1700000000000(MacBook) < 1701000000000(AirPods)
        assertEquals("Herman Miller 椅子", items[0].name)
        assertEquals("MacBook Pro", items[1].name)
        assertEquals("AirPods Pro", items[2].name)
    }

    @Test
    fun `排序 - 按 PurchaseDate 降序（默认方向）`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 切换到 PurchaseDate（默认降序）
        viewModel.selectSort(HomeSortField.PurchaseDate)
        advanceUntilIdle()

        val items = state(viewModel).listState.items
        // 降序 → 最新的在前
        assertEquals("AirPods Pro", items[0].name)
        assertEquals("MacBook Pro", items[1].name)
        assertEquals("Herman Miller 椅子", items[2].name)
    }

    @Test
    fun `排序 - 按 Name 升序`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.selectSort(HomeSortField.Name)
        advanceUntilIdle()
        viewModel.selectSort(HomeSortField.Name) // 再切一次变为升序
        advanceUntilIdle()

        val items = state(viewModel).listState.items
        // 字母序：AirPods Pro → Herman Miller 椅子 → MacBook Pro
        assertEquals("AirPods Pro", items[0].name)
        assertEquals("Herman Miller 椅子", items[1].name)
        assertEquals("MacBook Pro", items[2].name)
    }

    @Test
    fun `排序 - 同字段再次点击切换升降序`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 默认 UpdatedAt, ascending=false
        assertFalse("初始 sortAscending=false", state(viewModel).filterState.sortAscending)

        // 切到 Name（首次，默认降序）
        viewModel.selectSort(HomeSortField.Name)
        advanceUntilIdle()
        assertEquals(HomeSortField.Name, state(viewModel).filterState.sortField)
        assertFalse("首次切到新字段应为降序", state(viewModel).filterState.sortAscending)

        // 再次点击 Name → 升序
        viewModel.selectSort(HomeSortField.Name)
        advanceUntilIdle()
        assertTrue("再次点击同字段应切换为升序", state(viewModel).filterState.sortAscending)
    }

    // ==================== 5. 删除 item ====================

    @Test
    fun `单个删除 - confirmDelete 调用 deleteItemUseCase 并关闭对话框`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        val itemToDelete = testItems[0]
        coEvery { deleteItemUseCase(any<Item>()) } just Runs

        // When: 显示删除对话框 → 确认删除
        viewModel.showDeleteDialog(itemToDelete)
        assertEquals("对话框应显示", true, state(viewModel).showDeleteDialog)
        assertEquals("待删除 item 应设置", itemToDelete, state(viewModel).itemToDelete)

        viewModel.confirmDelete()
        advanceUntilIdle()

        // Then
        coVerify { deleteItemUseCase(itemToDelete) }
        assertFalse("对话框应关闭", state(viewModel).showDeleteDialog)
        assertNull("itemToDelete 应清空", state(viewModel).itemToDelete)
    }

    @Test
    fun `批量删除 - 选择模式下删除选中 items`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 进入选择模式
        viewModel.toggleSelectionMode()
        assertTrue(state(viewModel).isSelectionMode)

        // 选择两个 item
        viewModel.toggleItemSelection(1L)
        viewModel.toggleItemSelection(2L)
        assertEquals(setOf(1L, 2L), state(viewModel).selectedItemIds)

        coEvery { deleteItemUseCase(any<Set<Long>>()) } just Runs

        // When: 显示删除对话框 → 确认
        viewModel.showDeleteDialog(testItems[0])
        viewModel.confirmDelete()
        advanceUntilIdle()

        // Then
        coVerify { deleteItemUseCase(setOf(1L, 2L)) }
        assertFalse("选择模式应退出", state(viewModel).isSelectionMode)
        assertTrue("选中应清空", state(viewModel).selectedItemIds.isEmpty())
    }

    @Test
    fun `批量删除 - deleteSelectedItems 无选中时直接返回`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 未选择任何 item
        assertTrue(state(viewModel).selectedItemIds.isEmpty())

        // When
        viewModel.deleteSelectedItems()
        advanceUntilIdle()

        // Then: 不应调用 deleteItemUseCase
        coVerify(exactly = 0) { deleteItemUseCase(any<Set<Long>>()) }
    }

    @Test
    fun `批量删除失败 - 设置 errorMessage`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelectionMode()
        viewModel.toggleItemSelection(1L)

        coEvery { deleteItemUseCase(any<Set<Long>>()) } throws RuntimeException("DB error")

        // When
        viewModel.deleteSelectedItems()
        advanceUntilIdle()

        // Then
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
        assertTrue("errorMessage 应包含失败信息",
            state(viewModel).errorMessage!!.contains("批量删除失败"))
    }

    // ==================== 6. 导出 items ====================

    @Test
    fun `导出 - exportData 调用 exportItemsUseCase 并写入 Uri`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        val outputStream = mockk<OutputStream>(relaxed = true)
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns outputStream
        coEvery { exportItemsUseCase() } returns """[{"name":"test"}]"""

        // When
        viewModel.exportData(uri)
        advanceUntilIdle()

        // Then
        coVerify { exportItemsUseCase() }
        verify { contentResolver.openOutputStream(uri) }
        verify { outputStream.write(any<ByteArray>()) }
        verify { outputStream.close() }
    }

    @Test
    fun `导出失败 - 设置 errorMessage`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openOutputStream(uri) } returns null // 模拟失败
        coEvery { exportItemsUseCase() } returns "{}"

        // When
        viewModel.exportData(uri)
        advanceUntilIdle()

        // Then
        assertFalse("isExporting 应为 false", state(viewModel).isExporting)
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
    }

    // ==================== 7. 导入 items ====================

    @Test
    fun `导入 - importData 调用 importItemsUseCase`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        val jsonString = """[{"name":"test","purchasePrice":100}]"""
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        val inputStream = jsonString.byteInputStream()
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns inputStream
        coEvery { importItemsUseCase(any()) } just Runs

        // When
        viewModel.importData(uri)
        advanceUntilIdle()

        // Then
        coVerify { importItemsUseCase(jsonString) }
    }

    @Test
    fun `导入失败 - 无法读取输入流时设置 errorMessage`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns null

        // When
        viewModel.importData(uri)
        advanceUntilIdle()

        // Then
        assertFalse("isImporting 应为 false", state(viewModel).isImporting)
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
    }

    // ==================== 8. AI 分析文本 ====================

    @Test
    fun `AI 分析 - analyzeText 成功调用 siliconFlowRepository 并添加 items`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val extractedItems = listOf(
            AIExtractedItem(
                name = "机械键盘",
                price = 599.0,
                date = "2024-01-15",
                location = "京东",
                tags = listOf("电脑办公"),
                note = "Cherry 轴"
            )
        )
        coEvery { siliconFlowRepository.analyzeText("买了个机械键盘") } returns extractedItems
        coEvery { addItemUseCase(any()) } returns 1L

        // When
        viewModel.analyzeText("买了个机械键盘")
        waitForAsyncCompletion(viewModel)

        // Then
        coVerify { siliconFlowRepository.analyzeText("买了个机械键盘") }
        coVerify { addItemUseCase(match { it.name == "机械键盘" }) }
        assertFalse("isAIProcessing 应为 false", state(viewModel).isAIProcessing)
    }

    @Test
    fun `AI 分析 - 提取多个物品时全部添加`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val extractedItems = listOf(
            AIExtractedItem(name = "键盘", price = 500.0, date = null, location = null, tags = null, note = null),
            AIExtractedItem(name = "鼠标", price = 200.0, date = null, location = null, tags = null, note = null)
        )
        coEvery { siliconFlowRepository.analyzeText("键盘和鼠标") } returns extractedItems
        coEvery { addItemUseCase(any()) } returns 1L

        // When
        viewModel.analyzeText("键盘和鼠标")
        waitForAsyncCompletion(viewModel)

        // Then: 应调用 addItemUseCase 两次
        coVerify(exactly = 2) { addItemUseCase(any()) }
    }

    @Test
    fun `AI 分析 - 过滤无效名称的物品`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val extractedItems = listOf(
            AIExtractedItem(name = "有效物品", price = 100.0, date = null, location = null, tags = null, note = null),
            AIExtractedItem(name = "", price = null, date = null, location = null, tags = null, note = null),
            AIExtractedItem(name = "   ", price = null, date = null, location = null, tags = null, note = null),
            AIExtractedItem(name = null, price = null, date = null, location = null, tags = null, note = null)
        )
        coEvery { siliconFlowRepository.analyzeText("测试") } returns extractedItems
        coEvery { addItemUseCase(any()) } returns 1L

        // When
        viewModel.analyzeText("测试")
        waitForAsyncCompletion(viewModel)

        // Then: 只添加有效名称的物品
        coVerify(exactly = 1) { addItemUseCase(match { it.name == "有效物品" }) }
    }

    @Test
    fun `AI 分析 - 所有物品名称无效时报错`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        val extractedItems = listOf(
            AIExtractedItem(name = null, price = null, date = null, location = null, tags = null, note = null),
            AIExtractedItem(name = "", price = null, date = null, location = null, tags = null, note = null)
        )
        coEvery { siliconFlowRepository.analyzeText("无效文本") } returns extractedItems

        // When
        viewModel.analyzeText("无效文本")
        waitForAsyncCompletion(viewModel)

        // Then
        assertFalse("isAIProcessing 应为 false", state(viewModel).isAIProcessing)
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
        assertTrue("errorMessage 应包含无法识别",
            state(viewModel).errorMessage!!.contains("无法识别"))
        coVerify(exactly = 0) { addItemUseCase(any()) }
    }

    @Test
    fun `AI 分析 - siliconFlowRepository 抛异常时设置 errorMessage`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { siliconFlowRepository.analyzeText(any()) } throws RuntimeException("API 限流")

        // When
        viewModel.analyzeText("测试")
        waitForAsyncCompletion(viewModel)

        // Then
        assertFalse("isAIProcessing 应为 false", state(viewModel).isAIProcessing)
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
        assertTrue("errorMessage 应包含错误信息",
            state(viewModel).errorMessage!!.contains("AI 分析失败"))
    }

    @Test
    fun `AI 分析 - 超时时设置超时错误信息`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 模拟超时：ViewModel 中 IOException("Canceled") 会被当作超时处理
        coEvery { siliconFlowRepository.analyzeText(any()) } throws
            java.io.IOException("Canceled")

        // When
        viewModel.analyzeText("测试超时")
        waitForAsyncCompletion(viewModel)

        // Then
        assertFalse("isAIProcessing 应为 false", state(viewModel).isAIProcessing)
        assertNotNull("应设置 errorMessage", state(viewModel).errorMessage)
        assertTrue("errorMessage 应包含超时信息",
            state(viewModel).errorMessage!!.contains("超时"))
    }

    // ==================== 9. Tag 过滤 ====================

    @Test
    fun `Tag 过滤 - 选中 tag 后只显示匹配 items`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 选中 "电脑办公" tag
        viewModel.toggleTag("电脑办公")
        advanceUntilIdle()

        // Then: 只有 MacBook Pro 有 "电脑办公" tag
        val state = state(viewModel)
        assertTrue("selectedTags 应包含 '电脑办公'", state.filterState.selectedTags.contains("电脑办公"))
        assertEquals(1, state.listState.items.size)
        assertEquals("MacBook Pro", state.listState.items[0].name)
    }

    @Test
    fun `Tag 过滤 - 取消选中 tag 恢复全部`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 选中再取消
        viewModel.toggleTag("数码产品")
        advanceUntilIdle()
        assertEquals(2, state(viewModel).listState.items.size) // MacBook + AirPods

        viewModel.toggleTag("数码产品") // 再次 toggle 取消
        advanceUntilIdle()

        // Then: 恢复全部
        assertEquals(3, state(viewModel).listState.items.size)
    }

    // ==================== 10. 选择模式 ====================

    @Test
    fun `选择模式 - toggleSelectionMode 切换`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse("初始非选择模式", state(viewModel).isSelectionMode)

        // When: 进入选择模式
        viewModel.toggleSelectionMode()
        assertTrue("应为选择模式", state(viewModel).isSelectionMode)

        // When: 再次 toggle 退出
        viewModel.toggleSelectionMode()
        assertFalse("应退出选择模式", state(viewModel).isSelectionMode)
    }

    @Test
    fun `选择模式 - toggleItemSelection 添加和移除选中`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 选中 item 1
        viewModel.toggleItemSelection(1L)
        assertEquals(setOf(1L), state(viewModel).selectedItemIds)

        // 选中 item 2
        viewModel.toggleItemSelection(2L)
        assertEquals(setOf(1L, 2L), state(viewModel).selectedItemIds)

        // 取消选中 item 1
        viewModel.toggleItemSelection(1L)
        assertEquals(setOf(2L), state(viewModel).selectedItemIds)
    }

    @Test
    fun `选择模式 - selectAll 选中全部 items`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelectionMode()

        // When
        viewModel.selectAll()

        // Then
        assertEquals(setOf(1L, 2L, 3L), state(viewModel).selectedItemIds)
    }

    @Test
    fun `选择模式 - 退出选择模式时清空选中`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleSelectionMode()
        viewModel.toggleItemSelection(1L)
        viewModel.toggleItemSelection(2L)
        assertEquals(2, state(viewModel).selectedItemIds.size)

        // When: 退出选择模式
        viewModel.toggleSelectionMode()

        // Then
        assertFalse("应退出选择模式", state(viewModel).isSelectionMode)
        assertTrue("选中应清空", state(viewModel).selectedItemIds.isEmpty())
    }

    // ==================== 11. 对话框 ====================

    @Test
    fun `对话框 - showDeleteDialog 设置状态`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        val item = testItems[0]

        // When
        viewModel.showDeleteDialog(item)

        // Then
        assertTrue("showDeleteDialog 应为 true", state(viewModel).showDeleteDialog)
        assertEquals(item, state(viewModel).itemToDelete)
    }

    @Test
    fun `对话框 - dismissDeleteDialog 关闭并清空`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showDeleteDialog(testItems[0])
        assertTrue(state(viewModel).showDeleteDialog)

        // When
        viewModel.dismissDeleteDialog()

        // Then
        assertFalse("showDeleteDialog 应为 false", state(viewModel).showDeleteDialog)
        assertNull("itemToDelete 应为 null", state(viewModel).itemToDelete)
    }

    // ==================== 12. dismissMessage ====================

    @Test
    fun `dismissMessage - 清空所有消息状态`() = runTest {
        // Given
        itemsFlow.value = emptyList()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 模拟设置一些消息状态（通过 state update 不能直接设，通过操作触发）
        // 用 import 失败来设置 errorMessage
        val uri = mockk<Uri>()
        val contentResolver = mockk<ContentResolver>(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { contentResolver.openInputStream(uri) } returns null
        viewModel.importData(uri)
        advanceUntilIdle()
        assertNotNull("应有 errorMessage", state(viewModel).errorMessage)

        // When
        viewModel.dismissMessage()

        // Then
        assertFalse("exportSuccess 应为 false", state(viewModel).exportSuccess)
        assertFalse("importSuccess 应为 false", state(viewModel).importSuccess)
        assertNull("errorMessage 应为 null", state(viewModel).errorMessage)
        assertNull("infoMessage 应为 null", state(viewModel).infoMessage)
    }

    // ==================== 13. 分页 ====================

    @Test
    fun `分页 - pageCount 正确计算`() = runTest {
        // Given: 3 items, pageSize=10
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 3 items / 10 per page = 1 page
        assertEquals(1, state(viewModel).listState.pageCount)
        assertEquals(0, state(viewModel).listState.currentPage)
    }

    @Test
    fun `分页 - pageSize 变化时重新计算 pageCount`() = runTest {
        // Given: 创建更多 items 使分页有意义
        val manyItems = (1..25).map { i ->
            Item(id = i.toLong(), name = "Item $i", createdAt = 1700000000000L + i, updatedAt = 1700000000000L + i)
        }
        itemsFlow.value = manyItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 25 items / 10 = 3 pages
        assertEquals(3, state(viewModel).listState.pageCount)

        // When: pageSize 变为 20
        pageSizeFlow.value = 20
        advanceUntilIdle()

        // 25 items / 20 = 2 pages
        assertEquals(2, state(viewModel).listState.pageCount)
    }

    @Test
    fun `分页 - goToNextPage 和 goToPreviousPage`() = runTest {
        // Given: 25 items, pageSize=10 → 3 pages
        val manyItems = (1..25).map { i ->
            Item(id = i.toLong(), name = "Item $i", createdAt = 1700000000000L + i, updatedAt = 1700000000000L + i)
        }
        itemsFlow.value = manyItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(3, state(viewModel).listState.pageCount)
        assertEquals(0, state(viewModel).listState.currentPage)

        // When: 下一页
        viewModel.goToNextPage()
        assertEquals(1, state(viewModel).listState.currentPage)

        viewModel.goToNextPage()
        assertEquals(2, state(viewModel).listState.currentPage)

        // 边界：已到最后一页，不应超过
        viewModel.goToNextPage()
        assertEquals(2, state(viewModel).listState.currentPage)

        // When: 上一页
        viewModel.goToPreviousPage()
        assertEquals(1, state(viewModel).listState.currentPage)

        viewModel.goToPreviousPage()
        assertEquals(0, state(viewModel).listState.currentPage)

        // 边界：已到第一页，不应小于 0
        viewModel.goToPreviousPage()
        assertEquals(0, state(viewModel).listState.currentPage)
    }

    @Test
    fun `分页 - goToPage 直接跳页`() = runTest {
        // Given
        val manyItems = (1..25).map { i ->
            Item(id = i.toLong(), name = "Item $i", createdAt = 1700000000000L + i, updatedAt = 1700000000000L + i)
        }
        itemsFlow.value = manyItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // When: 跳到第 2 页
        viewModel.goToPage(1)
        assertEquals(1, state(viewModel).listState.currentPage)

        // 边界：超过 pageCount 应 clamp
        viewModel.goToPage(100)
        assertEquals(2, state(viewModel).listState.currentPage) // 最大 pageIndex = 2

        // 负数也应 clamp
        viewModel.goToPage(-1)
        assertEquals(0, state(viewModel).listState.currentPage)
    }

    @Test
    fun `分页 - 搜索时 currentPage 重置为 0`() = runTest {
        // Given
        val manyItems = (1..25).map { i ->
            Item(id = i.toLong(), name = "Item $i", createdAt = 1700000000000L + i, updatedAt = 1700000000000L + i)
        }
        itemsFlow.value = manyItems
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 跳到第 2 页
        viewModel.goToPage(2)
        assertEquals(2, state(viewModel).listState.currentPage)

        // When: 搜索
        viewModel.onSearchQueryChange("Item 1")
        advanceUntilIdle()

        // Then: currentPage 应重置为 0
        assertEquals(0, state(viewModel).listState.currentPage)
    }

    // ==================== 14. 下拉刷新 ====================

    @Test
    fun `下拉刷新 - refreshData 调用 itemRepository refreshFromRemote`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { itemRepository.refreshFromRemote() } just Runs

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then
        coVerify { itemRepository.refreshFromRemote() }
        assertFalse("isRefreshing 最终应为 false", state(viewModel).isRefreshing)
    }

    @Test
    fun `下拉刷新 - refreshFromRemote 抛异常也不崩溃`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { itemRepository.refreshFromRemote() } throws RuntimeException("网络错误")

        // When
        viewModel.refreshData()
        advanceUntilIdle()

        // Then: 不崩溃，isRefreshing 最终恢复 false
        assertFalse("isRefreshing 应恢复为 false", state(viewModel).isRefreshing)
    }

    // ==================== 15. 设置 observeSettings ====================

    @Test
    fun `observeSettings - infiniteScroll 变化更新 state`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue("默认 infiniteScroll=true", state(viewModel).infiniteScroll)

        // When
        infiniteScrollFlow.value = false
        advanceUntilIdle()

        // Then
        assertFalse("infiniteScroll 应更新为 false", state(viewModel).infiniteScroll)
    }

    // ==================== 16. setPageSize ====================

    @Test
    fun `setPageSize - 调用 settingsRepository savePageSize`() = runTest {
        // Given
        itemsFlow.value = testItems
        val viewModel = createViewModel()
        advanceUntilIdle()
        coEvery { settingsRepository.savePageSize(any()) } just Runs

        // When
        viewModel.setPageSize(20)
        advanceUntilIdle()

        // Then
        coVerify { settingsRepository.savePageSize(20) }
    }

    // ==================== 辅助 ====================

    private fun state(viewModel: HomeViewModel) = viewModel.state.value
}
