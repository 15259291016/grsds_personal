package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 扣除分项表单的金额规则与必填校验。 */
class DeductionRulesTest {

    private val medical = DEDUCTION_OPTIONS.first { it.type == "大病医疗" }
    private val elder = DEDUCTION_OPTIONS.first { it.type == "赡养老人" }
    private val education = DEDUCTION_OPTIONS.first { it.type == "子女教育" }

    @Test
    fun `medical deduction starts above the threshold`() {
        assertEquals(0L, DeductionRules.medicalDeductibleCents(10_000_00L))
        assertEquals(0L, DeductionRules.medicalDeductibleCents(15_000_00L))
        assertEquals(5_000_00L, DeductionRules.medicalDeductibleCents(20_000_00L))
    }

    @Test
    fun `medical deduction is capped at 80000`() {
        assertEquals(80_000_00L, DeductionRules.medicalDeductibleCents(200_000_00L))
    }

    @Test
    fun `amount of fixed option follows the ratio`() {
        assertEquals(24_000_00L, DeductionRules.amountOf(education, 100, emptyMap()))
        assertEquals(12_000_00L, DeductionRules.amountOf(education, 50, emptyMap()))
    }

    @Test
    fun `non only child elder support is capped at 18000`() {
        val details = mapOf("only_child" to "否")
        assertEquals(18_000_00L, DeductionRules.effectiveAmountOf(elder, 100, details))
        // 独生子女仍按 36000 元全额扣除
        assertEquals(36_000_00L, DeductionRules.effectiveAmountOf(elder, 100, mapOf("only_child" to "是")))
    }

    @Test
    fun `pension contribution follows actual payment`() {
        val pension = DEDUCTION_OPTIONS.first { it.type == "个人养老金" }
        assertEquals(
            10_000_00L,
            DeductionRules.amountOf(pension, 100, mapOf("paid_amount" to "10000"))
        )
        // 超过年度上限按 12000 元封顶
        assertEquals(
            12_000_00L,
            DeductionRules.amountOf(pension, 100, mapOf("paid_amount" to "20000"))
        )
    }

    @Test
    fun `validate reports the first missing field`() {
        val result = DeductionRules.validate(education, emptyMap())
        assertNotNull(result)
        assertTrue(result!!.contains("子女姓名"))
    }

    @Test
    fun `validate rejects self paid larger than total`() {
        val details = mapOf(
            "patient_name" to "张三",
            "relation" to "本人",
            "total_amount" to "30000",
            "self_paid" to "40000"
        )
        val result = DeductionRules.validate(medical, details)
        assertNotNull(result)
        assertTrue(result!!.contains("不能大于"))
    }

    @Test
    fun `validate accepts a complete elder form`() {
        val details = mapOf(
            "elder_name" to "张父",
            "elder_id" to "110101195001011234",
            "elder_relation" to "父亲",
            "only_child" to "否",
            "share_type" to "均摊"
        )
        assertNull(DeductionRules.validate(elder, details))
    }
}
