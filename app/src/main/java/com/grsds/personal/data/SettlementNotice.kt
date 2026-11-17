package com.grsds.personal.data

/** 首页提醒条要提示的办理事项。 */
enum class SettlementNoticeKind {
    /** 有退税还没申请收款账户。 */
    REFUND_TO_APPLY,

    /** 需要补税且尚未缴款。 */
    TAX_TO_PAY,

    /** 退税已提交，正在审核/国库/退库途中。 */
    REFUND_IN_PROGRESS
}

data class SettlementNotice(val kind: SettlementNoticeKind, val record: DeclarationRecord) {
    /** 点击提醒条要去办的申报记录 id。 */
    val declarationId: String get() = record.id
}

/**
 * 从申报记录里挑出首页提醒条该显示的那一条。
 *
 * 只挑生效中的申报，按「待申请退税 → 待缴款 → 正在退税」的紧急程度取第一条；
 * 都没有时返回 null，提醒条回落成固定的汇算通知。
 */
object SettlementNotices {

    fun top(declarations: List<DeclarationRecord>): SettlementNotice? {
        val active = declarations.filter { it.isActive }.sortedByDescending { it.year }
        active.firstOrNull { it.isRefund && !it.hasAppliedRefund }?.let {
            return SettlementNotice(SettlementNoticeKind.REFUND_TO_APPLY, it)
        }
        active.firstOrNull { !it.isRefund && !it.isPaid }?.let {
            return SettlementNotice(SettlementNoticeKind.TAX_TO_PAY, it)
        }
        active.firstOrNull {
            it.isRefund && it.hasAppliedRefund && !SettlementProcess.isSettled(it.processStatus)
        }?.let {
            return SettlementNotice(SettlementNoticeKind.REFUND_IN_PROGRESS, it)
        }
        return null
    }
}
