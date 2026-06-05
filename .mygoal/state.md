# Goal: ThingsPath 三阶段质量提升（R8混淆 + 测试 + 架构优化 + 依赖升级）

## Status: active
## Created: 2026-06-05 23:00
## Updated: 2026-06-05 23:55

## Objective
分三阶段提升 ThingsPath 项目的安全性、代码质量和开发体验：
- **P0**: 开启 R8 混淆 + 补充核心路径单元测试（ItemRepository 同步逻辑、R2ImageRepository 上传、HomeViewModel 状态管理）
- **P1**: 拆分 HomeState 为子状态（ListState/FilterState/SyncState）+ 引入 AppError sealed hierarchy 统一错误处理
- **P2**: 升级 Kotlin 2.0+、AGP 8.7+、Hilt 2.53+、Compose BOM 2025.x

## Verification
- P0: `./gradlew assembleRelease` 成功输出混淆后 APK；`./gradlew test` 新增测试全部通过（覆盖率覆盖 ItemRepository、R2ImageRepository、HomeViewModel）
- P1: HomeState 拆分为 ≤3 个子状态类；所有 catch 块使用 AppError 类型；现有功能不回归
- P2: `./gradlew assembleDebug` 在新版本上构建成功；无 deprecation warning 新增

## Constraints
- 不破坏现有功能（每个阶段完成后 `./gradlew assembleDebug` 通过）
- 不引入新依赖（P1 的错误处理用项目内 sealed class）
- ProGuard 规则必须保留 Reflection/Serialization 相关类（Room Entity、Gson model）
- 每阶段独立可交付，不依赖后续阶段

## Boundaries
- `app/build.gradle.kts` — R8 配置、依赖版本
- `app/proguard-rules.pro` — 混淆规则
- `app/src/main/java/com/thingspath/data/` — Repository 和数据源
- `app/src/main/java/com/thingspath/ui/screen/home/` — HomeState、HomeViewModel
- `app/src/test/` — 新增测试文件
- `build.gradle.kts` — 根级插件版本

## Iteration Policy
按 P0 → P1 → P2 顺序执行。每阶段内：先分析现有代码 → 实施变更 → 运行验证 → 记录证据。P0 未完成前不启动 P1。

## Blocked Stop Condition
报告阻塞点 + 已尝试的路径 + 需要用户提供的信息（如 ProGuard keep 规则不确定时、依赖兼容性冲突时）

---

## Phase Progress

| Phase | Scope | Status |
|-------|-------|--------|
| P0 | R8 混淆 + 核心单元测试 | ✅ completed |
| P1 | HomeState 拆分 + 统一错误处理 | pending |
| P2 | Kotlin 2.0 + Compose BOM 升级 | pending |

---

## Evidence Ledger

| # | Claim | Evidence | Status |
|---|-------|----------|--------|
| 1 | R8 minification enabled | `isMinifyEnabled = true` in build.gradle.kts:44, assembleRelease succeeds | confirmed |
| 2 | Resource shrinking enabled | `isShrinkResources = true` in build.gradle.kts:45, shrinkReleaseRes task runs | confirmed |
| 3 | ProGuard rules cover all frameworks | proguard-rules.pro has Gson/Room/Hilt/Retrofit/Coroutines/DataStore sections | confirmed |
| 4 | 117 new unit tests added | ItemRepositoryTest(38) + R2ImageRepositoryTest(33) + HomeViewModelTest(46) = 117 | confirmed |
| 5 | All 128 tests pass | `./gradlew test` → BUILD SUCCESSFUL, 0 failures | confirmed |
| 6 | Release build succeeds with R8 | `./gradlew assembleRelease` → BUILD SUCCESSFUL | confirmed |

## Iteration Log

| # | Time | Action | Result | Next |
|---|------|--------|--------|------|
| 1 | 2026-06-05 23:00 | 创建 Goal，解析三阶段任务 | Goal initialized | Start P0 |
| 2 | 2026-06-05 23:05 | 并行启动 4 个 worktree agent | R8 config + 3 test agents running | Wait for completion |
| 3 | 2026-06-05 23:15 | R8 config agent completed | build.gradle.kts + proguard-rules.pro updated | Continue waiting |
| 4 | 2026-06-05 23:35 | HomeVM test agent completed | 46 tests, all pass | Continue waiting |
| 5 | 2026-06-05 23:55 | ItemRepo + R2Repo agents completed | 38 + 33 tests, all pass | Merge all changes |
| 6 | 2026-06-05 23:55 | Merged all worktree changes to main | Fixed isShrinkResources typo, added JVM args | Run verification |
| 7 | 2026-06-05 23:57 | Full verification: 128/128 tests pass, assembleRelease OK | P0 complete | Commit + move to P1 |
