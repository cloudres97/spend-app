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
CSV 导入仍是原型：尚未验证微信/支付宝原始账单的编码、说明行、收支/退款和重复导入处理。核对原始账单后再使用统计；当前导入逻辑可能把收入或退款当作支出。备份导出按钮尚未完成文件写入。

## 微信与支付宝 API

当前未接入个人账单自动同步。公开商户对账接口不能用来读取个人账户在其他商家的全部消费。详见 [接入评估](docs/payment-api-assessment.md)。

## 来源

来源提交：f5926f49dad9b147d7f6ed464d459b78c43a6869。
只迁移 spend 源码及必要构建文件；不包含健康 App 源码、旧 APK 或个人账单。
