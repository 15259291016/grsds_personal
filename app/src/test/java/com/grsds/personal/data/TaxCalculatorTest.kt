package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalculatorTest {

    private fun wageRecord(id: String, year: Int, cents: Long) = IncomeRecord(
        id = id,
        year = year,
        month = 1,
        incomeType = "工资薪金",
        amountInCents = cents,
        withholdingAgent = "测试公司",
        createdAt = 0L
    )

    @Test
    fun `settle with no income returns zeros`() {
        val result = TaxCalculator.settle(emptyList(), emptyList(), 2025)
        assertEquals(0L, result.incomeCents)
        assertEquals(0L, result.taxableIncomeCents)
        assertEquals(0L, result.taxPayableCents)
        assertEquals(0L, result.balanceCents)
        assertTrue(result.isRefund)
    }

    @Test
    fun `wage income applies basic deduction and ten percent bracket`() {
        // 年收入 120,000 元 -> 应纳税所得额 60,000 元 -> 税额 6000 - 2520 = 3480 元
        val result = TaxCalculator.settle(
            records = listOf(wageRecord("a", 2025, 12_000_000L)),
            deductions = emptyList(),
            year = 2025
        )
        assertEquals(12_000_000L, result.incomeCents)
        assertEquals(6_000_000L, result.taxableIncomeCents)
        assertEquals(348_000L, result.taxPayableCents)
        // 已预缴 3% = 360,000 分，应退 12,000 分
        assertEquals(360_000L, result.taxPrepaidCents)
        assertEquals(12_000L, result.balanceCents)
        assertTrue(result.isRefund)
    }

    @Test
    fun `special deductions reduce taxable income`() {
        val deduction = DeductionRecord(
            type = "子女教育",
            year = 2025,
            ratioPercent = 100,
            amountInCents = 2_400_000L,
            updatedAt = 0L
        )
        val result = TaxCalculator.settle(
            records = listOf(wageRecord("a", 2025, 12_000_000L)),
            deductions = listOf(deduction),
            year = 2025
        )
        assertEquals(2_400_000L, result.specialDeductionCents)
        assertEquals(3_600_000L, result.taxableIncomeCents)
        // 36,000 元仍在 3% 档：36,000 * 3% = 1,080 元
        assertEquals(108_000L, result.taxPayableCents)
        assertEquals(3, result.taxRatePercent)
    }

    @Test
    fun `deductions from other years are ignored`() {
        val otherYear = DeductionRecord("子女教育", 2024, 100, 2_400_000L, 0L)
        val result = TaxCalculator.settle(
            records = listOf(wageRecord("a", 2025, 12_000_000L)),
            deductions = listOf(otherYear),
            year = 2025
        )
        assertEquals(0L, result.specialDeductionCents)
    }

    @Test
    fun `author remuneration is discounted by eighty percent then seventy percent`() {
        val record = wageRecord("a", 2025, 1_000_000L).copy(incomeType = "稿酬")
        assertEquals(560_000L, TaxCalculator.incomeOf(listOf(record), 2025))
    }

    @Test
    fun `service remuneration is discounted by eighty percent`() {
        val record = wageRecord("a", 2025, 1_000_000L).copy(incomeType = "劳务报酬")
        assertEquals(800_000L, TaxCalculator.incomeOf(listOf(record), 2025))
    }

    @Test
    fun `top of twenty percent bracket uses quick deduction`() {
        // 应纳税所得额 300,000 元 -> 300,000 * 20% - 16,920 = 43,080 元
        val result = TaxCalculator.settle(
            records = listOf(wageRecord("a", 2025, 36_000_000L)),
            deductions = emptyList(),
            year = 2025
        )
        assertEquals(30_000_000L, result.taxableIncomeCents)
        assertEquals(4_308_000L, result.taxPayableCents)
        assertEquals(20, result.taxRatePercent)
        // 已预缴 3% = 1,080,000 分，不足缴税 -> 负数表示应补
        assertTrue(!result.isRefund)
    }
}
