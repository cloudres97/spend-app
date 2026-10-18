# 日账 · Daily Spend

轻量离线 Android 记账应用，由 cloudres97/health-app 的 spend 模块拆分。

独立仓库：https://github.com/cloudres97/spend-app

[下载 APK](https://github.com/cloudres97/spend-app/raw/refs/heads/main/downloads/daily-spend.apk) · [构建状态](https://github.com/cloudres97/spend-app/actions)

## 构建与下载

Java 17、Android SDK 35。运行 `bash ./gradlew testDebugUnitTest lintDebug assembleDebug`。
APK：`app/build/outputs/apk/debug/app-debug.apk`。
GitHub Actions 构建成功后提供 `daily-spend-debug-apk` Artifact；main 分支构建后更新 `downloads/daily-spend.apk`。

保留包名 `com.cloudres.spend`，应用数据结构不变。升级覆盖安装还需要相同签名；不同仓库的默认 debug 签名可能不同，迁移仓库不保证可以直接覆盖旧 APK。

## 功能与当前限制

今日支出、本月累计、渠道汇总、手动记账、通用 CSV 导入、本机存储。
CSV 导入仍是原型：尚未验证微信/支付宝原始账单的编码、说明行、收支/退款和重复导入处理。核对原始账单后再使用统计；当前导入逻辑可能把收入或退款当作支出。JSON 本地备份导出可用。

## 微信与支付宝 API

当前未接入个人账单自动同步。公开商户对账接口不能用来读取个人账户在其他商家的全部消费。详见 [接入评估](docs/payment-api-assessment.md)。

## 来源

来源提交：f5926f49dad9b147d7f6ed464d459b78c43a6869。
只迁移 spend 源码及必要构建文件；不包含健康 App 源码、旧 APK 或个人账单。

## 自动记账试用版 1.1.0

适用于 Android 8+，本次目标试用设备为三星 S26+ / Android 16（未做真机验证）。

1. 安装后进入 **设置 → 自动记账**，阅读并同意说明，再到系统开启 **日账 · 支付通知监听**。
2. 可选开启 **日账 · 支付页面识别** 无障碍服务；只读取当前可访问的微信/支付宝成功页文本，不点击、不截屏、不执行付款。
3. 三星系统检查日账的后台电池限制；如有“从不自动休眠的应用”，加入日账。权限和后台设置无法由 App 代为静默开启。
4. 日常付款后自动入账，不要求逐笔确认。设置页显示最后检测时间、检测结果和累计入账数；本地规则自检不写入账本。

仅处理 `com.tencent.mm` 和 `com.eg.android.AlipayGphone`。通知使用权在系统层面可能具有更广的访问范围，但代码忽略其他包。仅明确的成功支出自动入账；退款、收入、转账、红包、汇总、历史详情和金额不明事件跳过。金额解析使用整数分；旧账单格式兼容。所有事件处理和数据保存都在本机，APK 不申请网络权限；不保存通知全文、页面全文、密码或验证码。

同一通知按事件标识去重。同一成功页在停留期间只处理一次；退出后再次进入成功页仍有重复风险。跨渠道在 15 秒内金额与商户一致时合并，商户不明的同额候选暂不计第二笔；这可能漏记短时间内的真实同额交易。不同金额/不同商户保留为不同记录。

**边界**：不是官方账单同步，不能读取历史账本。Android 16 保护的敏感页面与被隐藏的通知内容不可读取，不绕过保护，也不伪装残障辅助工具。不做 OCR 或 Root/Hook。仅支持保守中文规则，实际微信/支付宝版本可能需要适配。可能漏记或误记，不保证与官方账单完全一致；自动退款抵扣和自动分类尚未实现。此版旨在验证该手机能否获得足够信息。

源码构建需通过 `testDebugUnitTest lintDebug assembleDebug`。解析器测试使用虚构文本，覆盖支出、金额歧义、退款/收入排除、聊天与包名过滤、敏感内容和历史页面排除；不代表真机服务测试。Android 官方 Android 16 限制说明：https://android-developers.googleblog.com/2025/12/enhancing-android-security-stop-malware.html
