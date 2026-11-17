package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 家庭成员 / 任职受雇的校验规则与脱敏展示。 */
class ProfileArchiveTest {

    // -- FamilyMember -------------------------------------------------------

    @Test
    fun `身份证号为空提示必填`() {
        assertEquals("请输入身份证号", FamilyMember.idNumberError(""))
        assertEquals("请输入身份证号", FamilyMember.idNumberError("   "))
    }

    @Test
    fun `身份证号格式错误提示`() {
        assertNotNull(FamilyMember.idNumberError("1101011950010112"))
        assertNotNull(FamilyMember.idNumberError("11010119500101123AB"))
        assertNotNull(FamilyMember.idNumberError("1101011950010112345678"))
    }

    @Test
    fun `18位身份证与末位X均通过`() {
        assertNull(FamilyMember.idNumberError("110101195001011234"))
        assertNull(FamilyMember.idNumberError("11010119500101123X"))
        assertNull(FamilyMember.idNumberError("11010119500101123x"))
    }

    @Test
    fun `姓名为空或过长报错`() {
        assertEquals("请输入姓名", FamilyMember.nameError(""))
        assertNotNull(FamilyMember.nameError("一".repeat(21)))
        assertNull(FamilyMember.nameError("张付"))
    }

    @Test
    fun `证件号脱敏保留前4后2`() {
        val member = FamilyMember(
            id = "fm-1",
            relation = "父母",
            name = "张付",
            idNumber = "110101195001011234",
            updatedAt = 0L
        )
        assertEquals("1101**********34", member.maskedIdNumber)
    }

    @Test
    fun `同证件号视为同一成员`() {
        val a = FamilyMember("fm-1", "配偶", "李敏", "110101198001011234", 0L)
        val b = FamilyMember("fm-2", "配偶", "李敏", "110101198001011234", 1L)
        val c = FamilyMember("fm-3", "父母", "张付", "110101195001011234", 2L)
        assertTrue(a.idNumber == b.idNumber)
        assertFalse(a.idNumber == c.idNumber)
    }

    // -- EmploymentRecord ---------------------------------------------------

    @Test
    fun `年月格式校验`() {
        assertNull(EmploymentRecord.monthError("2023-05"))
        assertEquals("请输入年月", EmploymentRecord.monthError(""))
        assertNull(EmploymentRecord.monthError("", required = false))
        assertNotNull(EmploymentRecord.monthError("2023-13"))
        assertNotNull(EmploymentRecord.monthError("2023-5"))
        assertNotNull(EmploymentRecord.monthError("202305"))
    }

    @Test
    fun `单位名称校验`() {
        assertEquals("请输入单位名称", EmploymentRecord.unitNameError(""))
        assertNotNull(EmploymentRecord.unitNameError("公司".repeat(21)))
        assertNull(EmploymentRecord.unitNameError("北京示例科技有限公司"))
    }

    @Test
    fun `信用代码选填且18位合法`() {
        assertNull(EmploymentRecord.taxNoError(""))
        assertNull(EmploymentRecord.taxNoError("91110108MA01T8G123"))
        assertNotNull(EmploymentRecord.taxNoError("91110108MA01T8G1"))
        assertNotNull(EmploymentRecord.taxNoError("91110108MA01T8G12I"))
    }

    @Test
    fun `在职期间展示含至今`() {
        val current = EmploymentRecord(
            id = "emp-1",
            unitName = "北京示例科技有限公司",
            startMonth = "2023-05",
            updatedAt = 0L
        )
        assertTrue(current.isCurrent)
        assertEquals("2023-05 至今", current.periodLabel)

        val past = current.copy(endMonth = "2024-08")
        assertFalse(past.isCurrent)
        assertEquals("2023-05 至 2024-08", past.periodLabel)
    }
}
