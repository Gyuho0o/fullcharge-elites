package com.elites.fullcharge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "elite_prefs")

class ElitePreferences(private val context: Context) {

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NICKNAME = stringPreferencesKey("nickname")
        val TOTAL_ELITE_TIME = longPreferencesKey("total_elite_time")
        val LONGEST_SESSION = longPreferencesKey("longest_session")
        val SESSION_START_TIME = longPreferencesKey("session_start_time")
    }

    val userId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ID] ?: UUID.randomUUID().toString().also { newId ->
            // 새 ID 생성 시 저장
        }
    }

    val nickname: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NICKNAME] ?: generateRandomNickname()
    }

    val totalEliteTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.TOTAL_ELITE_TIME] ?: 0L
    }

    val longestSession: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LONGEST_SESSION] ?: 0L
    }

    val sessionStartTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.SESSION_START_TIME] ?: 0L
    }

    suspend fun initializeUserId(): String {
        var id: String? = null
        context.dataStore.edit { prefs ->
            id = prefs[Keys.USER_ID]
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs[Keys.USER_ID] = id!!
            }
        }
        return id!!
    }

    suspend fun setNickname(nickname: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NICKNAME] = nickname
        }
    }

    suspend fun startSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.SESSION_START_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun endSession() {
        context.dataStore.edit { prefs ->
            val startTime = prefs[Keys.SESSION_START_TIME] ?: return@edit
            val sessionDuration = System.currentTimeMillis() - startTime

            // 총 시간 업데이트
            val totalTime = (prefs[Keys.TOTAL_ELITE_TIME] ?: 0L) + sessionDuration
            prefs[Keys.TOTAL_ELITE_TIME] = totalTime

            // 최장 세션 업데이트
            val longestSession = prefs[Keys.LONGEST_SESSION] ?: 0L
            if (sessionDuration > longestSession) {
                prefs[Keys.LONGEST_SESSION] = sessionDuration
            }

            // 세션 시작 시간 초기화
            prefs[Keys.SESSION_START_TIME] = 0L
        }
    }

    private fun generateRandomNickname(): String {
        val adjectives = listOf(
            "행복한", "즐거운", "신나는", "편안한", "여유로운",
            "활기찬", "기분좋은", "상쾌한", "따뜻한", "포근한"
        )
        val nouns = listOf(
            "고양이", "강아지", "토끼", "햄스터", "판다",
            "펭귄", "북극곰", "수달", "라쿤", "다람쥐"
        )
        return "${adjectives.random()} ${nouns.random()}"
    }
}
