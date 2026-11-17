package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 首页提醒条取数规则与「家庭成员带入扣除表单」的映射。 */
class FamilyLinkAndNoticeTest {

    private fun record(
        id: String = "decl-1",
        year: Int = 2025,
        balanceCents: Long = 120_000,
        status: String = "已申报",
        processStatus: String = ""
    ) = DeclarationRecord(
        id = id,
        year = year,
        declarationNo = "HX$year" + id.takeLast(3),
        submittedAt = 1_765_000_000_000,
        incomeCents = 20_000_000,
        taxableIncomeCents = 12_000_000,
        taxPayableCents = 1_000_000,
        taxPrepaidCents = 1_000_000 + balanceCents,
        balanceCents = balanceCents,
        status = status,
        processStatus = processStatus
    )

    private fun member(id: String, relation: String, name: String, idNumber: String) =
        FamilyMember(id = id, relation = relation, name = name, idNumber = idNumber, updatedAt = 0L)

    // -- SettlementNotices --------------------------------------------------

    @Test
    fun `没有申报时不提示`() {
        assertNull(SettlementNotices.top(emptyList()))
    }

    @Test
    fun `退税未申请时提示去申请`() {
        val notice = SettlementNotices.top(listOf(record(balanceCents = 88_800)))
        assertEquals(SettlementNoticeKind.REFUND_TO_APPLY, notice?.kind)
        assertEquals("decl-1", notice?.declarationId)
    }

    @Test
    fun `补税未缴款时提示去缴款`() {
        val notice = SettlementNotices.top(listOf(record(balanceCents = -50_000)))
        assertEquals(SettlementNoticeKind.TAX_TO_PAY, notice?.kind)
    }

    @Test
    fun `退税办理中提示查看进度`() {
        val notice = SettlementNotices.top(
            listOf(record(processStatus = SettlementProcess.AUDITING))
        )
        assertEquals(SettlementNoticeKind.REFUND_IN_PROGRESS, notice?.kind)
    }

    @Test
    fun `待申请退税优先于待缴款与办理中`() {
        val notice = SettlementNotices.top(
            listOf(
                record(id = "a", year = 2024, balanceCents = -30_000),
                record(id = "b", year = 2026, processStatus = SettlementProcess.TREASURY),
                record(id = "c", year = 2025, balanceCents = 66_600)
            )
        )
        assertEquals(SettlementNoticeKind.REFUND_TO_APPLY, notice?.kind)
        assertEquals("c", notice?.declarationId)
    }

    @Test
    fun `办结的申报不再提示`() {
        assertNull(
            SettlementNotices.top(
                listOf(
                    record(id = "a", balanceCents = 10_000, processStatus = SettlementProcess.REFUNDED),
                    record(id = "b", balanceCents = -10_000, processStatus = SettlementProcess.PAID)
                )
            )
        )
    }

    @Test
    fun `作废的申报不参与提醒`() {
        assertNull(SettlementNotices.top(listOf(record(status = "已作废"))))
    }

    // -- FamilyMember.birthDateLabel ---------------------------------------

    @Test
    fun `身份证号可解析出生日期`() {
        assertEquals("2015-09-01", member("m1", "子女", "小明", "110101201509010011").birthDateLabel)
    }

    @Test
    fun `非法证件号不解析出生日期`() {
        assertEquals("", member("m1", "子女", "小明", "1101012015").birthDateLabel)
        assertEquals("", member("m1", "子女", "小明", "110101201513010011").birthDateLabel)
    }

    // -- FamilyLink ---------------------------------------------------------

    @Test
    fun `子女教育只列出子女`() {
        val members = listOf(
            member("m1", "配偶", "李某", "110101199001010011"),
            member("m2", "子女", "小明", "110101201509010011"),
            member("m3", "父亲", "张某", "110101195001010011")
        )
        assertEquals(listOf("m2"), FamilyLink.candidates("子女教育", members).map { it.id })
        assertEquals(listOf("m2"), FamilyLink.candidates("3岁以下婴幼儿照护", members).map { it.id })
    }

    @Test
    fun `赡养老人列出父母与祖父母`() {
        val members = listOf(
            member("m1", "配偶", "李某", "110101199001010011"),
            member("m2", "子女", "小明", "110101201509010011"),
            member("m3", "父亲", "张某", "110101195001010011"),
            member("m4", "祖父母", "王某", "110101194001010011")
        )
        assertEquals(listOf("m3", "m4"), FamilyLink.candidates("赡养老人", members).map { it.id })
    }

    @Test
    fun `不支持带入的项目返回空候选`() {
        val members = listOf(member("m1", "子女", "小明", "110101201509010011"))
        assertFalse(FamilyLink.supports("住房租金"))
        assertTrue(FamilyLink.candidates("住房租金", members).isEmpty())
        assertTrue(FamilyLink.prefill("住房租金", members.first()).isEmpty())
    }

    @Test
    fun `子女教育带入姓名证件号与出生日期`() {
        val child = member("m1", "子女", "小明", "110101201509010011")
        val filled = FamilyLink.prefill("子女教育", child)
        assertEquals("小明", filled["child_name"])
        assertEquals("110101201509010011", filled["child_id"])
        assertEquals("2015-09-01", filled["child_birth"])
    }

    @Test
    fun `赡养老人带入关系映射`() {
        val father = member("m1", "父亲", "张某", "110101195001010011")
        val grandpa = member("m2", "祖父母", "王某", "110101194001010011")
        assertEquals("父亲", FamilyLink.prefill("赡养老人", father)["elder_relation"])
        assertEquals("其他法定赡养人", FamilyLink.prefill("赡养老人", grandpa)["elder_relation"])
    }

    @Test
    fun `大病医疗带入患者姓名与关系`() {
        val spouse = member("m1", "配偶", "李某", "110101199001010011")
        assertEquals("配偶", FamilyLink.prefill("大病医疗", spouse)["relation"])
        assertEquals("李某", FamilyLink.prefill("大病医疗", spouse)["patient_name"])
    }

    @Test
    fun `带入的关系取值都在表单候选项内`() {
        val relations = listOf("配偶", "子女", "父亲", "母亲", "父母", "祖父母", "外祖父母", "其他")
        val elderOptions = DEDUCTION_FIELDS.getValue("赡养老人")
            .first { it.key == "elder_relation" }.options
        val medicalOptions = DEDUCTION_FIELDS.getValue("大病医疗")
            .first { it.key == "relation" }.options
        relations.forEach { relation ->
            assertTrue(
                "赡养老人关系 $relation 不在候选项内",
                FamilyLink.elderRelationOf(relation) in elderOptions
            )
            assertTrue(
                "大病医疗关系 $relation 不在候选项内",
                FamilyLink.medicalRelationOf(relation) in medicalOptions
            )
        }
    }
}
