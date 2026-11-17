package com.grsds.personal.data

import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * 任职受雇信息：任职过的单位及在职期间，独立落盘，
 * 用于「任职受雇信息」页核对收入明细与汇算数据。
 */
@Serializable
data class EmploymentRecord(
    val id: String,
    /** 单位名称。 */
    val unitName: String,
    /** 统一社会信用代码，选填。 */
    val taxNo: String = "",
    /** 任职受雇起始年月，yyyy-MM。 */
    val startMonth: String,
    /** 空串表示至今在职。 */
    val endMonth: String = "",
    /** 职务，选填。 */
    val position: String = "",
    val updatedAt: Long
) {
    /** 是否当前在职。 */
    val isCurrent: Boolean get() = endMonth.isBlank()

    /** 展示用任职期间：2023-05 至今 / 2021-03 至 2024-08。 */
    val periodLabel: String
        get() = if (isCurrent) "$startMonth 至今" else "$startMonth 至 $endMonth"

    companion object {

        /** yyyy-MM 校验：月份 01-12。 */
        fun monthError(input: String, required: Boolean = true): String? = when {
            input.isBlank() -> if (required) "请输入年月" else null
            !Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(input) -> "年月格式应为 yyyy-MM"
            else -> null
        }

        fun unitNameError(input: String): String? = when {
            input.isBlank() -> "请输入单位名称"
            input.length > 40 -> "单位名称过长"
            else -> null
        }

        /** 统一社会信用代码：18 位数字或大写字母，选填。 */
        fun taxNoError(input: String): String? = when {
            input.isBlank() -> null
            !Regex("^[0-9A-HJ-NPQRTUWXY]{18}$").matches(input) -> "信用代码应为 18 位数字或字母"
            else -> null
        }

        fun generateId(unitName: String, now: Long): String =
            "emp-" + abs(unitName.hashCode()).toString(36) + "-" + now
    }
}
