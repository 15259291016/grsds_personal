package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 申报记录的编号生成与 JSON 往返，保证“申报记录查询”的数据可复现、可持久化。 */
class DeclarationRecordTest {

    private fun settlement(
        year: Int = 2026,
        income: Long = 16_001_00,
        payable: Long = 0L,
        prepaid: Long = 480_02,
        balance: Long = 480_02
    ) = TaxCalculator.Settlement(
        year = year,
        incomeCents = income,
        basicDeductionCents = TaxCalculator.BASIC_DEDUCTION_CENTS,
        specialDeductionCents = 24_000_00,
        taxableIncomeCents = 0L,
        taxRatePercent = 3,
        taxPayableCents = payable,
        taxPrepaidCents = prepaid,
        balanceCents = balance
    )

    @Test
    fun `declaration number is deterministic for the same timestamp`() {
        val first = DeclarationRecord.generateNo(2026, 1_794_817_100_085L)
        val second = DeclarationRecord.generateNo(2026, 1_794_817_100_085L)
        assertEquals(first, second)
        assertTrue(first.startsWith("HX2026"))
        // "HX2026" + 6 位时间戳尾数
        assertEquals("HX2026".length + 6, first.length)
    }

    @Test
    fun `declaration number keeps six digits when timestamp tail is short`() {
        assertEquals("HX2026000042", DeclarationRecord.generateNo(2026, 42L))
    }

    @Test
    fun `from settlement fills amount snapshot and refund flag`() {
        val record = DeclarationRecord.from(settlement(), submittedAt = 1_794_817_100_085L)
        assertEquals(2026, record.year)
        assertEquals(16_001_00L, record.incomeCents)
        assertEquals(480_02L, record.taxPrepaidCents)
        assertEquals(480_02L, record.balanceCents)
        assertTrue(record.isRefund)
        assertTrue(record.submittedAtLabel.startsWith("2026-11-16"))
    }

    @Test
    fun `payable declaration is not a refund`() {
        val record = DeclarationRecord.from(
            settlement(payable = 1_200_00, prepaid = 0L, balance = -1_200_00),
            submittedAt = 1_000L
        )
        assertFalse(record.isRefund)
        assertEquals(-1_200_00L, record.balanceCents)
    }

    @Test
    fun `codec round trips declarations and tolerates broken payload`() {
        val records = listOf(
            DeclarationRecord.from(settlement(), 1_794_817_100_085L),
            DeclarationRecord.from(settlement(year = 2025, income = 0L, prepaid = 0L, balance = 0L), 1_700_000_000_000L)
        )
        val decoded = AppPreferencesCodec.decodeDeclarations(
            AppPreferencesCodec.encodeDeclarations(records)
        )
        assertEquals(records, decoded)
        assertTrue(AppPreferencesCodec.decodeDeclarations(null).isEmpty())
        assertTrue(AppPreferencesCodec.decodeDeclarations("不是 json").isEmpty())
    }
}
