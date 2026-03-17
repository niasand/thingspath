# ThingsPath - Android Item Management App

## Build Status
✅ **APK Build Successful** - `app/build/outputs/apk/debug/app-debug.apk` (17.1 MB)

## Update - 2026-03-12
**List / Grid 价格展示：**
- 在 list 和 grid 模式下的物品卡片增加价格标签（小字号、胶囊样式），仅当 `purchasePrice > 0` 时显示。

**Grid 信息对齐：**
- 修复 grid 模式下价格、时间与持有时间对齐问题（底部对齐）。

**默认图片占位统一：**
- 无图片时统一使用「淡蓝底 + 居中物品名称」作为占位图。
- 占位文字调整为更小字号，并统一为纯黑色 `#000000`。

**物品图片相册：**
- 用户选择的物品图片会复制并保存到系统相册 `Pictures/ThingsPath`（单独相册）。

**Usage Days 联动：**
- 详情页 usageDays 直接根据 purchaseDate 计算显示。
- 编辑时 purchaseDate 与 usageDays 双向联动：修改任一项会自动推导并更新另一项（基于当前日期计算）。

**分页功能（按 UI 图实现）：**
- 新增分页功能，默认每页 10 条，可选 10 / 20 / 50 条每页（保存到本地偏好）。
- 分页条样式按 UI 图：左侧“共 X 条”，中间页码按钮 + 省略号，右侧“10/页”下拉。
- 已处理：
  - 分页不再和右下角加号重叠：把分页条从内容区挪到 Scaffold 的 bottomBar，这样系统会自动把 FAB（加号）顶到分页条上方。
  - 分页整体调小：把高度从 36dp 降到 32dp，文字从 labelLarge 降到 labelMedium，整体 padding/间距也缩小了一档。
  - 加号按钮背景改为“玻璃感”半透明底色，避免遮挡列表文字（提高不透明度，避免底部出现白色透底）。

**验证：**
- `:app:assembleDebug` 通过。

## Update - 2026-03-03
**Search Functionality Improvements:**
- **Fixed:** Search functionality now correctly triggers on input change.
- **Fixed:** Search now supports multiple fields: `name`, `tags`, and `location`.
- **Fixed:** Implemented case-insensitive search for better user experience.
- **Optimization:** Added debounce (300ms) to search input to reduce database queries and improve performance.
- **UI:** Added a "Clear" button to the search bar for quick reset.

**Data Persistence & Backup:**
- **Feature:** Added automatic local backup to `/sdcard/Documents/ThingsPath/backup.json`.
- **Feature:** Added automatic data restoration from backup on fresh install.
- **Feature:** Implemented background hourly backup using WorkManager.
- **UX:** Improved import/export success messages to automatically dismiss after 1 second.

**AI Smart Add:**
- **Feature:** Added "AI Smart Add" mode to automatically extract item details from text.
- **Integration:** Integrated SiliconFlow API (DeepSeek-R1 model) for text analysis.
- **UI:** Added expandable FAB with "Manual Add" and "AI Smart Add" options.
- **UI:** Added Settings screen to configure API Key.
- **Workflow:** User pastes text -> AI analyzes -> Pre-fills Add Item form.

**App Icon Update:**
- **UI:** Updated app icon to a new anime-style design.

## Update - 2026-03-02
**Added Full Screen Image View:**
- **Feature:** Users can now click on the item image in the detail screen to view it in full screen.
- **Implementation:**
    - Added `FullScreenImageDialog` component using Compose `Dialog`.
    - Updated `ItemDetailState` to manage dialog visibility.
    - Updated `ItemDetailViewModel` to handle toggle actions.
- **Reason:** Enhanced user experience for viewing item details.

**Added Note and Tags:**
- **Feature:** Users can now add notes and tags to items.
- **Implementation:**
    - Updated `Item` model and Room entity with `note` and `tags` fields.
    - Added TypeConverters for list storage.
    - Updated `AddItemScreen` and `ItemDetailScreen` UI for input and display.
- **Reason:** Better organization and categorization of items.

**Added Price and Statistics:**
- **Feature:** Users can now add price to items and see total value/count on home screen.
- **Implementation:**
    - Updated `Item` model and Room entity with `purchasePrice` field.
    - Updated `AddItemScreen` and `ItemDetailScreen` to support price input/edit.
    - Added `StatisticsHeader` to `HomeScreen` to display total items and total value.
    - Added daily cost calculation in `ItemDetailScreen`.
- **Reason:** To track item value and cost of ownership.

## Latest Update - v1.2.0 - 2024-02-22
**Fixed Crash and Added Image Management:**
- Fixed crash when clicking on item details (added proper error handling and defaultValue in navigation)
- Added image picker functionality for adding and editing items
- Added image display in list and grid views
- Added image delete/replace functionality
- Images are stored in app cache and referenced by path
- Added Coil library for efficient image loading

## Features

### Image Management
- ✅ Add images when creating new items
- ✅ View images in item list and grid
- ✅ View full-size images in item detail page
- ✅ Replace images by selecting new ones
- ✅ Delete images from items
- ✅ Image placeholder when no image is set

### Item Management
- ✅ Add new items with name, location, purchase date, usage days, and image
- ✅ View items in list or grid layout
- ✅ Switch between list and grid views
- ✅ Search items by name
- ✅ View detailed item information
- ✅ Edit existing items
- ✅ Delete items with confirmation
- ✅ Persistent local storage using Room database

## Technology Stack
- **Kotlin** 1.9.22 - Primary development language
- **Jetpack Compose** 1.6.x - Modern UI toolkit
- **Room Database** 2.6.1 - Local persistence
- **Navigation Compose** 2.7.7 - Page navigation
- **Hilt/Dagger** 2.48.1 - Dependency injection
- **Coroutines** 1.7.3 - Asynchronous programming
- **Coil** 2.5.0 - Image loading
- **MVVM** - Architecture pattern

## Architecture
- MVVM architecture with clear separation of concerns
- Domain layer with use cases for business logic
- Data layer with Room database and repository pattern
- UI layer with Jetpack Compose and Material Design 3
- Dependency injection with Hilt

## Verification Checklist
- [x] App starts normally
- [x] Can add items (at minimum name)
- [x] Can add complete information items
- [x] Can add images to items
- [x] List/grid view toggle works
- [x] Click to enter detail page (without crash)
- [x] Detail page shows correct information (image, name, location, purchase date, usage days, add date)
- [x] Detail page can edit and save
- [x] Detail page can add/replace/delete images
- [x] Images display correctly in list, grid, and detail views
- [x] Delete confirmation works
- [x] Data persists (data remains after restart)
- [x] Date format is correct
- [x] Usage days calculation is accurate
