package com.elites.fullcharge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        // 계급 복구용 이전 세션 정보
        val LAST_SESSION_DURATION = longPreferencesKey("last_session_duration")
        val LAST_SESSION_TIMESTAMP = longPreferencesKey("last_session_timestamp")
        // 업적/통계
        val TOTAL_MESSAGES = intPreferencesKey("total_messages")
        val CRISIS_ESCAPE_COUNT = intPreferencesKey("crisis_escape_count")
        val HIGHEST_RANK = stringPreferencesKey("highest_rank")
        val UNLOCKED_ACHIEVEMENTS = stringSetPreferencesKey("unlocked_achievements")
        // 차단한 사용자 ID 목록
        val BLOCKED_USER_IDS = stringSetPreferencesKey("blocked_user_ids")
        // 연속 접속 (스트릭)
        val LAST_LOGIN_DATE = stringPreferencesKey("last_login_date")  // yyyy-MM-dd 형식
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        // 배신 횟수
        val BETRAYAL_COUNT = intPreferencesKey("betrayal_count")
    }

    companion object {
        // 세션 복구 유효 기간 (24시간)
        private const val SESSION_RESTORE_VALIDITY_MS = 24 * 60 * 60 * 1000L
        // 복구 가능 최소 계급 기준 (1분 = 이등병)
        const val MIN_RESTORE_DURATION_MS = 1 * 60 * 1000L
    }

    val userId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USER_ID] ?: UUID.randomUUID().toString().also { newId ->
            // 새 ID 생성 시 저장
        }
    }

    val nickname: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.NICKNAME] ?: ""
    }

    suspend fun initializeNickname(): String {
        var name: String? = null
        context.dataStore.edit { prefs ->
            name = prefs[Keys.NICKNAME]
            if (name.isNullOrBlank()) {
                name = generateRandomNickname()
                prefs[Keys.NICKNAME] = name!!
            }
        }
        return name!!
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

    suspend fun getNickname(): String {
        return context.dataStore.data.first()[Keys.NICKNAME] ?: ""
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

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
        }
    }

    /**
     * 복구 가능한 이전 세션 정보 (24시간 내, 이등병 이상일 때만)
     */
    val restorableSessionDuration: Flow<Long?> = context.dataStore.data.map { prefs ->
        val duration = prefs[Keys.LAST_SESSION_DURATION] ?: 0L
        val timestamp = prefs[Keys.LAST_SESSION_TIMESTAMP] ?: 0L
        val now = System.currentTimeMillis()

        // 24시간 이내이고, 이등병(1분) 이상인 경우에만 복구 가능
        if (duration >= MIN_RESTORE_DURATION_MS && (now - timestamp) < SESSION_RESTORE_VALIDITY_MS) {
            duration
        } else {
            null
        }
    }

    /**
     * 퇴장 시 현재 세션 저장 (복구용)
     */
    suspend fun saveSessionForRestore(durationMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SESSION_DURATION] = durationMs
            prefs[Keys.LAST_SESSION_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * 세션 복구 후 복구 정보 삭제
     */
    suspend fun clearRestorableSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SESSION_DURATION] = 0L
            prefs[Keys.LAST_SESSION_TIMESTAMP] = 0L
        }
    }

    // ========== 업적/통계 관련 ==========

    val userStats: Flow<UserStats> = context.dataStore.data.map { prefs ->
        UserStats(
            totalMessages = prefs[Keys.TOTAL_MESSAGES] ?: 0,
            totalSessionTimeMs = prefs[Keys.TOTAL_ELITE_TIME] ?: 0L,
            crisisEscapeCount = prefs[Keys.CRISIS_ESCAPE_COUNT] ?: 0,
            highestRank = prefs[Keys.HIGHEST_RANK] ?: EliteRank.TRAINEE.name,
            unlockedAchievements = prefs[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
        )
    }

    val unlockedAchievements: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
    }

    /**
     * 메시지 전송 시 카운트 증가
     */
    suspend fun incrementMessageCount(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            newCount = (prefs[Keys.TOTAL_MESSAGES] ?: 0) + 1
            prefs[Keys.TOTAL_MESSAGES] = newCount
        }
        return newCount
    }

    /**
     * 위기 탈출 시 카운트 증가
     */
    suspend fun incrementCrisisEscapeCount(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            newCount = (prefs[Keys.CRISIS_ESCAPE_COUNT] ?: 0) + 1
            prefs[Keys.CRISIS_ESCAPE_COUNT] = newCount
        }
        return newCount
    }

    /**
     * 최고 계급 업데이트
     */
    suspend fun updateHighestRank(rank: EliteRank) {
        context.dataStore.edit { prefs ->
            val currentHighest = prefs[Keys.HIGHEST_RANK]?.let {
                try { EliteRank.valueOf(it) } catch (e: Exception) { EliteRank.TRAINEE }
            } ?: EliteRank.TRAINEE

            if (rank.ordinal > currentHighest.ordinal) {
                prefs[Keys.HIGHEST_RANK] = rank.name
            }
        }
    }

    /**
     * 업적 해금
     */
    suspend fun unlockAchievement(achievement: Achievement): Boolean {
        var isNewUnlock = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.UNLOCKED_ACHIEVEMENTS] ?: emptySet()
            if (!current.contains(achievement.id)) {
                prefs[Keys.UNLOCKED_ACHIEVEMENTS] = current + achievement.id
                isNewUnlock = true
            }
        }
        return isNewUnlock
    }

    /**
     * 업적 해금 여부 확인
     */
    suspend fun hasAchievement(achievement: Achievement): Boolean {
        return unlockedAchievements.first().contains(achievement.id)
    }

    // ========== 차단 사용자 관련 ==========

    val blockedUserIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.BLOCKED_USER_IDS] ?: emptySet()
    }

    /**
     * 사용자 차단 (신고)
     */
    suspend fun blockUser(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_USER_IDS] ?: emptySet()
            prefs[Keys.BLOCKED_USER_IDS] = current + userId
        }
    }

    /**
     * 사용자 차단 해제
     */
    suspend fun unblockUser(userId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.BLOCKED_USER_IDS] ?: emptySet()
            prefs[Keys.BLOCKED_USER_IDS] = current - userId
        }
    }

    /**
     * 차단 목록 가져오기 (동기)
     */
    suspend fun getBlockedUserIds(): Set<String> {
        return blockedUserIds.first()
    }

    // ========== 연속 접속 (스트릭) 관련 ==========

    val currentStreak: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_STREAK] ?: 0
    }

    val longestStreak: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.LONGEST_STREAK] ?: 0
    }

    /**
     * 접속 시 스트릭 업데이트
     * @return 현재 스트릭 일수
     */
    suspend fun updateLoginStreak(): Int {
        var streak = 0
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        context.dataStore.edit { prefs ->
            val lastLoginDate = prefs[Keys.LAST_LOGIN_DATE] ?: ""
            val currentStreakValue = prefs[Keys.CURRENT_STREAK] ?: 0

            streak = when {
                lastLoginDate == today -> {
                    // 오늘 이미 접속함 - 스트릭 유지
                    currentStreakValue
                }
                isYesterday(lastLoginDate) -> {
                    // 어제 접속함 - 스트릭 증가
                    currentStreakValue + 1
                }
                else -> {
                    // 연속 접속 끊김 - 1일부터 다시 시작
                    1
                }
            }

            prefs[Keys.LAST_LOGIN_DATE] = today
            prefs[Keys.CURRENT_STREAK] = streak

            // 최장 스트릭 업데이트
            val longestStreakValue = prefs[Keys.LONGEST_STREAK] ?: 0
            if (streak > longestStreakValue) {
                prefs[Keys.LONGEST_STREAK] = streak
            }
        }
        return streak
    }

    private fun isYesterday(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return false
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val date = format.parse(dateStr) ?: return false
            val yesterday = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, -1)
            }.time
            format.format(date) == format.format(yesterday)
        } catch (e: Exception) {
            false
        }
    }

    // ========== 배신 횟수 관련 ==========

    val betrayalCount: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.BETRAYAL_COUNT] ?: 0
    }

    /**
     * 배신 횟수 증가 (배터리 방전으로 퇴장 시)
     */
    suspend fun incrementBetrayalCount(): Int {
        var newCount = 0
        context.dataStore.edit { prefs ->
            newCount = (prefs[Keys.BETRAYAL_COUNT] ?: 0) + 1
            prefs[Keys.BETRAYAL_COUNT] = newCount
        }
        return newCount
    }

    // ========== 충전 통계 데이터 클래스 ==========

    data class ChargingStats(
        val totalEliteTimeMs: Long = 0L,
        val longestSessionMs: Long = 0L,
        val currentStreak: Int = 0,
        val longestStreak: Int = 0,
        val betrayalCount: Int = 0,
        val totalMessages: Int = 0,
        val crisisEscapeCount: Int = 0,
        val highestRank: String = EliteRank.TRAINEE.name
    )

    val chargingStats: Flow<ChargingStats> = context.dataStore.data.map { prefs ->
        ChargingStats(
            totalEliteTimeMs = prefs[Keys.TOTAL_ELITE_TIME] ?: 0L,
            longestSessionMs = prefs[Keys.LONGEST_SESSION] ?: 0L,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            betrayalCount = prefs[Keys.BETRAYAL_COUNT] ?: 0,
            totalMessages = prefs[Keys.TOTAL_MESSAGES] ?: 0,
            crisisEscapeCount = prefs[Keys.CRISIS_ESCAPE_COUNT] ?: 0,
            highestRank = prefs[Keys.HIGHEST_RANK] ?: EliteRank.TRAINEE.name
        )
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
