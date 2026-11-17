package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 汇算计算明细用到的档位、收入构成与申报快照还原。 */
class TaxCalculationDetailTest {

    private fun income(
        id: String,
        type: String,
        cents: Long,
        year: Int = 2025,
        month: Int = 1,
        paidTaxCents: Long? = null
    ) = IncomeRecord(
        id = id,
        year = year,
        month = month,
        incomeType = type,
        amountInCents = cents,
        withholdingAgent = "测试单位",
        createdAt = 0L,
        taxPaidCents = paidTaxCents
    )

    private fun declaration(
        incomeCents: Long = 180_000_00,
        taxableCents: Long = 96_000_00,
        payableCents: Long = 7_080_00,
        prepaidCents: Long = 5_520_00
    ) = DeclarationRecord(
        id = "decl-2025",
        year = 2025,
        declarationNo = "HX2025000001",
        submittedAt = 1_765_000_000_000,
        incomeCents = incomeCents,
        taxableIncomeCents = taxableCents,
        taxPayableCents = payableCents,
        taxPrepaidCents = prepaidCents,
        balanceCents = prepaidCents - payableCents
    )

    // -- 税率档位 -----------------------------------------------------------

    @Test
    fun `bracket boundaries switch at the cent`() {
        assertEquals(3, TaxCalculator.bracketOf(0L).ratePercent)
        assertEquals(3, TaxCalculator.bracketOf(36_000_00L).ratePercent)
        assertEquals(10, TaxCalculator.bracketOf(36_000_01L).ratePercent)
        assertEquals(10, TaxCalculator.bracketOf(144_000_00L).ratePercent)
        assertEquals(20, TaxCalculator.bracketOf(144_000_01L).ratePercent)
        assertEquals(30, TaxCalculator.bracketOf(660_000_00L).ratePercent)
        assertEquals(45, TaxCalculator.bracketOf(960_000_01L).ratePercent)
        assertTrue(TaxCalculator.bracketOf(999_999_00L).isTop)
    }

    @Test
    fun `bracket range labels read like the published table`() {
        val brackets = TaxCalculator.taxBrackets
        assertEquals("不超过 ¥36,000.00", TaxCalculator.bracketRangeLabel(brackets[0]))
        assertEquals("超过 ¥36,000.00 至 ¥144,000.00", TaxCalculator.bracketRangeLabel(brackets[1]))
        assertEquals("超过 ¥960,000.00", TaxCalculator.bracketRangeLabel(brackets.last()))
    }

    @Test
    fun `settlement carries the quick deduction of the applied bracket`() {
        val settlement = TaxCalculator.settleWith(
            year = 2025,
            incomeCents = 180_000_00,
            specialDeductionCents = 24_000_00,
            taxPrepaidCents = 5_520_00
        )
        assertEquals(96_000_00L, settlement.taxableIncomeCents)
        assertEquals(10, settlement.taxRatePercent)
        assertEquals(2_520_00L, settlement.quickDeductionCents)
        assertEquals(7_080_00L, settlement.taxPayableCents)
        // 应退 = 预缴 − 应纳
        assertEquals(-1_560_00L, settlement.balanceCents)
        assertEquals(84_000_00L, settlement.totalDeductionCents)
    }

    @Test
    fun `tax payable formula shows rate and quick deduction`() {
        val settlement = TaxCalculator.settleWith(2025, 180_000_00, 24_000_00, 5_520_00)
        val formula = settlement.taxPayableFormula
        assertTrue(formula, formula.contains("96,000.00"))
        assertTrue(formula, formula.contains("10%"))
        assertTrue(formula, formula.contains("2,520.00"))
    }

    // -- 收入构成 -----------------------------------------------------------

    @Test
    fun `income slices discount labour and royalty income`() {
        val records = listOf(
            income("a", "工资薪金", 100_000_00),
            income("b", "劳务报酬", 20_000_00),
            income("c", "稿酬", 10_000_00),
            income("d", "特许权使用费", 5_000_00)
        )
        val slices = TaxCalculator.incomeSlices(records, 2025)
        assertEquals(listOf("工资薪金", "劳务报酬", "稿酬", "特许权使用费"), slices.map { it.type })
        val wage = slices.first { it.type == "工资薪金" }
        assertEquals(100_000_00L, wage.countedCents)
        assertFalse(wage.discounted)
        val labour = slices.first { it.type == "劳务报酬" }
        assertEquals(20_000_00L, labour.grossCents)
        assertEquals(16_000_00L, labour.countedCents)
        assertEquals(80, labour.discountPercent)
        val royalty = slices.first { it.type == "稿酬" }
        assertEquals(5_600_00L, royalty.countedCents)
        assertEquals(56, royalty.discountPercent)
    }

    @Test
    fun `income slices only cover the requested year`() {
        val records = listOf(
            income("a", "工资薪金", 100_000_00, year = 2025),
            income("b", "工资薪金", 50_000_00, year = 2024)
        )
        assertEquals(100_000_00L, TaxCalculator.incomeSlices(records, 2025).sumOf { it.countedCents })
        assertTrue(TaxCalculator.incomeSlices(records, 2023).isEmpty())
    }

    @Test
    fun `slice total matches settlement income`() {
        val records = listOf(
            income("a", "工资薪金", 100_000_00),
            income("b", "劳务报酬", 20_000_00),
            income("c", "稿酬", 10_000_00)
        )
        val slices = TaxCalculator.incomeSlices(records, 2025)
        assertEquals(TaxCalculator.incomeOf(records, 2025), slices.sumOf { it.countedCents })
    }

    // -- 申报快照还原 -------------------------------------------------------

    @Test
    fun `declaration snapshot rebuilds the same settlement`() {
        val record = declaration()
        val settlement = TaxCalculator.fromDeclaration(record)
        assertEquals(record.incomeCents, settlement.incomeCents)
        assertEquals(24_000_00L, settlement.specialDeductionCents)
        assertEquals(record.taxableIncomeCents, settlement.taxableIncomeCents)
        assertEquals(record.taxPayableCents, settlement.taxPayableCents)
        assertEquals(record.taxPrepaidCents, settlement.taxPrepaidCents)
        assertEquals(record.balanceCents, settlement.balanceCents)
    }

    @Test
    fun `snapshot keeps working when income is below the basic deduction`() {
        val record = declaration(
            incomeCents = 50_000_00,
            taxableCents = 0L,
            payableCents = 0L,
            prepaidCents = 300_00
        )
        val settlement = TaxCalculator.fromDeclaration(record)
        // 收入不够 6 万时扣除按实际收入封顶，不能出现负扣除
        assertEquals(0L, settlement.specialDeductionCents)
        assertEquals(0L, settlement.taxableIncomeCents)
        assertEquals(300_00L, settlement.balanceCents)
        assertTrue(settlement.isRefund)
    }
}
