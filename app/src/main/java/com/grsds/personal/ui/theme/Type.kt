package com.grsds.personal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 全局字体规范，数值来自官方 APP 截图的像素比对
 * （1080px 宽画布，本机 420dpi 下 1sp ≈ 2.625px，汉字墨迹高约为字号的 0.9 倍）：
 *
 * | 层级 | 字号 | 官方对应 |
 * | --- | --- | --- |
 * | 页面标题 | 20sp Bold | 「收入纳税明细」墨迹 47px |
 * | 区块标题 | 17sp Bold | 「重点服务推荐」墨迹 41px |
 * | 正文 / 列表项 | 17sp | 「年度 / 2026」墨迹 45px |
 * | 卡片标题 | 15sp Bold | 首页小卡标题墨迹 35px |
 * | 辅助说明 | 14sp | 卡片描述墨迹 27px |
 * | 最小字号 | 12sp | 分类标签墨迹 31px |
 *
 * 两个刻意的写法：
 * 1. **不写死 lineHeight**。MaterialTheme 会把 [bodyLarge] 作为 `LocalTextStyle` 提供给所有
 *    没有显式 style 的 Text；如果这里写死 24sp，13sp 的小标题也会被撑成 24sp 行距，
 *    整页会显得字又小、行又空。留空后 Compose 按字号取字体自身行距（约 1.3~1.4 倍）。
 * 2. **letterSpacing 一律为 0**。中文正文不需要额外字距，0.5sp 会让每行明显变散，
 *    和官方那种紧凑的排版差别很大。
 */
private val Base = TextStyle(
    fontFamily = FontFamily.Default,
    letterSpacing = 0.sp
)

val Typography = Typography(
    // 展示型数字（汇算金额、申报编号等）
    displayLarge = Base.copy(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    displayMedium = Base.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    displaySmall = Base.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),

    headlineLarge = Base.copy(fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold),
    headlineMedium = Base.copy(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    headlineSmall = Base.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),

    // 页面标题 / 区块标题 / 卡片标题
    titleLarge = Base.copy(fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleMedium = Base.copy(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
    titleSmall = Base.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),

    // 正文：Text() 没有指定 style 时用的就是这一档
    bodyLarge = Base.copy(fontSize = 17.sp, lineHeight = 23.sp),
    bodyMedium = Base.copy(fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = Base.copy(fontSize = 13.sp, lineHeight = 18.sp),

    // 按钮与标签
    labelLarge = Base.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.copy(fontSize = 14.sp, lineHeight = 19.sp),
    labelSmall = Base.copy(fontSize = 12.sp, lineHeight = 16.sp)
)
