package com.grsds.personal.data

import kotlin.math.abs
import kotlinx.serialization.Serializable

/**
 * 家庭成员：填报专项附加扣除（子女教育、赡养老人等）时需要的信息，
 * 独立落盘，供「家庭成员」页与后续扣除表单引用。
 */
@Serializable
data class FamilyMember(
    val id: String,
    /** 关系：配偶 / 子女 / 父母 等，自由取值，列表页按添加顺序展示。 */
    val relation: String,
    val name: String,
    /** 18 位身份证号。 */
    val idNumber: String,
    val updatedAt: Long
) {
    /** 列表展示用脱敏证件号：保留前 4 后 2。 */
    val maskedIdNumber: String
        get() = if (idNumber.length >= 8) {
            idNumber.take(4) + "**********" + idNumber.takeLast(2)
        } else {
            idNumber
        }

    /**
     * 从 18 位身份证号里取出生日期（yyyy-MM-dd）。
     * 填报子女教育/婴幼儿照护时要填出生日期，没必要让用户再手输一遍。
     */
    val birthDateLabel: String
        get() {
            if (idNumber.length != 18) return ""
            val month = idNumber.substring(10, 12).toIntOrNull() ?: return ""
            val day = idNumber.substring(12, 14).toIntOrNull() ?: return ""
            if (month !in 1..12 || day !in 1..31) return ""
            return "${idNumber.substring(6, 10)}-${idNumber.substring(10, 12)}-${idNumber.substring(12, 14)}"
        }

    companion object {

        /** 身份证号校验：18 位、前 17 位数字、末位数字或 X。 */
        fun idNumberError(input: String): String? = when {
            input.isBlank() -> "请输入身份证号"
            !Regex("^\\d{17}[0-9Xx]$").matches(input) -> "身份证号应为 18 位（末位可为 X）"
            else -> null
        }

        fun nameError(input: String): String? = when {
            input.isBlank() -> "请输入姓名"
            input.length > 20 -> "姓名过长"
            else -> null
        }

        fun generateId(name: String, now: Long): String =
            "fm-" + abs(name.hashCode()).toString(36) + "-" + now
    }
}
