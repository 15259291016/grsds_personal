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
├── BatchCreateScreen.kt            # 批量录入（藏在 设置 → 数据管理）
├── AnnualReconciliationScreen.kt   # 年度汇算四步向导
├── DeductionsScreen.kt             # 专项附加扣除填报
├── DeclarationRecordsScreen.kt     # 申报记录查询
├── TaxRecordIssueScreen.kt         # 纳税记录开具
├── LoginScreen.kt / SettingsScreen.kt
├── data/                           # 记录模型、DataStore 仓库、税额计算、TaxCalendar 办理期判定
├── ui/Glyphs.kt                    # 自绘线性图标集（24dp / 1.8dp 描边）
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

## 字号与排版

字号基准是从官方 APP 截图里量出来的像素高度（1080px 宽画布，本机 420dpi 下 1sp ≈ 2.625px，汉字墨迹高约为字号的 0.9 倍），写死在 `ui/theme/Type.kt`：

| 层级 | 字号 | 用途 |
| --- | --- | --- |
| 展示数字 | 28–32sp Bold | 申报金额、合计金额 |
| 页面标题 | 20sp Bold | 顶部导航标题 |
| 区块标题 | 17sp Bold | 「重点服务推荐」「我要办税」 |
| 正文 / 列表项 | 17–18sp | 明细行、待办标题 |
| 卡片标题 | 15sp Bold | 首页小卡、消息标题 |
| 辅助说明 | 14sp | 卡片描述、时间戳 |
| 最小字号 | 12–13sp | 分类标签、角标 |

两条硬性约定，改字号前先看一眼：

1. `Typography` 里**不要给 `bodyLarge` 写死 `lineHeight`**。MaterialTheme 会把它作为 `LocalTextStyle` 提供给所有没写 `style` 的 `Text`，一旦写死，13sp 的小标题也会被撑成 24sp 行距，整页字小行空。
2. `letterSpacing` 保持 0。中文加 0.5sp 字距后整行会明显变散，和官方那种紧凑排版差别很大。

多行文本（卡片标题、描述）请显式写 `lineHeight ≈ 1.3 × fontSize`，并配 `maxLines` + `TextOverflow.Ellipsis`，避免字号调整后把卡片撑破。

## 图标

界面图标全部来自 `ui/Glyphs.kt`：在 24×24 的网格上用 `Canvas` 画路径，1.8dp 圆头描边，规格对齐官方 App。
不要再用「⌂ ✓ ▤」这类字符或单个汉字当图标——字形和粗细跟着系统字体走，换机型就会变形，风格也和线性图标对不上。

用法：

```kotlin
GlyphIcon(Glyph.Bell, tint = Color(0xFFFF8A2B), size = 20.dp)
```

新增图标时在 `Glyph` 枚举里加一项，并在 `drawGlyph` 的 `when` 里补一段路径即可。

## 图标与启动封面

`tools/gen_assets.ps1` 用 GDI+ 画出蓝色盾牌 + 「税」字，输出 5 档密度的 `ic_launcher*`（含自适应与单色层）以及 `drawable-nodpi/cover.png`（1080×2400 启动页）。改样式后重跑脚本即可：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\gen_assets.ps1
```

## 办理期与状态

「能不能办」按系统当天日期算，不写死在文案里（`data/TaxCalendar.kt`）：

- 综合所得年度汇算：次年 3 月 1 日 ~ 6 月 30 日。期内显示「可办理」，期后显示「可补办」，未到期显示「未开始」且禁用「下一步」。
- 专项附加扣除确认：上年 12 月 1 日 ~ 12 月 31 日。
- 待办列表按截止日排序，已过期的置灰并显示「已结束」，与首页「已于 6 月 30 日结束」不再互相矛盾。

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
- 首页「相关政策」「更多服务」等入口会提示「暂不可用，请稍后再试」。
