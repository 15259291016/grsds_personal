package com.grsds.personal

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.grsds.personal.ui.Glyph

/**
 * 消息中心数据。
 *
 * 每条消息除了列表要用的标题/摘要/时间，还带一段正文和可选办理入口——
 * 官方的消息点进去是完整通知，末尾通常有「去办理」「去查看」的跳转，
 * 这里用 [MessageAction.target] 表示目标页面，由 MainActivity 负责解析路由。
 */
data class MessageAction(
    @StringRes val label: Int,
    val target: String
)

data class MessageItem(
    val id: String,
    @StringRes val category: Int,
    val glyph: Glyph,
    val color: Color,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val time: Int,
    val body: List<Int>,
    val action: MessageAction? = null
)

object MessageCenter {

    const val TARGET_ANNUAL = "annual"
    const val TARGET_DEDUCTIONS = "deductions"
    const val TARGET_INCOME = "income"
    const val TARGET_RECORDS = "records"
    const val TARGET_SECURITY = "security"

    val items: List<MessageItem> = listOf(
        MessageItem(
            id = "msg-annual",
            category = R.string.tab_66,
            glyph = Glyph.Bell,
            color = Color(0xFF1598F4),
            title = R.string.tab_70,
            summary = R.string.tab_71,
            time = R.string.tab_72,
            body = listOf(
                R.string.msg_annual_1,
                R.string.msg_annual_2,
                R.string.msg_annual_3
            ),
            action = MessageAction(R.string.msg_go_annual, TARGET_ANNUAL)
        ),
        MessageItem(
            id = "msg-deduction",
            category = R.string.tab_66,
            glyph = Glyph.Message,
            color = Color(0xFF8B5CF6),
            title = R.string.tab_74,
            summary = R.string.tab_75,
            time = R.string.tab_76,
            body = listOf(
                R.string.msg_deduction_1,
                R.string.msg_deduction_2,
                R.string.msg_deduction_3
            ),
            action = MessageAction(R.string.msg_go_deductions, TARGET_DEDUCTIONS)
        ),
        MessageItem(
            id = "msg-appeal",
            category = R.string.tab_67,
            glyph = Glyph.Shield,
            color = Color(0xFFFF7043),
            title = R.string.tab_78,
            summary = R.string.tab_79,
            time = R.string.tab_80,
            body = listOf(
                R.string.msg_appeal_1,
                R.string.msg_appeal_2,
                R.string.msg_appeal_3
            ),
            action = MessageAction(R.string.msg_go_income, TARGET_INCOME)
        ),
        MessageItem(
            id = "msg-security",
            category = R.string.tab_68,
            glyph = Glyph.Card,
            color = Color(0xFF26B4A8),
            title = R.string.tab_82,
            summary = R.string.tab_83,
            time = R.string.tab_84,
            body = listOf(
                R.string.msg_security_1,
                R.string.msg_security_2,
                R.string.msg_security_3
            ),
            action = MessageAction(R.string.msg_go_security, TARGET_SECURITY)
        ),
        MessageItem(
            id = "msg-maintain",
            category = R.string.tab_68,
            glyph = Glyph.DocCheck,
            color = Color(0xFF6C7CF0),
            title = R.string.tab_86,
            summary = R.string.tab_87,
            time = R.string.tab_88,
            body = listOf(
                R.string.msg_maintain_1,
                R.string.msg_maintain_2
            )
        )
    )

    fun find(id: String?): MessageItem? = items.firstOrNull { it.id == id }

    /** 当前消息全集 id，供“清除未读”一次性落盘。 */
    fun allIds(): List<String> = items.map { it.id }

    /** 未读条数 = 全集减去已读；已读集合里出现不存在的 id 不影响结果。 */
    fun unreadCount(readIds: List<String>): Int = items.count { it.id !in readIds }
}
