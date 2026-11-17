package com.grsds.personal

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.grsds.personal.ui.Glyph

/**
 * 首页「政策资讯」文章数据。
 *
 * 首页按分类平铺 8 篇，点「查看更多」进列表页，点单篇进详情页。
 * 正文放在 strings.xml（art_xx），这里只维护结构和排序。
 */
data class ArticleItem(
    val id: String,
    /** 所属分类下标：0 警示案例 / 1 通知公告 / 2 热点问题 / 3 政策解读。 */
    val categoryIndex: Int,
    @StringRes val title: Int,
    val date: String,
    val glyph: Glyph,
    val accent: Color,
    val imageFirst: Boolean = false,
    val body: List<Int> = emptyList()
)

object ArticleCenter {

    val categories = listOf(
        R.string.home_31,
        R.string.home_32,
        R.string.home_33,
        R.string.home_34
    )

    val items: List<ArticleItem> = listOf(
        // 警示案例
        ArticleItem(
            id = "art-password",
            categoryIndex = 0,
            title = R.string.home_56,
            date = "2025-02-28",
            glyph = Glyph.Card,
            accent = Color(0xFF8FD3F4),
            body = listOf(R.string.art_01, R.string.art_02, R.string.art_03)
        ),
        ArticleItem(
            id = "art-bankcard",
            categoryIndex = 0,
            title = R.string.home_58,
            date = "2025-02-28",
            glyph = Glyph.Shield,
            accent = Color(0xFFFFD68D),
            body = listOf(R.string.art_04, R.string.art_05, R.string.art_06)
        ),
        ArticleItem(
            id = "art-case-fail",
            categoryIndex = 0,
            title = R.string.home_60,
            date = "2024-03-15",
            glyph = Glyph.DocCheck,
            accent = Color(0xFF94E9EB),
            imageFirst = true,
            body = listOf(R.string.art_07, R.string.art_08, R.string.art_09)
        ),
        ArticleItem(
            id = "art-case-false",
            categoryIndex = 0,
            title = R.string.home_62,
            date = "2024-03-15",
            glyph = Glyph.Stamp,
            accent = Color(0xFFB9D7FF),
            imageFirst = true,
            body = listOf(R.string.art_10, R.string.art_11, R.string.art_12)
        ),
        // 通知公告
        ArticleItem(
            id = "art-announce",
            categoryIndex = 1,
            title = R.string.home_64,
            date = "2026-02-26",
            glyph = Glyph.Receipt,
            accent = Color(0xFF81C6FF),
            body = listOf(R.string.art_13, R.string.art_14, R.string.art_15, R.string.art_16)
        ),
        ArticleItem(
            id = "art-maintain",
            categoryIndex = 1,
            title = R.string.home_66,
            date = "2026-01-05",
            glyph = Glyph.Calendar,
            accent = Color(0xFF9CE4D8),
            body = listOf(R.string.art_17, R.string.art_18)
        ),
        // 热点问题
        ArticleItem(
            id = "art-refund-progress",
            categoryIndex = 2,
            title = R.string.home_68,
            date = "2025-06-12",
            glyph = Glyph.Briefcase,
            accent = Color(0xFFFFDCA4),
            body = listOf(R.string.art_19, R.string.art_20, R.string.art_21)
        ),
        ArticleItem(
            id = "art-deduction-edit",
            categoryIndex = 2,
            title = R.string.home_70,
            date = "2025-05-20",
            glyph = Glyph.People,
            accent = Color(0xFFAED5FF),
            body = listOf(R.string.art_22, R.string.art_23, R.string.art_24)
        ),
        // 政策解读
        ArticleItem(
            id = "art-annual-guide",
            categoryIndex = 3,
            title = R.string.home_72,
            date = "2025-03-01",
            glyph = Glyph.Clock,
            accent = Color(0xFFA6E6DB),
            body = listOf(R.string.art_25, R.string.art_26, R.string.art_27)
        ),
        ArticleItem(
            id = "art-deduction-guide",
            categoryIndex = 3,
            title = R.string.home_74,
            date = "2025-02-28",
            glyph = Glyph.Check,
            accent = Color(0xFFC7D7FF),
            body = listOf(R.string.art_28, R.string.art_29, R.string.art_30)
        )
    )

    fun byCategory(index: Int): List<ArticleItem> = items.filter { it.categoryIndex == index }

    fun find(id: String?): ArticleItem? = items.firstOrNull { it.id == id }
}
