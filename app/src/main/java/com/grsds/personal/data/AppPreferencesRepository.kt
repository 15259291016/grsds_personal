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
}

private val Context.appPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_preferences"
)

/** 登录会话 + 专项附加扣除的本地仓库。 */
class AppPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private val sessionKey = stringPreferencesKey("login_session_json")
    private val deductionsKey = stringPreferencesKey("deductions_json")
    private val declarationsKey = stringPreferencesKey("declarations_json")

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
     * 提交申报并落盘。同一年度重复提交时保留最早那条，直接返回已有记录，
     * 避免用户来回点“提交申报”刷出多条重复申报。
     */
    suspend fun submitDeclaration(record: DeclarationRecord): DeclarationRecord {
        var saved = record
        dataStore.edit { preferences ->
            val existing = AppPreferencesCodec.decodeDeclarations(preferences[declarationsKey])
            val sameYear = existing.firstOrNull { it.year == record.year }
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
