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
    val disputed: Boolean = false,
    /** 扣缴义务人统一社会信用代码（18 位），旧记录为空时按单位名生成。 */
    val agentTaxNo: String = "",
    /** 该笔所得已预缴的个人所得税（分）；为空表示按类型默认口径推算。 */
    val taxPaidCents: Long? = null
) {
    val amountLabel: String
        get() = formatYuan(amountInCents)

    /**
     * 已预缴税额：优先取录入时填写的实际值，缺省时按类型推算
     * （工资薪金 3%、其余综合所得 20%）。汇算与明细展示共用这一口径。
     */
    val paidTaxCents: Long
        get() = taxPaidCents ?: defaultPrepaidOf(incomeType, amountInCents)

    /** 明细行/详情页展示用的识别号，老记录没有落盘值时按单位名稳定生成。 */
    val displayAgentTaxNo: String
        get() = agentTaxNo.ifBlank { generateAgentTaxNo(withholdingAgent) }

    companion object {
        /** 演示口径：工资薪金按 3% 预扣，劳务报酬等按 20% 预扣。 */
        fun defaultPrepaidOf(incomeType: String, amountInCents: Long): Long =
            if (incomeType == "工资薪金") amountInCents * 3 / 100 else amountInCents * 20 / 100

        /**
         * 按单位名生成稳定的 18 位统一社会信用代码形态的字符串。
         * 只用于展示，保证同名的扣缴义务人每次算出来一致。
         */
        fun generateAgentTaxNo(agent: String): String {
            if (agent.isBlank()) return ""
            var seed = 7L
            agent.forEach { ch -> seed = (seed * 131 + ch.code) and 0x7FFFFFFFFFFFFFFFL }
            // 18 位 = 登记管理部门(2) + 主体标识(15) + 校验码(1)
            val body = seed.toString().padStart(15, '0').takeLast(15)
            val check = "0123456789ABCDEFGHJKLMNPQRTUWXY"[(seed % 30).toInt()]
            return "91" + body + check
        }
    }
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
