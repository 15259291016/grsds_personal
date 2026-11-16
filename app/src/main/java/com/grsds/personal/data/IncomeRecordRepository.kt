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

/** 记录列表与 JSON 之间的转换，隔离持久化细节，方便单测。 */
internal object IncomeRecordCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val serializer = ListSerializer(IncomeRecord.serializer())

    fun encode(records: List<IncomeRecord>): String = json.encodeToString(serializer, records)

    fun decode(raw: String?): List<IncomeRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: SerializationException) {
            emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }
}

private val Context.incomeRecordDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "income_records"
)

/**
 * 收入纳税明细的本地仓库，基于 DataStore Preferences + JSON 序列化。
 * 列表整体写入，读侧返回 [Flow]，界面用 collectAsState 订阅即可自动刷新。
 */
class IncomeRecordRepository(private val dataStore: DataStore<Preferences>) {

    private val recordsKey = stringPreferencesKey("records_json")

    val records: Flow<List<IncomeRecord>> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> IncomeRecordCodec.decode(preferences[recordsKey]) }

    suspend fun addAll(newRecords: List<IncomeRecord>) {
        if (newRecords.isEmpty()) return
        dataStore.edit { preferences ->
            val existing = IncomeRecordCodec.decode(preferences[recordsKey])
            preferences[recordsKey] = IncomeRecordCodec.encode(newRecords + existing)
        }
    }

    suspend fun delete(id: String) {
        dataStore.edit { preferences ->
            val remaining = IncomeRecordCodec.decode(preferences[recordsKey]).filterNot { it.id == id }
            preferences[recordsKey] = IncomeRecordCodec.encode(remaining)
        }
    }

    /** 按 id 覆盖写入，用于“编辑已录入的明细”。id 不存在时不做任何事。 */
    suspend fun update(record: IncomeRecord) {
        dataStore.edit { preferences ->
            val records = IncomeRecordCodec.decode(preferences[recordsKey])
            if (records.none { it.id == record.id }) return@edit
            preferences[recordsKey] = IncomeRecordCodec.encode(
                records.map { if (it.id == record.id) record else it }
            )
        }
    }

    /** 标记为“已提出异议申诉”，重复调用无副作用。 */
    suspend fun markDisputed(id: String) {
        dataStore.edit { preferences ->
            val records = IncomeRecordCodec.decode(preferences[recordsKey])
            if (records.none { it.id == id && !it.disputed }) return@edit
            preferences[recordsKey] = IncomeRecordCodec.encode(
                records.map { if (it.id == id) it.copy(disputed = true) else it }
            )
        }
    }

    suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(recordsKey) }
    }

    companion object {
        @Volatile
        private var instance: IncomeRecordRepository? = null

        fun get(context: Context): IncomeRecordRepository =
            instance ?: synchronized(this) {
                instance ?: IncomeRecordRepository(
                    context.applicationContext.incomeRecordDataStore
                ).also { instance = it }
            }
    }
}
