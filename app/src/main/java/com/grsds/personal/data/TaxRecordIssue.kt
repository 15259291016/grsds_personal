package com.grsds.personal.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlinx.serialization.Serializable

/** 一条已开具的个人所得税纳税记录。 */
@Serializable
data class TaxRecordIssue(
    val id: String,
    val year: Int,
    val issuedAt: Long,
    val verifyCode: String
) {
    val issuedAtLabel: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(issuedAt))

    companion object {
        /** 生成 8 位验真码，模拟官方记录的二维码验真参数。 */
        fun generateVerifyCode(random: Random = Random()): String {
            val chars = "0123456789ABCDEF"
            return buildString {
                repeat(8) { append(chars[random.nextInt(chars.length)]) }
            }
        }

        fun create(year: Int, now: Long): TaxRecordIssue = TaxRecordIssue(
            id = "issue-$year-$now",
            year = year,
            issuedAt = now,
            verifyCode = generateVerifyCode()
        )
    }
}
