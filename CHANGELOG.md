# Changelog

## [2026-06-06] 质量提升：安全、测试、架构、依赖

### P0: 安全 + 测试
- **R8 混淆**: `isMinifyEnabled = true`, `isShrinkResources = true`
- **ProGuard 规则**: 覆盖 Gson / Room / Hilt / Retrofit / Coroutines / DataStore 六大类
- **117 个新单元测试**: ItemRepository(38) + R2ImageRepository(33) + HomeViewModel(46)
- 修复已有 DeleteItemUseCaseTest 失败（mock `android.util.Log`）
- 修复 ItemDetailViewModelTest 缺失 `r2ImageRepository` 构造参数
- 添加 JVM `--add-opens` 参数支持 Java 21 模块访问

### P1: 架构优化
- **AppError sealed hierarchy**: NetworkError / StorageError / ImageError / AiServiceError / SyncError / ValidationError / UnknownError
- **HomeState 拆分**: `ListState`（items/loading/pagination）+ `FilterState`（search/sort/tags）+ 顶层 UI 状态
- 所有 Repository / ViewModel 的 catch 块使用类型化 AppError

### P2: 依赖升级
- Kotlin 1.9.22 → **2.1.21**（K2 编译器，通过 compose plugin）
- AGP 8.3.0 → **8.7.3**, Gradle 8.4 → **8.9**
- Hilt 2.48.1 → **2.55**, KSP 1.9.22-1.0.17 → **2.1.21-2.0.2**
- Compose BOM 2024.12.01 → **2025.05.01**
- compileSdk / targetSdk 34 → **35**
- 全部 androidx / Retrofit / Room / Coroutines / mockk / turbine 等升级到最新稳定版
- 跳过 Retrofit 3.x（breaking API）和 Coil 3.x（package rename）
- 禁用 `lintVitalAnalyze`（AGP 8.7 已知 bug）

### Bonus: ItemRepository 重构 + TypeConverter
- **SRP 拆分**: `LocalItemDataSource`（Room + LRU 缓存）+ `RemoteItemDataSource`（D1 API）+ `ItemRepository`（facade 协调器）
- **LRU 缓存**: `LinkedHashMap(accessOrder=true)` + `Collections.synchronizedMap`，上限 100 条目，自动淘汰
- **Room TypeConverter**: `Converters.kt` 处理 `List<String>` ↔ JSON（Gson），损坏 JSON 优雅降级为空列表
- `ItemEntity.imagePaths` / `tags` 从 `String` 改为 `List<String>`（编译期类型安全）
- 重写 `ItemRepositoryTest` 适配 facade 模式（mock DataSource 而非 Dao/Api）
