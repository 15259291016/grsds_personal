package com.grsds.personal.data

import kotlinx.serialization.Serializable

/** 一条专项附加扣除填报记录，金额以“分”存储。 */
@Serializable
data class DeductionRecord(
    val type: String,
    val year: Int,
    val ratioPercent: Int,
    val amountInCents: Long,
    val updatedAt: Long
)

/** 登录会话，演示版只保存手机号与昵称，不做真实鉴权。 */
@Serializable
data class LoginSession(
    val phone: String,
    val nickname: String,
    val loginAt: Long
) {
    /** 138****8888 形式的脱敏手机号。 */
    val maskedPhone: String
        get() = if (phone.length == 11) "${phone.take(3)}****${phone.takeLast(4)}" else phone
}

/**
 * 专项附加扣除项目定义：名称、年度扣除标准（分）、是否支持 50% 分摊、图标字与配色。
 *
 * [DeductionOption.type] 会随填报记录一起落盘，属于数据值而非纯 UI 文案，
 * 因此与 [INCOME_TYPE_OPTIONS] 一样保留字面量、不抽到 strings.xml。
 */
data class DeductionOption(
    val type: String,
    val yearlyStandardInCents: Long,
    val supportsHalf: Boolean,
    val symbol: String,
    val colorHex: Long
)

val DEDUCTION_OPTIONS = listOf(
    DeductionOption("子女教育", 24_000_00, true, "子", 0xFF1598F4),
    DeductionOption("继续教育", 4_800_00, false, "续", 0xFF8B5CF6),
    DeductionOption("大病医疗", 80_000_00, false, "病", 0xFFFF7043),
    DeductionOption("住房贷款利息", 12_000_00, true, "贷", 0xFF3D8BFF),
    DeductionOption("住房租金", 18_000_00, false, "租", 0xFF15CDB1),
    DeductionOption("赡养老人", 36_000_00, true, "养", 0xFF6C7CF0),
    DeductionOption("3岁以下婴幼儿照护", 24_000_00, true, "婴", 0xFFFF9F43),
    DeductionOption("个人养老金", 12_000_00, false, "老", 0xFF26B4A8)
)
