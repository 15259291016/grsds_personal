package com.grsds.personal.data

import java.util.Calendar

/**
 * 个税办理期的时间判定。
 *
 * 真实 APP 里「能不能办」完全由当天日期决定：综合所得年度汇算的办理期是
 * 次年 3 月 1 日至 6 月 30 日，专项附加扣除确认是每年 12 月。之前这些状态
 * 全写死在文案里，导致 11 月还显示「去办理」、2026 年度还显示「可办理」，
 * 和首页「已于 6 月 30 日结束」自相矛盾。这里统一按系统日期算。
 */
object TaxCalendar {

    enum class Window {
        /** 办理期还没开始 */
        NotOpen,

        /** 正在办理期内 */
        Open,

        /** 办理期已结束（年度汇算仍可补办） */
        Closed
    }

    internal fun today(): Triple<Int, Int, Int> {
        val now = Calendar.getInstance()
        return Triple(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH) + 1,
            now.get(Calendar.DAY_OF_MONTH)
        )
    }

    /** 某年度的综合所得年度汇算办理期：次年的 3 月 1 日 ~ 6 月 30 日。 */
    fun settlementWindow(year: Int): Window = settlementWindow(year, today())

    /** 同上，但显式传入「今天」，方便单测覆盖跨年边界。 */
    internal fun settlementWindow(year: Int, today: Triple<Int, Int, Int>): Window {
        val (y, m, d) = today
        val startYear = year + 1
        return when {
            y < startYear -> Window.NotOpen
            y > startYear -> Window.Closed
            m < 3 -> Window.NotOpen
            m <= 5 -> Window.Open
            m == 6 && d <= 30 -> Window.Open
            else -> Window.Closed
        }
    }

    /** 某年度的专项附加扣除确认：上年 12 月 1 日 ~ 12 月 31 日。 */
    fun deductionConfirmWindow(year: Int): Window = deductionConfirmWindow(year, today())

    /** 同上，显式传入「今天」，便于单测。 */
    internal fun deductionConfirmWindow(year: Int, today: Triple<Int, Int, Int>): Window {
        val (y, m) = today
        val startYear = year - 1
        return when {
            y < startYear -> Window.NotOpen
            y > startYear -> Window.Closed
            m < 12 -> Window.NotOpen
            else -> Window.Open
        }
    }

    /** 用「年-月-日」判断是否已过期，供待办列表这类带明确截止日的事项使用。 */
    fun isPast(year: Int, month: Int, day: Int): Boolean = isPast(year, month, day, today())

    /** 同上，显式传入「今天」，便于单测。 */
    internal fun isPast(year: Int, month: Int, day: Int, today: Triple<Int, Int, Int>): Boolean {
        val (y, m, d) = today
        return when {
            y != year -> y > year
            m != month -> m > month
            else -> d > day
        }
    }
}
