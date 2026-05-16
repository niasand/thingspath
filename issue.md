# Issue Log

## [2026-05-16] M3 SearchBar 组件内部 padding 导致布局空白

**现象：** 使用 Material 3 的 `SearchBar` 组件替换自定义搜索框后，搜索框与顶部栏之间出现一片无法去除的空白区域，占用布局空间。

**根因：** M3 `SearchBar` 组件内部自带固定 padding（用于展开态内容预留），即使设置 `expanded = false` 也不可消除。`SearchBarDefaults.InputField` 作为子组件同样继承此 padding。

**修复方案：** 放弃 M3 `SearchBar` 组件，改用 `OutlinedTextField` + 手动添加搜索图标和清除按钮，实现相同的视觉效果且完全可控间距。

**涉及文件：**
- `app/src/main/java/com/thingspath/ui/screen/home/HomeScreen.kt`

**经验总结：** M3 `SearchBar` 适用于需要展开/收起的搜索场景（如全屏搜索），不适合固定在布局中的内联搜索框。内联搜索框用 `OutlinedTextField` 更合适。
