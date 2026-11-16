package com.example.myapplication.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomeRecordCodecTest {

    private val sample = listOf(
        IncomeRecord("a", 2025, 3, "工资薪金", 1_234_56L, "某某科技有限公司", 1_700_000_000_000L),
        IncomeRecord("b", 2025, 4, "稿酬", 800_00L, "未填写扣缴义务人", 1_700_000_000_001L)
    )

    @Test
    fun `encode then decode keeps every field`() {
        val decoded = IncomeRecordCodec.decode(IncomeRecordCodec.encode(sample))

        assertEquals(sample, decoded)
    }

    @Test
    fun `decode returns empty list for missing or corrupted payload`() {
        assertTrue(IncomeRecordCodec.decode(null).isEmpty())
        assertTrue(IncomeRecordCodec.decode("   ").isEmpty())
        assertTrue(IncomeRecordCodec.decode("not a json").isEmpty())
        assertTrue(IncomeRecordCodec.decode("[]").isEmpty())
    }

    @Test
    fun `records written before the disputed field still decode as not disputed`() {
        val legacyJson = """
            [{"id":"legacy","year":2025,"month":6,"incomeType":"劳务报酬",
              "amountInCents":50000,"withholdingAgent":"某公司","createdAt":1700000000000}]
        """.trimIndent()

        val decoded = IncomeRecordCodec.decode(legacyJson)

        assertEquals(1, decoded.size)
        assertEquals(false, decoded.first().disputed)
        assertEquals(50_000L, decoded.first().amountInCents)
    }

    @Test
    fun `disputed flag survives a round trip`() {
        val disputed = sample.map { it.copy(disputed = true) }

        assertEquals(disputed, IncomeRecordCodec.decode(IncomeRecordCodec.encode(disputed)))
    }

    @Test
    fun `parseYuanToCents converts input to cents`() {
        assertEquals(1_234_56L, parseYuanToCents("1234.56"))
        assertEquals(123_450L, parseYuanToCents("1,234.5"))
        assertEquals(1_00L, parseYuanToCents(" 1 "))
        assertNull(parseYuanToCents(""))
        assertNull(parseYuanToCents("0"))
        assertNull(parseYuanToCents("-5"))
        assertNull(parseYuanToCents("abc"))
        assertNull(parseYuanToCents("200000000"))
    }

    @Test
    fun `formatYuan renders grouped amount with two decimals`() {
        assertEquals("1,234.56", formatYuan(1_234_56L))
        assertEquals("0.80", formatYuan(80L))
        assertEquals("0.00", formatYuan(0L))
    }
}
