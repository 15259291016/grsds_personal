package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 退税进度推进、补税办理状态，以及明细新增的已缴税额 / 识别号口径。 */
class SettlementProcessTest {

    private fun declaration(
        balance: Long = 480_02L,
        status: String = "已申报",
        process: String = ""
    ) = DeclarationRecord(
        id = "decl-1",
        year = 2025,
        declarationNo = "HX2025000123",
        submittedAt = 1_760_000_000_000L,
        incomeCents = 160_000_00L,
        taxableIncomeCents = 76_000_00L,
        taxPayableCents = 4_480_00L,
        taxPrepaidCents = 4_480_00L + balance,
        balanceCents = balance,
        status = status,
        processStatus = process
    )

    @Test
    fun `refund stages advance one step at a time`() {
        var status = SettlementProcess.REFUND_APPLIED
        status = SettlementProcess.next(status)
        assertEquals(SettlementProcess.AUDITING, status)
        status = SettlementProcess.next(status)
        assertEquals(SettlementProcess.TREASURY, status)
        status = SettlementProcess.next(status)
        assertEquals(SettlementProcess.REFUNDED, status)
        // 到终点后不再前进
        assertEquals(SettlementProcess.REFUNDED, SettlementProcess.next(status))
        assertTrue(SettlementProcess.isSettled(status))
    }

    @Test
    fun `unapplied refund stays in place`() {
        assertEquals(SettlementProcess.NONE, SettlementProcess.next(SettlementProcess.NONE))
        assertFalse(SettlementProcess.isSettled(SettlementProcess.NONE))
        assertEquals(-1, SettlementProcess.stageIndex(SettlementProcess.NONE))
    }

    @Test
    fun `refund declaration needs action until applied`() {
        val fresh = declaration()
        assertTrue(fresh.isRefund)
        assertTrue(fresh.needsAction)
        assertFalse(fresh.hasAppliedRefund)

        val applied = declaration(process = SettlementProcess.REFUND_APPLIED)
        assertTrue(applied.hasAppliedRefund)
        assertFalse(applied.needsAction)
    }

    @Test
    fun `payment declaration needs action until paid`() {
        val owe = declaration(balance = -1_080_00L)
        assertFalse(owe.isRefund)
        assertTrue(owe.needsAction)

        val paid = declaration(balance = -1_080_00L, process = SettlementProcess.PAID)
        assertTrue(paid.isPaid)
        assertFalse(paid.needsAction)
    }

    @Test
    fun `voided declaration no longer needs action`() {
        val voided = declaration(status = "已作废")
        assertFalse(voided.needsAction)
    }

    @Test
    fun `paid tax falls back to type based estimate`() {
        val salary = IncomeRecord("a", 2025, 1, "工资薪金", 10_000_00L, "某公司", 0L)
        assertEquals(300_00L, salary.paidTaxCents)

        val service = IncomeRecord("b", 2025, 2, "劳务报酬", 10_000_00L, "某公司", 0L)
        assertEquals(2_000_00L, service.paidTaxCents)

        val explicit = IncomeRecord("c", 2025, 3, "工资薪金", 10_000_00L, "某公司", 0L, taxPaidCents = 450_00L)
        assertEquals(450_00L, explicit.paidTaxCents)
    }

    @Test
    fun `agent tax number is stable and 18 characters long`() {
        val first = IncomeRecord.generateAgentTaxNo("北京华信科技有限公司")
        val second = IncomeRecord.generateAgentTaxNo("北京华信科技有限公司")
        assertEquals(first, second)
        assertEquals(18, first.length)
        assertTrue(first.startsWith("91"))
        // 单位名不同，识别号不同
        assertTrue(first != IncomeRecord.generateAgentTaxNo("上海锦明信息技术有限公司"))
        assertEquals("", IncomeRecord.generateAgentTaxNo(""))
    }

    @Test
    fun `prepaid total uses explicit paid tax per record`() {
        val records = listOf(
            IncomeRecord("a", 2025, 1, "工资薪金", 10_000_00L, "某公司", 0L, taxPaidCents = 100_00L),
            IncomeRecord("b", 2025, 2, "工资薪金", 10_000_00L, "某公司", 0L)
        )
        // 100 元显式值 + 第二笔按 3% 推算的 300 元
        assertEquals(400_00L, TaxCalculator.prepaidOf(records, 2025))
    }
}
