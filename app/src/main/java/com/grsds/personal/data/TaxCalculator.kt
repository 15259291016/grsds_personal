package com.grsds.personal.data

import kotlin.math.abs

/**
 * 综合所得年度汇算的简化算法，规则贴近真实政策但只保留主要项：
 * 收入额按类型折算 -> 减除费用 6 万 -> 专项附加扣除 -> 七级超额累进税率。
 * 金额全部以“分”为单位，避免浮点误差。
 */
object TaxCalculator {

    /** 基本减除费用 6 万元/年。 */
    const val BASIC_DEDUCTION_CENTS = 60_000_00L

    /**
     * 一级税率档：上限（分）、税率、速算扣除数（分）。
     * 最高档上限为 null，表示「超过上一档上限」。
     */
    data class TaxBracket(
        val upperCents: Long?,
        val ratePercent: Int,
        val quickDeductionCents: Long
    ) {
        val isTop: Boolean get() = upperCents == null
    }

    /** 七级超额累进税率表，顺序即档位顺序。 */
    val taxBrackets: List<TaxBracket> = listOf(
        TaxBracket(36_000_00L, 3, 0L),
        TaxBracket(144_000_00L, 10, 2_520_00L),
        TaxBracket(300_000_00L, 20, 16_920_00L),
        TaxBracket(420_000_00L, 25, 31_920_00L),
        TaxBracket(660_000_00L, 30, 52_920_00L),
        TaxBracket(960_000_00L, 35, 85_920_00L),
        TaxBracket(null, 45, 181_920_00L)
    )

    /** 应纳税所得额落在哪一档；0 元也落在第一档。 */
    fun bracketOf(taxableCents: Long): TaxBracket =
        taxBrackets.first { it.upperCents == null || taxableCents <= it.upperCents }

    /** 某一档的说明文案，计算明细页的税率表直接用它。 */
    fun bracketRangeLabel(bracket: TaxBracket): String = when {
        bracket.isTop -> "超过 ¥960,000.00"
        bracket.upperCents == 36_000_00L -> "不超过 ¥36,000.00"
        else -> "超过 ¥${formatYuan(prevUpperOf(bracket))} 至 ¥${formatYuan(bracket.upperCents!!)}"
    }

    private fun prevUpperOf(bracket: TaxBracket): Long {
        val index = taxBrackets.indexOf(bracket)
        return if (index <= 0) 0L else taxBrackets[index - 1].upperCents ?: 0L
    }

    /** 综合所得的收入构成：某类收入的原额，以及计入收入额的部分。 */
    data class IncomeSlice(
        val type: String,
        /** 明细里的收入原额合计。 */
        val grossCents: Long,
        /** 折算后计入综合所得收入额的金额。 */
        val countedCents: Long
    ) {
        /** 是否做过折算（劳务报酬 80%、稿酬 56%）。 */
        val discounted: Boolean get() = countedCents != grossCents

        val discountPercent: Int
            get() = if (grossCents == 0L) 100 else (countedCents * 100 / grossCents).toInt()
    }

    data class Settlement(
        val year: Int,
        val incomeCents: Long,
        val basicDeductionCents: Long,
        val specialDeductionCents: Long,
        val taxableIncomeCents: Long,
        val taxRatePercent: Int,
        val quickDeductionCents: Long = 0L,
        val taxPayableCents: Long,
        val taxPrepaidCents: Long,
        /** 正数为应退税额，负数为应补税额。 */
        val balanceCents: Long
    ) {
        val isRefund: Boolean get() = balanceCents >= 0

        /** 扣除合计（减除费用 + 专项附加扣除）。 */
        val totalDeductionCents: Long get() = basicDeductionCents + specialDeductionCents

        /** 应纳税额的计算式：应纳税所得额 × 税率 − 速算扣除数。 */
        val taxPayableFormula: String
            get() = "¥${formatYuan(taxableIncomeCents)} × $taxRatePercent% − ¥${formatYuan(quickDeductionCents)}"
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

    /** 按所得类型汇总收入构成，金额从大到小；没有记录的年度返回空表。 */
    fun incomeSlices(records: List<IncomeRecord>, year: Int): List<IncomeSlice> =
        records.filter { it.year == year }
            .groupBy { it.incomeType }
            .map { (type, list) ->
                val gross = list.sumOf { it.amountInCents }
                IncomeSlice(type = type, grossCents = gross, countedCents = countedOf(type, gross))
            }
            .sortedByDescending { it.countedCents }

    /** 单类收入的折算口径，收入构成与总结算共用，避免两处算法漂移。 */
    fun countedOf(incomeType: String, grossCents: Long): Long = when (incomeType) {
        "劳务报酬", "特许权使用费" -> grossCents * 80 / 100
        "稿酬" -> grossCents * 80 / 100 * 70 / 100
        else -> grossCents
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
        val bracket = bracketOf(taxable)
        val payable = (taxable * bracket.ratePercent / 100 - bracket.quickDeductionCents)
            .coerceAtLeast(0L)
        return Settlement(
            year = year,
            incomeCents = incomeCents,
            basicDeductionCents = BASIC_DEDUCTION_CENTS,
            specialDeductionCents = specialDeductionCents,
            taxableIncomeCents = taxable,
            taxRatePercent = bracket.ratePercent,
            quickDeductionCents = bracket.quickDeductionCents,
            taxPayableCents = payable,
            taxPrepaidCents = taxPrepaidCents,
            balanceCents = taxPrepaidCents - payable
        )
    }

    /**
     * 由申报快照还原一份结算结果：应纳税所得额与专项附加扣除是账面恒等式推导出来的，
     * 保证「计算明细」与已提交的申报数字完全一致（事后改扣除也不会让历史申报变形）。
     */
    fun fromDeclaration(declaration: DeclarationRecord): Settlement {
        val special = (declaration.incomeCents - BASIC_DEDUCTION_CENTS - declaration.taxableIncomeCents)
            .coerceAtLeast(0L)
        return settleWith(
            year = declaration.year,
            incomeCents = declaration.incomeCents,
            specialDeductionCents = special,
            taxPrepaidCents = declaration.taxPrepaidCents
        )
    }

    /** 供 UI 展示用：应退/应补的绝对值。 */
    fun absBalanceOf(settlement: Settlement): Long = abs(settlement.balanceCents)
}
