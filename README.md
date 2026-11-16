# grsds_personal

仿「个人所得税」官方 App 的 Android 练习项目：Kotlin + Jetpack Compose（Material 3），从启动封面、首页、收入纳税明细到年度汇算申报，覆盖一条完整的主流程。

数据全部落在本机（DataStore），不联网、不采集真实税务数据。

## 环境

| 项 | 值 |
| --- | --- |
| JDK | Temurin 21（jabba 管理，`D:\Program Files\jabba\jdk\temurin@21.0.11`） |
| Gradle / AGP | 9.5.0 / 9.3.0 |
| Kotlin | 2.2.10（Compose Compiler 插件同版本） |
| compileSdk / minSdk | 36 / 24 |
| applicationId | `com.grsds.personal` |

```bash
# 构建 Debug
./gradlew :app:assembleDebug

# 单元测试（19 个：编解码容错、税额计算、申报编号等）
./gradlew :app:testDebugUnitTest

# 构建 Release（R8 + 资源收缩，13M → 约 1.7M）
./gradlew :app:assembleRelease
```

## 目录

```
app/src/main/java/com/grsds/personal/
├── MainActivity.kt                 # 启动封面 + 返回栈路由（sealed interface AppScreen）
├── HomeScreen.kt                   # 首页：搜索、年度汇算卡、服务卡片、资讯、底部 Tab
├── TabScreens.kt                   # 待办 / 办&查 / 消息 / 我的
├── IncomeTaxDetailsScreen.kt       # 收入纳税明细：查询条件 + 已录入列表（编辑/删除/撤销）
├── QueryResultScreen.kt            # 查询结果：汇总、占比、按月分组
├── RecordDetailScreen.kt           # 明细详情 + 异议申诉
├── BatchCreateScreen.kt            # 批量录入（演示用，藏在 设置 → 演示数据）
├── AnnualReconciliationScreen.kt   # 年度汇算四步向导
├── DeductionsScreen.kt             # 专项附加扣除填报
├── DeclarationRecordsScreen.kt     # 申报记录查询
├── TaxRecordIssueScreen.kt         # 纳税记录开具
├── LoginScreen.kt / SettingsScreen.kt
├── data/                           # 记录模型、DataStore 仓库、税额计算
└── ui/theme/                       # Compose 主题

tools/gen_assets.ps1                # 生成启动图标与封面（GDI+，可重复运行）
app/sampledata/                     # 官方 App 真机截图，UI 还原参考
```

## 文案

界面文案统一放在 `res/values/strings.xml`，按页面分段，key 形如 `home_01` / `tab_29`（页面前缀 + 序号）。

两个例外，属于**数据值**而非 UI 文案，刻意保留字面量：

- `data/IncomeRecord.kt` 的 `INCOME_TYPE_OPTIONS`：记录的 `incomeType` 字段存的就是这些字符串。
- `data/DeductionRecord.kt` 的 `DEDUCTION_OPTIONS[].type`：随扣除填报记录一起落盘。

要支持多语言时，应先把这两个字段改成稳定 id，再按 id 取文案。

## 图标与启动封面

`tools/gen_assets.ps1` 用 GDI+ 画出蓝色盾牌 + 「税」字，输出 5 档密度的 `ic_launcher*`（含自适应与单色层）以及 `drawable-nodpi/cover.png`（1080×2400 启动页）。改样式后重跑脚本即可：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\gen_assets.ps1
```

## 发布

当前 `release` 构建复用 **debug 签名**，只是为了方便直接装机验证。正式发布前：

1. 生成自己的 keystore：
   ```bash
   keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias grsds
   ```
2. 在 `app/build.gradle.kts` 里加 `signingConfigs { create("release") { ... } }`，把 `buildTypes.release.signingConfig` 指向它（密钥口令不要写进仓库，用 `local.properties` 或环境变量）。
3. 每发一版递增 `versionCode`，`versionName` 同步。

## 已知限制

- 所有业务数据均为本地演示数据，与税务总局系统无关，也不会外传。
- 未接入网络层、真实鉴权与实名认证。
- 首页「相关政策」「更多服务」等入口会提示「尚未接入，当前为演示版本」。
