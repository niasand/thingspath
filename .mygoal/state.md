# Goal: ThingsPath 三阶段质量提升（R8混淆 + 测试 + 架构优化 + 依赖升级）

## Status: active
## Created: 2026-06-05 23:00
## Updated: 2026-06-06 00:15

## Objective
分三阶段提升 ThingsPath 项目的安全性、代码质量和开发体验：
- **P0**: 开启 R8 混淆 + 补充核心路径单元测试
- **P1**: 拆分 HomeState 为子状态 + 引入 AppError sealed hierarchy 统一错误处理
- **P2**: 升级 Kotlin 2.0+、AGP 8.7+、Hilt 2.53+、Compose BOM 2025.x

## Verification
- P0: `./gradlew assembleRelease` 成功；128/128 tests pass
- P1: HomeState 拆分为 ListState + FilterState；所有 catch 块使用 AppError；128/128 tests pass
- P2: `./gradlew assembleDebug` 在新版本上构建成功

## Constraints
- 不破坏现有功能
- 不引入新依赖
- 每阶段独立可交付

---

## Phase Progress

| Phase | Scope | Status |
|-------|-------|--------|
| P0 | R8 混淆 + 核心单元测试 | ✅ completed |
| P1 | HomeState 拆分 + 统一错误处理 | ✅ completed |
| P2 | Kotlin 2.0 + Compose BOM 升级 | 🔄 in progress (agent running) |

---

## Evidence Ledger

| # | Claim | Evidence | Status |
|---|-------|----------|--------|
| 1 | R8 minification enabled | assembleRelease succeeds, isMinifyEnabled=true | confirmed |
| 2 | 117 new unit tests | ItemRepo(38) + R2Repo(33) + HomeVM(46) = 117 | confirmed |
| 3 | All 128 tests pass | `./gradlew test` BUILD SUCCESSFUL | confirmed |
| 4 | AppError sealed hierarchy | 7 subtypes, all catch blocks wrapped | confirmed |
| 5 | HomeState split | ListState + FilterState + top-level UI state | confirmed |
| 6 | P1 tests still pass | 128/128 after refactor | confirmed |

## Iteration Log

| # | Time | Action | Result | Next |
|---|------|--------|--------|------|
| 1-7 | 2026-06-05 23:00-23:57 | P0: 4 parallel agents + merge | R8 + 117 tests, all pass | Start P1 |
| 8 | 2026-06-06 00:00 | P1 agent launched (single, sequential) | Running | Wait |
| 9 | 2026-06-06 00:10 | P2 agent launched (parallel with P1) | Running | Wait |
| 10 | 2026-06-06 00:15 | P1 completed, merged, fixed test assertion | 128/128 pass, committed | Wait for P2 |
