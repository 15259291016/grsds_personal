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
    /** 已申报 / 更正申报 / 已更正 / 已作废。 */
    val status: String = "已申报",
    /** 被更正的原始申报编号；为空表示首次申报。 */
    val correctionOf: String? = null,
    /** 第几次更正，0 表示首次申报。 */
    val correctionSeq: Int = 0,
    /** 退税/补税办理状态，见 [SettlementProcess]；空串表示尚未办理。 */
    val processStatus: String = "",
    /** 退税收款账户 id 与展示用掩码（卡解绑后仍能显示当时的账户）。 */
    val bankCardId: String = "",
    val bankCardLabel: String = "",
    /** 补税缴款凭证号。 */
    val paymentNo: String = "",
    /** 办理状态最后更新时间（申请退税 / 缴款完成）。 */
    val processedAt: Long = 0L
) {
    val isRefund: Boolean get() = balanceCents >= 0

    /** 可发起更正/作废的记录：生效中的申报。 */
    val isActive: Boolean get() = status == "已申报" || status == "更正申报"

    val submittedAtLabel: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(submittedAt))

    val processedAtLabel: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(processedAt))

    /** 退税已申请（进入审核/国库/退库任一阶段）。 */
    val hasAppliedRefund: Boolean get() = processStatus in SettlementProcess.refundStages

    /** 补税已缴款。 */
    val isPaid: Boolean get() = processStatus == SettlementProcess.PAID

    /** 还需要用户去办理：退税未申请，或补税未缴款。 */
    val needsAction: Boolean
        get() = isActive && (if (isRefund) !hasAppliedRefund else !isPaid)

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
