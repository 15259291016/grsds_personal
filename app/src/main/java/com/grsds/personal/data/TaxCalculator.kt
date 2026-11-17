package com.grsds.personal.data

/**
 * 综合所得年度汇算的简化算法，规则贴近真实政策但只保留主要项：
 * 收入额按类型折算 -> 减除费用 6 万 -> 专项附加扣除 -> 七级超额累进税率。
 * 金额全部以“分”为单位，避免浮点误差。
 */
object TaxCalculator {

    /** 基本减除费用 6 万元/年。 */
    const val BASIC_DEDUCTION_CENTS = 60_000_00L

    /** 各档税率上限（分）与税率、速算扣除数（分）。 */
    private data class Bracket(val upperCents: Long, val ratePercent: Int, val quickDeductionCents: Long)

    private val brackets = listOf(
        Bracket(36_000_00L, 3, 0L),
        Bracket(144_000_00L, 10, 2_520_00L),
        Bracket(300_000_00L, 20, 16_920_00L),
        Bracket(420_000_00L, 25, 31_920_00L),
        Bracket(660_000_00L, 30, 52_920_00L),
        Bracket(960_000_00L, 35, 85_920_00L),
        Bracket(Long.MAX_VALUE, 45, 181_920_00L)
    )

    data class Settlement(
        val year: Int,
        val incomeCents: Long,
        val basicDeductionCents: Long,
        val specialDeductionCents: Long,
        val taxableIncomeCents: Long,
        val taxRatePercent: Int,
        val taxPayableCents: Long,
        val taxPrepaidCents: Long,
        /** 正数为应退税额，负数为应补税额。 */
        val balanceCents: Long
    ) {
        val isRefund: Boolean get() = balanceCents >= 0
    }

    /** 综合所得收入额：工资薪金全额，劳务报酬/稿酬/特许权使用费按 80%（稿酬再按 70%）。 */
    fun incomeOf(records: List<IncomeRecord>, year: Int): Long =
        records.filter { it.year == year }.sumOf { record ->
            when (record.incomeType) {
                "劳务报酬", "特许权使用费" -> record.amountInCents * 80 / 100
                "稿酬" -> record.amountInCents * 80 / 100 * 70 / 100
                else -> record.amountInCents
            }
        }

    /** 已预缴税额：取每笔明细的实缴税额（未填时按类型默认口径推算）。 */
    fun prepaidOf(records: List<IncomeRecord>, year: Int): Long =
        records.filter { it.year == year }.sumOf { it.paidTaxCents }

    fun settle(
        records: List<IncomeRecord>,
        deductions: List<DeductionRecord>,
        year: Int
    ): Settlement = settleWith(
        year = year,
        incomeCents = incomeOf(records, year),
        specialDeductionCents = deductions.filter { it.year == year }.sumOf { it.amountInCents },
        taxPrepaidCents = prepaidOf(records, year)
    )

    /**
     * 用显式金额直接结算：更正申报不重放录入记录，
     * 而是拿原申报快照 + 用户改动后的收入/已缴税额重算。
     */
    fun settleWith(
        year: Int,
        incomeCents: Long,
        specialDeductionCents: Long,
        taxPrepaidCents: Long
    ): Settlement {
        val taxable = (incomeCents - BASIC_DEDUCTION_CENTS - specialDeductionCents).coerceAtLeast(0L)
        val bracket = brackets.first { taxable <= it.upperCents }
        val payable = (taxable * bracket.ratePercent / 100 - bracket.quickDeductionCents)
            .coerceAtLeast(0L)
        return Settlement(
            year = year,
            incomeCents = incomeCents,
            basicDeductionCents = BASIC_DEDUCTION_CENTS,
            specialDeductionCents = specialDeductionCents,
            taxableIncomeCents = taxable,
            taxRatePercent = bracket.ratePercent,
            taxPayableCents = payable,
            taxPrepaidCents = taxPrepaidCents,
            balanceCents = taxPrepaidCents - payable
        )
    }
}
