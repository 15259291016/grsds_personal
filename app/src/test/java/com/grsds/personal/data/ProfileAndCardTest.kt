package com.grsds.personal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAndCardTest {

    // ---- 个人信息 ----------------------------------------------------------

    @Test
    fun `profile from same phone is deterministic`() {
        val a = ProfileInfo.fromPhone("13812345678")
        val b = ProfileInfo.fromPhone("13812345678")
        assertEquals(a, b)
    }

    @Test
    fun `profile from different phones differs`() {
        val a = ProfileInfo.fromPhone("13812345678")
        val b = ProfileInfo.fromPhone("13987654321")
        assertTrue(a.idNumber != b.idNumber || a.name != b.name)
    }

    @Test
    fun `profile generates 18 digit resident id`() {
        val profile = ProfileInfo.fromPhone("13812345678")
        assertEquals(18, profile.idNumber.length)
        assertTrue(profile.idNumber.all { it.isDigit() })
    }

    @Test
    fun `name masking keeps surname only`() {
        val profile = ProfileInfo.fromPhone("13812345678")
        assertEquals(profile.name.first(), profile.maskedName.first())
        assertTrue(profile.maskedName.length == profile.name.length)
        assertFalse(profile.maskedName.contains(profile.name.drop(1)))
    }

    @Test
    fun `id number masking keeps head six and tail four`() {
        val profile = ProfileInfo.fromPhone("13812345678")
        val masked = profile.maskedIdNumber
        assertEquals(profile.idNumber.take(6), masked.take(6))
        assertEquals(profile.idNumber.takeLast(4), masked.takeLast(4))
        assertTrue(masked.drop(6).dropLast(4).all { it == '*' })
    }

    @Test
    fun `profile codec round trip preserves contact fields`() {
        val profile = ProfileInfo.fromPhone("13812345678")
            .copy(email = "a@b.com", address = "北京市朝阳区")
        val encoded = AppPreferencesCodec.encodeProfile(profile)
        assertEquals(profile, AppPreferencesCodec.decodeProfile(encoded))
    }

    @Test
    fun `profile codec rejects garbage`() {
        assertNull(AppPreferencesCodec.decodeProfile("not-json"))
        assertNull(AppPreferencesCodec.decodeProfile(null))
    }

    // ---- 银行卡 ------------------------------------------------------------

    @Test
    fun `card number validation accepts 16 to 19 digits`() {
        assertTrue(BankCard.isValidCardNumber("6222021234567890"))
        assertTrue(BankCard.isValidCardNumber("62220212345678901"))    // 17 位也合法
        assertTrue(BankCard.isValidCardNumber("6222021234567890123"))
        assertFalse(BankCard.isValidCardNumber("622202123456789"))     // 15 位
    }

    @Test
    fun `card number validation rejects letters and symbols`() {
        assertFalse(BankCard.isValidCardNumber("622202123456789a"))
        assertFalse(BankCard.isValidCardNumber("6222 0212 3456 7890"))
        assertFalse(BankCard.isValidCardNumber(""))
    }

    @Test
    fun `masked number shows last four and bank`() {
        val card = BankCard(
            id = "c1", bankName = "中国工商银行", cardNumber = "6222021234567890",
            holderName = "张* ", boundAt = 0L
        )
        assertEquals("尾号 7890（中国工商银行）", card.maskedNumber)
    }

    @Test
    fun `bank cards codec round trip keeps default flag`() {
        val cards = listOf(
            BankCard("c1", "招商银行", "6225751234567890", "张*", 1L, true),
            BankCard("c2", "中国银行", "6217001234567890", "张*", 2L, false)
        )
        val decoded = AppPreferencesCodec.decodeBankCards(
            AppPreferencesCodec.encodeBankCards(cards)
        )
        assertEquals(cards, decoded)
        assertNotNull(decoded.firstOrNull { it.isDefault })
    }

    // ---- 更正 / 作废 ---------------------------------------------------------

    @Test
    fun `correction record keeps chain fields`() {
        val original = DeclarationRecord(
            id = "decl-2025-1", year = 2025, declarationNo = "HX2025000001",
            submittedAt = 1L, incomeCents = 100L, taxableIncomeCents = 50L,
            taxPayableCents = 10L, taxPrepaidCents = 20L, balanceCents = 10L
        )
        val corrected = original.copy(
            id = "decl-2025-2",
            status = "更正申报",
            correctionOf = original.declarationNo,
            correctionSeq = 1,
            incomeCents = 200L
        )
        assertEquals("已申报", original.status)
        assertTrue(original.isActive)
        assertTrue(corrected.isActive)
        assertEquals(1, corrected.correctionSeq)
        assertEquals(original.declarationNo, corrected.correctionOf)
        assertEquals("已更正", original.copy(status = "已更正").status)
        assertFalse(original.copy(status = "已作废").isActive)
    }

    @Test
    fun `declarations codec round trip keeps correction fields`() {
        val record = DeclarationRecord(
            id = "decl-2025-1", year = 2025, declarationNo = "HX2025000001",
            submittedAt = 1L, incomeCents = 100L, taxableIncomeCents = 50L,
            taxPayableCents = 10L, taxPrepaidCents = 20L, balanceCents = 10L,
            status = "更正申报", correctionOf = "HX2025000000", correctionSeq = 2
        )
        val decoded = AppPreferencesCodec.decodeDeclarations(
            AppPreferencesCodec.encodeDeclarations(listOf(record))
        )
        assertEquals(record, decoded.first())
    }

    @Test
    fun `legacy record without correction fields decodes with defaults`() {
        // 老版本落盘的记录没有 correctionOf / correctionSeq，升级后要能读回
        val legacy = """
            [{"id":"d","year":2025,"declarationNo":"HX2025000001","submittedAt":1,
              "incomeCents":100,"taxableIncomeCents":50,"taxPayableCents":10,
              "taxPrepaidCents":20,"balanceCents":10}]
        """.trimIndent()
        val decoded = AppPreferencesCodec.decodeDeclarations(legacy)
        assertEquals(1, decoded.size)
        assertNull(decoded.first().correctionOf)
        assertEquals(0, decoded.first().correctionSeq)
        assertEquals("已申报", decoded.first().status)
    }

    // ---- 更正重算 -----------------------------------------------------------

    @Test
    fun `settleWith matches settle for same inputs`() {
        val records = listOf(
            IncomeRecord("a", 2025, 1, "工资薪金", 12_000_000L, "测试公司", 0L)
        )
        val deductions = listOf(DeductionRecord("子女教育", 2025, 50, 12_000_00L, 0L))
        val viaRecords = TaxCalculator.settle(records, deductions, 2025)
        val viaAmounts = TaxCalculator.settleWith(
            2025, viaRecords.incomeCents, viaRecords.specialDeductionCents, viaRecords.taxPrepaidCents
        )
        assertEquals(viaRecords, viaAmounts)
    }
}
