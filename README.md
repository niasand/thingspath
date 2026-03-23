# ThingsPath

一款 Android 家庭物品管理 App，帮助你记录家中所有物品的存放位置、购买信息和使用状态，告别"东西放哪了？""买了多久了？"的困扰。

---

## 功能特性

### 物品管理
- 添加物品：名称、存放位置、购买日期、价格、使用天数、备注、标签、多张图片
- 列表浏览：支持分页（10 / 20 / 50 条/页），底部/顶部边缘滑动自动翻页
- 详情编辑：查看完整信息并随时修改
- 左滑删除（带确认弹窗），长按进入多选模式批量删除
- 批量选择：支持全选、批量删除

### 图片管理
- 从相册选取多张图片 / 相机拍照
- 图片自动保存至系统相册 `Pictures/ThingsPath`
- 详情页支持全屏预览与滑动浏览

### AI 智能添加
- 接入 SiliconFlow API（DeepSeek 大模型）
- 粘贴一段文字（购物小票、备忘录等），AI 自动提取物品名称、价格、日期、位置、标签
- 支持一次识别多个物品并批量添加
- 内置 13 大分类标签的智能推断规则

### 搜索与筛选
- 实时搜索（物品名称 / 备注 / 位置，防抖 300ms）
- 标签快速筛选（横向滚动 FilterChip）
- 多维度排序：购买日期 / 名称 / 使用天数 / 更新时间（支持升序/降序）

### 统计分析
- 首页顶部：总物品数 + 总资产价值
- 统计弹窗：标签分布饼图（Top 5）、价格区间分布饼图

### 数据备份与恢复
- 导出：将所有物品序列化为 JSON 文件
- 导入：从 JSON 文件恢复数据
- 自动备份：每次增删改后自动备份至 `/sdcard/Documents/ThingsPath/backup.json`
- 定时后台备份（WorkManager，每小时一次）
- 重装恢复：数据库为空时自动从备份文件恢复

---

## 技术栈

| 类别 | 技术 |
|---|---|
| 语言 | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material Design 3 |
| 架构 | MVVM + Clean Architecture（UseCase 分层） |
| 依赖注入 | Dagger Hilt 2.48.1 |
| 本地数据库 | Room 2.6.1 |
| 持久化配置 | DataStore Preferences 1.0.0 |
| 网络 | Retrofit 2.9.0 + OkHttp 4.12.0 |
| 图片加载 | Coil 2.5.0 |
| 后台任务 | WorkManager 2.9.0 |
| 导航 | Navigation Compose 2.7.7 |
| 异步 | Kotlin Coroutines 1.7.3 |

---

## 项目结构

```
app/src/main/java/com/thingspath/
├── di/                        # Hilt 依赖注入模块
├── data/
│   ├── model/                 # 领域模型
│   ├── local/                 # Room 数据库、DAO、DataStore
│   └── remote/                # Retrofit API、SiliconFlow 仓库
├── domain/
│   └── usecase/               # 业务用例层（Add/Delete/Update/Get/Export/Import）
├── ui/
│   ├── navigation/            # 路由定义与导航图
│   ├── screen/                # 页面（home / additem / itemdetail / settings / statistics）
│   ├── component/             # 可复用 Compose 组件
│   └── theme/                 # Material3 主题（颜色、字体）
├── util/                      # 图片存储工具类
└── worker/                    # WorkManager 后台备份 Worker
```

数据流向：`UI → ViewModel → UseCase → Repository → Room / Retrofit / DataStore`

---

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK API 34
- 设备或模拟器：Android 7.0+（API 24+）

### 构建 Debug 包

```bash
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/thingspath.apk
```

### 安装到设备

```bash
adb install app/build/outputs/apk/debug/thingspath.apk
```

### 使用 deploy 脚本

```bash
./deploy.sh
```

---

## AI 功能配置（可选）

1. 前往 [SiliconFlow 官网](https://siliconflow.cn) 注册并获取 API Key
2. 打开 App → 右上角设置图标 → 输入 API Key 并保存
3. 回到首页，点击 "+" → "AI Smart Add"，粘贴文字即可

---

## 权限说明

| 权限 | 用途 |
|---|---|
| `CAMERA` | 拍照添加物品图片 |
| `READ_EXTERNAL_STORAGE` | 从相册选取图片 |
| `WRITE_EXTERNAL_STORAGE` | 保存备份文件 |
| `INTERNET` | AI 智能添加功能 |

---

## 版本信息

- 当前版本：1.1.0（versionCode: 2）
- 最低支持：Android 7.0（API 24）
- 目标 SDK：Android 14（API 34）
