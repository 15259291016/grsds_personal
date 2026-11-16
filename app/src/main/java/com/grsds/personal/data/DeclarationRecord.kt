package com.grsds.personal.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * 年度汇算申报记录。提交成功后落盘，支撑“申报记录查询”：
 * 金额全部是提交那一刻的快照，后续再改收入或扣除都不会影响历史申报。
 */
@Serializable
data class DeclarationRecord(
    val id: String,
    val year: Int,
    val declarationNo: String,
    /** 提交时间（毫秒）。 */
    val submittedAt: Long,
    val incomeCents: Long,
    val taxableIncomeCents: Long,
    val taxPayableCents: Long,
    val taxPrepaidCents: Long,
    val balanceCents: Long,
    val status: String = "已申报"
) {
    val isRefund: Boolean get() = balanceCents >= 0

    val submittedAtLabel: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(submittedAt))

    companion object {
        /** 编号沿用真实 APP 的 HX + 年度 前缀，后 6 位取自提交时间，保证可复现。 */
        fun generateNo(year: Int, submittedAt: Long): String =
            "HX$year" + (submittedAt % 1_000_000).toString().padStart(6, '0')

        /** 由算税结果生成申报记录。 */
        fun from(
            settlement: TaxCalculator.Settlement,
            submittedAt: Long
        ): DeclarationRecord = DeclarationRecord(
            id = "decl-${settlement.year}-$submittedAt",
            year = settlement.year,
            declarationNo = generateNo(settlement.year, submittedAt),
            submittedAt = submittedAt,
            incomeCents = settlement.incomeCents,
            taxableIncomeCents = settlement.taxableIncomeCents,
            taxPayableCents = settlement.taxPayableCents,
            taxPrepaidCents = settlement.taxPrepaidCents,
            balanceCents = settlement.balanceCents
        )
    }
}
