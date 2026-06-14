[AI-REVIEW] Large commit detected: 2757 lines added. Consider reviewing for AI Psychosis.
[AI-REVIEW] Large commit detected: 238 lines added. Consider reviewing for AI Psychosis.
[AI-REVIEW] Large commit detected: 240 lines added. Consider reviewing for AI Psychosis.
[AI-REVIEW] Large commit detected: 298 lines added. Consider reviewing for AI Psychosis.

## [2026-06-13] Release 签名配置 + CI artifact 上传失败

**现象**
1. release build 一直用 debug 签名，CI 的 `KEYSTORE_*` secret 配了但没人消费（死代码）。
2. CI 构建成功，但 artifact 上传报 `No files were found`，GitHub Release 里没有 APK。
3. APK 文件名每次/每 variant 都叫 `thingspath.apk`，debug/release 同名混淆。

**根因**
1. `app/build.gradle.kts` 的 `release { signingConfig = debug }` 写死，没读 keystore。
2. `applicationVariants.all` 把所有 variant 重命名为 `thingspath.apk`，与 workflow 期望的 `thingspath-unsigned.apk` / `thingspath-signed.apk` 路径不符。
3. debug/release 共用同一文件名。

**修复**
- 新增 `releaseSigningConfig`：优先 `System.getenv()`（CI），回退 `local.properties`（本地），再回退 debug。两者都没有时 fresh clone 仍能构建。
- keystore 信息存 `local.properties`（已 gitignore）；CI 走 GitHub Secrets（KEYSTORE_BASE64/PASSWORD/ALIAS/KEY_PASSWORD）。
- APK 按 variant 命名：debug→`thingspath-unsigned.apk`，release→`thingspath-signed.apk`，与 workflow 路径对齐，名字跨构建固定。
- 顺手：`.codegraph/daemon.pid` / `daemon.sock` 加入 `.codegraph/.gitignore` 并从 git 移除（本地产物不该提交）。

**涉及文件**
- `app/build.gradle.kts`（签名读取 + 文件名）
- `local.properties`（本地 keystore 信息，未进 git）
- `.codegraph/.gitignore`（忽略运行时文件）
- GitHub Secrets：4 个 KEYSTORE_* 已更新为新 keystore

**验证**
- 本地 release APK：v2 签名，证书 SHA-256 `72e5e40533fdf6a4466262036ffb364f7658f3c8b663a8eadc25bdeceebe0f02`
- CI run 27466181756：`assembleRelease BUILD SUCCESSFUL`，artifact 上传成功
- GitHub Release v1.1.0-build.87：`thingspath-signed.apk` (2.02MB) + `thingspath-unsigned.apk` (19.09MB) 可下载

**注意**
- 新 keystore 覆盖了 GitHub 上 2026-03-11 的老 keystore。老 keystore 签名的 app 无法被新版本覆盖升级，需卸载重装。
- keystore 本地路径 `~/.android/thingspath.jks`，密码在 `local.properties`，务必备份。

## [2026-06-13] release D1 云同步失效：R8 混淆吃掉了请求字段

**现象**
- release 包（开了 minify）启动后无法从云端拉数据，首页物品列表为空；debug 包正常。
- 抓到 D1 REST API 返回 `400, code 7400`：
  `Invalid property: sql => Required | Invalid property: batch => Required`

**根因**
- D1 REST 要求 body 是 `{"sql": "...", "params": [...]}`。
- `D1QueryRequest` / `D1Result` / `D1Error` / `D1Meta` 这几个 Gson 反序列化用的 data class 放在 `com.thingspath.data.remote` 包下，而 proguard-rules.pro 的 Gson keep 规则只覆盖了 `com.thingspath.data.remote.model.**`，漏了这个包根下的类。
- R8 把 `D1QueryRequest.sql` 重命名成 `a`、`params` 重命名成 `b`，Gson 按字段名序列化出 `{"a":...,"b":...}`，D1 收不到 `sql` 字段直接 400。
- debug 包没开 minify，字段名保留，所以表象是「debug 好的，release 挂的」——典型 R8/Gson 盲区。

**修复**
- `app/proguard-rules.pro` 在 Gson 区段补 4 条 keep，显式锁住这 4 个类的字段名：
  ```
  -keep class com.thingspath.data.remote.D1QueryRequest { *; }
  -keep class com.thingspath.data.remote.D1Result { *; }
  -keep class com.thingspath.data.remote.D1Error { *; }
  -keep class com.thingspath.data.remote.D1Meta { *; }
  ```
- 重新 `assembleRelease`，检查 `mapping.txt` 确认 `D1QueryRequest -> com.thingspath.data.remote.D1QueryRequest:`，`sql` / `params` 不再被改名。

**涉及文件**
- `app/proguard-rules.pro`（+4 行 keep）

**验证**
- curl 直连 D1 确认远端 `items` 表 6 条、`categories` 表存在（远端数据完好）。
- release 包重装到 vivo，force-stop 重启，等 sync 完成。
- 截屏首页：6 个物品全部显示（短裤 / 白色长裤 / 衣服 / 迈从 G87 V2 / 地奈德乳膏 / 运动护膝），与 `cnt: 6` 对上，无错误无空状态。

**教训（Gson + R8 通用铁律）**
- 任何参与 Gson 序列化/反序列化的 data class，所属包必须 100% 覆盖在 keep 规则里，不能只靠 `model.**` 通配，新增在包根下或别的子包的类会静默漏网。
- release 失败但 debug 成功 → 第一反应查 R8 keep 规则，而不是查网络/接口逻辑。
- release build 默认移除 Log，定位此类问题靠 `mapping.txt` + curl 复现请求格式，别指望 logcat。
[AI-REVIEW] Large commit detected: 336 lines added. Consider reviewing for AI Psychosis.
[AI-REVIEW] Large commit detected: 232 lines added. Consider reviewing for AI Psychosis.
[AI-REVIEW] Large commit detected: 234 lines added. Consider reviewing for AI Psychosis.

## [2026-06-14] R2 图片上传失败：API Token 凭据失效（401 Unauthorized）

**现象**
- 添加图片提示"图片上传失败，请重试"，所有图片无法上传，debug/release 包均失败。

**根因**
- `R2_ACCESS_KEY_ID` / `R2_SECRET_ACCESS_KEY` 这对凭据已失效（被轮换/禁用）。所有 PUT 被 R2 以 **401 Unauthorized** 拒绝（401 = 认证失败，非 403 权限不足）。
- 三重对照锁定，排除其他可能：
  - 同账号 **D1（Bearer token）正常**（`success:true, cnt:7`）→ 排除账号/网络/TLS。
  - **boto3（工业级独立 SigV4）用相同凭据也 401** → 排除项目签名算法 bug。
  - bucket 内仍有历史文件 `items/3d950a83-...jpg`（curl public GET 200）→ 上传曾成功，凭据是后来失效。
- 错误被吞没：`UploadImageUseCase` 失败只回传 null，`R2ImageRepositoryTest` 是 mock 测试（mock 掉网络，永远测不到真实 401），所以这个 bug 一直没被测试发现。

**修复方案**
- Cloudflare R2 → Manage R2 API Tokens → 新建 Object Read & Write token（bucket=thingspath），拿新 Access Key/Secret。
- 更新 `local.properties` 两个 R2 凭据字段（已备份 `local.properties.bak`）。
- 重新构建并安装 APK（`BuildConfig.R2_*` 是编译期常量，必须重装新版才生效）。

**涉及文件**
- `local.properties`（R2 凭据两行；该文件 gitignore，不进仓库）

**验证证据**
- 旧凭据：手写 SigV4 PUT 401、boto3 PUT/LIST 401。
- 新凭据：boto3 PUT 200（ETag 返回）、LIST 200、DELETE 200、curl GET public URL 200。

**教训（云凭据失效 + 错误吞没通用铁律）**
- 对接云存储/第三方 API 的"无法上传/无法连接"类 bug，第一反应用成熟工具（boto3/awscli）直连复现，区分"凭据失效(401)"与"代码 bug"：工业级工具也失败 = 凭据问题；工业级工具成功 = 自己代码 bug。比手写签名复现更可靠。
- 错误吞没是 bug 温床：网络层失败只回传 null + 通用文案，会把"凭据失效""403""超时"全压成一个"上传失败"。失败路径必须保留真实 HTTP code / 错误体（至少写日志）。
- 单元测试 mock 掉网络层 = 永远测不到真实认证失败。云对接关键路径要有至少一个真实集成验证（本地脚本 / 集成测试），mock 测试只验证逻辑分支。
- 云凭据会过期/被轮换；编译进 BuildConfig 的凭据失效后用户只能重装新版修复。可轮换凭据宜走服务端中转或可热更新配置，而非编译期常量。
- 旁证：macOS Python 缺根证书时，`urllib + _create_unverified_context()` 访问 r2.dev 会诡异地返回 403（而非 SSL 错误），用 curl（系统钥匙串）复测才是干净的 200——验证公开 URL 别用 unverified SSL。
[AI-REVIEW] Large commit detected: 309 lines added. Consider reviewing for AI Psychosis.
