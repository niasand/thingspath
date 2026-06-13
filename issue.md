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
