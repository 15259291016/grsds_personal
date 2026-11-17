package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCalendarTest {

    @Test
    fun `汇算办理期前一年为未开始`() {
        // 2026-11-17 时，2026 年度要等到 2027-03-01 才开始
        assertEquals(
            TaxCalendar.Window.NotOpen,
            TaxCalendar.settlementWindow(2026, Triple(2026, 11, 17))
        )
        assertEquals(
            TaxCalendar.Window.NotOpen,
            TaxCalendar.settlementWindow(2026, Triple(2027, 2, 28))
        )
    }

    @Test
    fun `汇算办理期当年为进行中`() {
        assertEquals(
            TaxCalendar.Window.Open,
            TaxCalendar.settlementWindow(2025, Triple(2026, 3, 1))
        )
        assertEquals(
            TaxCalendar.Window.Open,
            TaxCalendar.settlementWindow(2025, Triple(2026, 6, 30))
        )
    }

    @Test
    fun `汇算办理期结束后为已结束`() {
        assertEquals(
            TaxCalendar.Window.Closed,
            TaxCalendar.settlementWindow(2025, Triple(2026, 7, 1))
        )
        // 首页大卡「已于 6 月 30 日结束」的场景
        assertEquals(
            TaxCalendar.Window.Closed,
            TaxCalendar.settlementWindow(2025, Triple(2026, 11, 17))
        )
    }

    @Test
    fun `跨年后的补办年度仍算已结束`() {
        assertEquals(
            TaxCalendar.Window.Closed,
            TaxCalendar.settlementWindow(2024, Triple(2026, 11, 17))
        )
    }

    @Test
    fun `专项附加扣除确认在当年12月`() {
        assertEquals(
            TaxCalendar.Window.NotOpen,
            TaxCalendar.deductionConfirmWindow(2027, Triple(2026, 11, 17))
        )
        assertEquals(
            TaxCalendar.Window.Open,
            TaxCalendar.deductionConfirmWindow(2027, Triple(2026, 12, 3))
        )
        assertEquals(
            TaxCalendar.Window.Closed,
            TaxCalendar.deductionConfirmWindow(2026, Triple(2026, 11, 17))
        )
    }

    @Test
    fun `截止日按年月日逐级比较`() {
        assertFalse(TaxCalendar.isPast(2026, 6, 30, Triple(2026, 6, 30)))
        assertTrue(TaxCalendar.isPast(2026, 6, 30, Triple(2026, 7, 1)))
        assertTrue(TaxCalendar.isPast(2025, 12, 31, Triple(2026, 1, 1)))
        assertFalse(TaxCalendar.isPast(2026, 1, 31, Triple(2025, 12, 31)))
        // 日不同不能只看月份
        assertTrue(TaxCalendar.isPast(2026, 1, 10, Triple(2026, 1, 31)))
    }
}
