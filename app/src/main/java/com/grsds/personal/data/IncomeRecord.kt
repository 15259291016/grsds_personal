package com.grsds.personal.data

import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * 一条收入纳税明细记录。金额以“分”为单位用 [Long] 存储，避免浮点累加误差。
 */
@Serializable
data class IncomeRecord(
    val id: String,
    val year: Int,
    val month: Int,
    val incomeType: String,
    val amountInCents: Long,
    val withholdingAgent: String,
    val createdAt: Long,
    val disputed: Boolean = false
) {
    val amountLabel: String
        get() = formatYuan(amountInCents)
}

/**
 * 个税法定所得类型，批量录入与明细查询共用同一份定义。
 *
 * 这些文案同时是**落盘用的数据值**（[IncomeRecord.incomeType] 存的就是这里的字符串），
 * 所以刻意不抽到 strings.xml：一旦跟着语言变，历史记录就对不上类型了。
 * 将来要做多语言，应该改成存稳定 id、展示时再按 id 取文案。
 */
val INCOME_TYPE_OPTIONS: List<String> = listOf(
    "工资薪金",
    "劳务报酬",
    "稿酬",
    "特许权使用费",
    "经营所得",
    "利息、股息、红利",
    "财产租赁",
    "财产转让",
    "偶然所得"
)

/** 把“分”格式化成带千分位的两位小数金额。 */
fun formatYuan(cents: Long): String = String.format(Locale.CHINA, "%,.2f", cents / 100.0)

/** 把用户输入的金额文本解析成“分”，非法或超出范围时返回 null。 */
fun parseYuanToCents(text: String): Long? {
    val normalized = text.trim().replace(",", "").replace("，", "")
    if (normalized.isEmpty()) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (value <= 0.0 || value > 100_000_000.0) return null
    return Math.round(value * 100)
}
