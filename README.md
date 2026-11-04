# 喵喵记账

Android 本地手动支出账本。打开首页即可记一笔，也可以点开账单修改或删除。记账时填写金额，选择分类、日期和支付方式，消费内容可选。

升级使用原有的本地账本。覆盖安装需使用相同的应用签名。

## 构建

使用 Java 17 和 Android SDK 35。Windows 执行 `gradlew.bat lintDebug assembleDebug`，其他平台执行 `./gradlew lintDebug assembleDebug`。APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

