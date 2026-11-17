package com.grsds.personal.data

import kotlinx.serialization.Serializable

/**
 * 一张已绑定的银行卡：用于接收退税税款或补税扣款。
 * 真实 APP 绑卡要走银联鉴权，这里只做本地登记 + 脱敏展示。
 */
@Serializable
data class BankCard(
    val id: String,
    val bankName: String,
    /** 16–19 位卡号，只存原文，展示时永远脱敏。 */
    val cardNumber: String,
    val holderName: String,
    val boundAt: Long,
    /** 是否为默认退税收款账户。同一时刻最多一张默认卡。 */
    val isDefault: Boolean = false
) {
    /** 尾号 8888（工商银行）形式的展示文案。 */
    val maskedNumber: String
        get() = "尾号 ${cardNumber.takeLast(4)}（$bankName）"

    val boundAtLabel: String
        get() = java.text.SimpleDateFormat(
            "yyyy-MM-dd", java.util.Locale.CHINA
        ).format(java.util.Date(boundAt))

    companion object {
        /** 退税支持的主要银行，真实 APP 是全量行号表，这里取常见 12 家。 */
        val SUPPORTED_BANKS = listOf(
            "中国工商银行", "中国农业银行", "中国银行", "中国建设银行",
            "交通银行", "招商银行", "中国邮政储蓄银行", "中信银行",
            "兴业银行", "浦发银行", "中国民生银行", "平安银行"
        )

        /** 卡号合法性：16–19 位纯数字。 */
        fun isValidCardNumber(input: String): Boolean =
            input.length in 16..19 && input.all { it.isDigit() }
    }
}
