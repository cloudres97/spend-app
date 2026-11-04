# 喵喵记账

Android 本地手动支出账本。打开首页即可记一笔，也可以点开账单修改或删除。记账时填写金额，选择分类和日期。

统计页支持最近 7 天与本月切换。

[下载签名版 APK](downloads/meow-upgrade.apk)。同一个 APK 可首次安装，也可覆盖使用同一签名的旧版并保留本地账单。GitHub Actions 生成的调试 APK 使用单独的签名。

## 构建

使用 Java 17 和 Android SDK 35。Windows 执行 `gradlew.bat lintDebug assembleDebug`，其他平台执行 `./gradlew lintDebug assembleDebug`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。



