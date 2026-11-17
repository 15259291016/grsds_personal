package com.grsds.personal.data

/**
 * 申报之后的税款办理状态：退税走"申请 → 审核 → 国库 → 退库"，补税直接缴款。
 *
 * 这是演示实现：真实 APP 的状态由税务机关后台推进，这里用本地时间轴模拟，
 * 进入进度页后按 [AUTO_ADVANCE_MILLIS] 自动前进一档，便于完整看到闭环。
 */
object SettlementProcess {

    const val NONE = ""
    const val REFUND_APPLIED = "已申请退税"
    const val AUDITING = "税务审核中"
    const val TREASURY = "国库处理中"
    const val REFUNDED = "已退库"
    const val PAID = "已缴款"

    /** 退税进度时间轴，顺序即阶段顺序。 */
    val refundStages = listOf(REFUND_APPLIED, AUDITING, TREASURY, REFUNDED)

    /** 演示用：进入进度页后每隔多久自动推进一档。 */
    const val AUTO_ADVANCE_MILLIS = 20_000L

    /** 当前阶段序号；未申请或非退税状态返回 -1。 */
    fun stageIndex(status: String): Int = refundStages.indexOf(status)

    /** 下一档状态；已是最后一档时保持不变。 */
    fun next(status: String): String {
        val index = stageIndex(status)
        if (index < 0 || index >= refundStages.lastIndex) return status
        return refundStages[index + 1]
    }

    /** 是否已走到终点（退库完成 / 已缴款）。 */
    fun isSettled(status: String): Boolean = status == REFUNDED || status == PAID
}
