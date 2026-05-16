# Issue Log

## [2026-05-16] M3 SearchBar 组件内部 padding 导致布局空白

**现象：** 使用 Material 3 的 `SearchBar` 组件替换自定义搜索框后，搜索框与顶部栏之间出现一片无法去除的空白区域，占用布局空间。

**根因：** M3 `SearchBar` 组件内部自带固定 padding（用于展开态内容预留），即使设置 `expanded = false` 也不可消除。`SearchBarDefaults.InputField` 作为子组件同样继承此 padding。

**修复方案：** 放弃 M3 `SearchBar` 组件，改用 `OutlinedTextField` + 手动添加搜索图标和清除按钮，实现相同的视觉效果且完全可控间距。

**涉及文件：**
- `app/src/main/java/com/thingspath/ui/screen/home/HomeScreen.kt`

**经验总结：** M3 `SearchBar` 适用于需要展开/收起的搜索场景（如全屏搜索），不适合固定在布局中的内联搜索框。内联搜索框用 `OutlinedTextField` 更合适。

## [2026-05-16] 首次安装点击 TopAppBar Logo 区域导致白屏

**现象：** 首次安装后打开 app，点击左上角 TP Logo 区域，整个屏幕变白（包括 TopAppBar 消失）。仅第一次点击出现，后续点击正常。

**根因：** `Scaffold` 使用了 `nestedScroll(scrollBehavior.nestedScrollConnection)`，TopAppBar 的 `title` 区域触摸事件未被消费，穿透到 `nestedScroll` 连接器，触发了异常的滚动/手势状态，导致 Scaffold 渲染异常。

**修复方案：** 用 `Box(modifier = Modifier.clickable { })` 包裹 Logo，消费触摸事件，阻止穿透到 `nestedScroll`。

**涉及文件：**
- `app/src/main/java/com/thingspath/ui/screen/home/HomeScreen.kt`

**经验总结：** 当 Scaffold 使用 `nestedScroll` + `pinnedScrollBehavior` 时，TopAppBar 的 title 区域需要用 `clickable` 或其他触摸消费修饰符包裹，否则触摸事件可能穿透导致渲染异常。
