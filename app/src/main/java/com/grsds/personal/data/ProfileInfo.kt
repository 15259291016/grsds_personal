package com.grsds.personal.data

import kotlinx.serialization.Serializable

/**
 * 个人信息：登录后由手机号生成一份演示档案。
 * 姓名、证件号、出生日期等实名要素在真实 APP 里由税务系统带出、不可修改，
 * 这里也做成只读；只有电子邮箱与联系地址允许保存（对应官方「联系信息」可编辑）。
 */
@Serializable
data class ProfileInfo(
    val name: String,
    val idType: String = "居民身份证",
    val idNumber: String,
    val phone: String,
    val birthDate: String,
    val residence: String,
    val employer: String,
    val email: String = "",
    val address: String = ""
) {
    /** 张* 形式的脱敏姓名：保留姓氏，两字姓名补一个 *。 */
    val maskedName: String
        get() = when {
            name.isEmpty() -> ""
            name.length == 1 -> name
            name.length == 2 -> "${name.first()}*"
            else -> "${name.first()}${"*".repeat(name.length - 1)}"
        }

    /** 110101********1234 形式的脱敏证件号：保留前 6 位与后 4 位。 */
    val maskedIdNumber: String
        get() = when {
            idNumber.length <= 10 -> idNumber
            else -> idNumber.take(6) + "*".repeat(idNumber.length - 10) + idNumber.takeLast(4)
        }

    val maskedPhone: String
        get() = if (phone.length == 11) "${phone.take(3)}****${phone.takeLast(4)}" else phone

    companion object {
        /**
         * 由登录手机号生成一份确定性的演示档案：
         * 同一个手机号永远得到同一份信息，换手机号重新登录会换一份。
         */
        fun fromPhone(phone: String): ProfileInfo {
            val seed = phone.filter { it.isDigit() }.takeLast(8).padStart(8, '0').toInt()
            val surnames = listOf("王", "李", "张", "刘", "陈", "杨", "赵", "周", "吴", "徐")
            val given = listOf("伟", "芳", "静", "磊", "军", "洋", "勇", "艳", "杰", "娟")
            val surname = surnames[seed / 1000 % surnames.size]
            val name = if (seed % 10 < 5) surname + given[seed / 100 % given.size]
            else surname + given[seed / 100 % given.size] + given[seed % given.size]
            val areaCode = (seed % 900 + 100).toString()
            // 18 位身份证：6 位地区码 + 8 位出生日期 + 4 位顺序与校验
            val idSeq = (seed % 10_000).toString().padStart(4, '0')
            val birthYear = 1970 + seed % 30
            val birthMonth = seed / 100 % 12 + 1
            val birthDay = seed / 10000 % 28 + 1
            val employers = listOf(
                "北京华信科技有限公司", "上海锦明信息技术有限公司",
                "广州晟达贸易有限公司", "深圳市恒远网络技术有限公司"
            )
            val residences = listOf(
                "北京市朝阳区", "上海市浦东新区", "广州市天河区", "深圳市南山区", "杭州市西湖区"
            )
            return ProfileInfo(
                name = name,
                idNumber = "110$areaCode${birthYear}${birthMonth.toString().padStart(2, '0')}${birthDay.toString().padStart(2, '0')}$idSeq",
                phone = phone,
                birthDate = "${birthYear}年${birthMonth}月${birthDay}日",
                residence = residences[seed / 10 % residences.size],
                employer = employers[seed % employers.size]
            )
        }
    }
}
