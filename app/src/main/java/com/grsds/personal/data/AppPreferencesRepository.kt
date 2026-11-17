package com.grsds.personal.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** 会话与扣除记录的编解码，单独抽出来便于单测。 */
internal object AppPreferencesCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val deductionSerializer = ListSerializer(DeductionRecord.serializer())
    private val declarationSerializer = ListSerializer(DeclarationRecord.serializer())
    private val bankCardSerializer = ListSerializer(BankCard.serializer())
    private val taxRecordIssueSerializer = ListSerializer(TaxRecordIssue.serializer())

    fun encodeSession(session: LoginSession): String = json.encodeToString(LoginSession.serializer(), session)

    fun decodeSession(raw: String?): LoginSession? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(LoginSession.serializer(), raw)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun encodeDeductions(records: List<DeductionRecord>): String =
        json.encodeToString(deductionSerializer, records)

    fun decodeDeductions(raw: String?): List<DeductionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(deductionSerializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    fun encodeDeclarations(records: List<DeclarationRecord>): String =
        json.encodeToString(declarationSerializer, records)

    fun decodeDeclarations(raw: String?): List<DeclarationRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(declarationSerializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    fun encodeProfile(profile: ProfileInfo): String =
        json.encodeToString(ProfileInfo.serializer(), profile)

    fun decodeProfile(raw: String?): ProfileInfo? {
        if (raw.isNullOrBlank()) return null
        return try {
            json.decodeFromString(ProfileInfo.serializer(), raw)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun encodeTaxRecordIssues(records: List<TaxRecordIssue>): String =
        json.encodeToString(taxRecordIssueSerializer, records)

    fun decodeTaxRecordIssues(raw: String?): List<TaxRecordIssue> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(taxRecordIssueSerializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }

    fun encodeBankCards(cards: List<BankCard>): String =
        json.encodeToString(bankCardSerializer, cards)

    fun decodeBankCards(raw: String?): List<BankCard> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(bankCardSerializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

/** 登录会话 + 专项附加扣除的本地仓库。 */
class AppPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val sessionKey = stringPreferencesKey("login_session_json")
    private val deductionsKey = stringPreferencesKey("deductions_json")
    private val declarationsKey = stringPreferencesKey("declarations_json")
    private val profileKey = stringPreferencesKey("profile_info_json")
    private val bankCardsKey = stringPreferencesKey("bank_cards_json")
    private val taxRecordIssuesKey = stringPreferencesKey("tax_record_issues_json")

    val session: Flow<LoginSession?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> AppPreferencesCodec.decodeSession(preferences[sessionKey]) }

    val deductions: Flow<List<DeductionRecord>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> AppPreferencesCodec.decodeDeductions(preferences[deductionsKey]) }

    suspend fun login(session: LoginSession) {
        dataStore.edit { preferences ->
            preferences[sessionKey] = AppPreferencesCodec.encodeSession(session)
        }
    }

    suspend fun logout() {
        dataStore.edit { preferences -> preferences.remove(sessionKey) }
    }

    /** 同一年度同一类型的扣除只保留一条，重复填报视为修改。 */
    suspend fun saveDeduction(record: DeductionRecord) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeductions(preferences[deductionsKey])
            val merged = existing.filterNot {
                it.type == record.type && it.year == record.year
            } + record
            preferences[deductionsKey] = AppPreferencesCodec.encodeDeductions(merged)
        }
    }

    suspend fun removeDeduction(type: String, year: Int) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeductions(preferences[deductionsKey])
            preferences[deductionsKey] = AppPreferencesCodec.encodeDeductions(
                existing.filterNot { it.type == type && it.year == year }
            )
        }
    }

    /** 已提交的年度汇算申报，按提交时间倒序。 */
    val declarations: Flow<List<DeclarationRecord>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
                .sortedByDescending { it.submittedAt }
        }

    /**
     * 提交申报并落盘。同年份已有生效记录（未作废、未更正）时直接返回已有记录，
     * 避免用户来回点“提交申报”刷出多条重复申报；已作废或已被更正的不阻塞。
     */
    suspend fun submitDeclaration(record: DeclarationRecord): DeclarationRecord {
        var saved = record
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            val sameYear = existing.firstOrNull { it.year == record.year && it.isActive }
            if (sameYear != null) {
                saved = sameYear
            } else {
                preferences[declarationsKey] =
                    AppPreferencesCodec.encodeDeclarations(existing + record)
            }
        }
        return saved
    }

    /** 清空申报记录（设置页“清空演示数据”用）。 */
    suspend fun clearDeclarations() {
        dataStore.edit { preferences -> preferences.remove(declarationsKey) }
    }

    // -- 个人信息 -----------------------------------------------------------

    /**
     * 个人信息：登录后按手机号生成演示档案；未登录时为 null。
     * 首次登录时生成一次，之后只更新可编辑字段，与官方“实名要素只读”一致。
     */
    val profile: Flow<ProfileInfo?> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            val session = AppPreferencesCodec.decodeSession(preferences[sessionKey])
            val stored = AppPreferencesCodec.decodeProfile(preferences[profileKey])
            when {
                session == null -> null
                stored != null && stored.phone == session.phone -> stored
                else -> ProfileInfo.fromPhone(session.phone)
            }
        }

    /** 保存联系信息（邮箱、地址）；写入前先保证档案存在，避免未登录时落脏数据。 */
    suspend fun saveContactInfo(phone: String, email: String, address: String) {
        dataStore.edit { preferences ->
            val stored = AppPreferencesCodec.decodeProfile(preferences[profileKey])
            val base = (stored?.takeIf { it.phone == phone }) ?: ProfileInfo.fromPhone(phone)
            preferences[profileKey] =
                AppPreferencesCodec.encodeProfile(base.copy(email = email, address = address))
        }
    }

    suspend fun clearProfile() {
        dataStore.edit { preferences -> preferences.remove(profileKey) }
    }

    // -- 银行卡 -------------------------------------------------------------

    val bankCards: Flow<List<BankCard>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> AppPreferencesCodec.decodeBankCards(preferences[bankCardsKey]) }

    /**
     * 绑定一张卡。首张卡自动成为默认退税收款账户，
     * 后续绑卡保持现有默认卡不动（与官方行为一致）。
     */
    suspend fun addBankCard(card: BankCard) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeBankCards(preferences[bankCardsKey])
            val withDefault = if (existing.isEmpty()) card.copy(isDefault = true) else card
            preferences[bankCardsKey] =
                AppPreferencesCodec.encodeBankCards(existing + withDefault)
        }
    }

    /** 解绑：若是默认卡，默认顺延给剩余卡中最早绑定的一张。 */
    suspend fun removeBankCard(id: String) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeBankCards(preferences[bankCardsKey])
            val remaining = existing.filterNot { it.id == id }
            val promoted = if (existing.firstOrNull { it.id == id }?.isDefault == true) {
                val target = remaining.minByOrNull { it.boundAt }
                if (target != null) remaining.map { if (it.id == target.id) it.copy(isDefault = true) else it }
                else remaining
            } else {
                remaining
            }
            preferences[bankCardsKey] = AppPreferencesCodec.encodeBankCards(promoted)
        }
    }

    /** 设为默认退税收款账户：同一时刻只保留一张默认卡。 */
    suspend fun setDefaultBankCard(id: String) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeBankCards(preferences[bankCardsKey])
            preferences[bankCardsKey] = AppPreferencesCodec.encodeBankCards(
                existing.map { it.copy(isDefault = it.id == id) }
            )
        }
    }

    suspend fun clearBankCards() {
        dataStore.edit { preferences -> preferences.remove(bankCardsKey) }
    }

    // -- 纳税记录开具 ---------------------------------------------------------

    /** 已开具的纳税记录，按开具时间倒序。 */
    val taxRecordIssues: Flow<List<TaxRecordIssue>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppPreferencesCodec.decodeTaxRecordIssues(preferences[taxRecordIssuesKey])
                .sortedByDescending { it.issuedAt }
        }

    /** 开具一条年度纳税记录并落盘。 */
    suspend fun issueTaxRecord(year: Int, now: Long): TaxRecordIssue {
        val created = TaxRecordIssue.create(year, now)
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeTaxRecordIssues(preferences[taxRecordIssuesKey])
            preferences[taxRecordIssuesKey] =
                AppPreferencesCodec.encodeTaxRecordIssues(listOf(created) + existing)
        }
        return created
    }

    suspend fun clearTaxRecordIssues() {
        dataStore.edit { preferences -> preferences.remove(taxRecordIssuesKey) }
    }

    // -- 更正 / 作废 ----------------------------------------------------------

    /**
     * 提交更正：原申报置为“已更正”，追加新的“更正申报”记录。
     * 逐条打标而不是按年度打标，保证多次更正能串成链。
     */
    suspend fun correctDeclaration(originalId: String, record: DeclarationRecord) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            val marked = existing.map {
                if (it.id == originalId) it.copy(status = "已更正") else it
            }
            preferences[declarationsKey] =
                AppPreferencesCodec.encodeDeclarations(marked + record)
        }
    }

    /** 作废申报：状态置为“已作废”，不再参与统计，也不可再次更正。 */
    suspend fun voidDeclaration(id: String) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            preferences[declarationsKey] = AppPreferencesCodec.encodeDeclarations(
                existing.map {
                    if (it.id == id) it.copy(status = "已作废") else it
                }
            )
        }
    }

    // -- 退税 / 补税办理 ------------------------------------------------------

    /** 提交退税申请：记录收款账户，状态从空白进入“已申请退税”。 */
    suspend fun applyRefund(id: String, cardId: String, cardLabel: String, at: Long) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            preferences[declarationsKey] = AppPreferencesCodec.encodeDeclarations(
                existing.map {
                    if (it.id == id && !it.hasAppliedRefund) {
                        it.copy(
                            processStatus = SettlementProcess.REFUND_APPLIED,
                            bankCardId = cardId,
                            bankCardLabel = cardLabel,
                            processedAt = at
                        )
                    } else {
                        it
                    }
                }
            )
        }
    }

    /** 推进退税进度：已申请 → 审核 → 国库 → 退库。已完结时保持不变。 */
    suspend fun advanceRefund(id: String) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            preferences[declarationsKey] = AppPreferencesCodec.encodeDeclarations(
                existing.map {
                    if (it.id == id && it.hasAppliedRefund) {
                        val next = SettlementProcess.next(it.processStatus)
                        if (next == it.processStatus) it else it.copy(processStatus = next)
                    } else {
                        it
                    }
                }
            )
        }
    }

    /** 补税缴款完成：写入缴款凭证号与时间。 */
    suspend fun markPaid(id: String, paymentNo: String, at: Long) {
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            preferences[declarationsKey] = AppPreferencesCodec.encodeDeclarations(
                existing.map {
                    if (it.id == id) {
                        it.copy(processStatus = SettlementProcess.PAID, paymentNo = paymentNo, processedAt = at)
                    } else {
                        it
                    }
                }
            )
        }
    }

    /** 清空全部年度的扣除填报。 */
    suspend fun clearDeductions() {
        dataStore.edit { preferences -> preferences.remove(deductionsKey) }
    }

    companion object {
        @Volatile
        private var instance: AppPreferencesRepository? = null

        fun get(context: Context): AppPreferencesRepository =
            instance ?: synchronized(this) {
                instance ?: AppPreferencesRepository(
                    context.applicationContext.appPreferencesDataStore
                ).also { instance = it }
            }
    }
}
