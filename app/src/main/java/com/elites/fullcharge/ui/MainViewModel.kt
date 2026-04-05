package com.elites.fullcharge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elites.fullcharge.ElitesApplication
import com.elites.fullcharge.data.*
import com.elites.fullcharge.notification.EliteMessagingService
import com.elites.fullcharge.util.ContentFilter
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Calendar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

enum class AppScreen {
    GATEKEEPER,
    CHAT,
    EXILE
}

data class MainUiState(
    val currentScreen: AppScreen = AppScreen.GATEKEEPER,
    val batteryState: BatteryState = BatteryState(),
    val userId: String = "",
    val nickname: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val onlineUsers: List<EliteUser> = emptyList(),
    val allTimeRecords: List<AllTimeRecord> = emptyList(),
    val onlineUserCount: Int = 0,
    val sessionStartTime: Long = 0L,
    val sessionDuration: Long = 0L,
    val isInChat: Boolean = false,
    // 관리자 모드
    val isAdminMode: Boolean = false,
    val showAdminLoginDialog: Boolean = false,
    val reports: List<Report> = emptyList(),  // 신고 목록
    // 위험 카운트다운 관련
    val isInDanger: Boolean = false,
    val dangerCountdown: Int = 0,  // 남은 초
    val dangerStartTime: Long = 0L,
    // 필터링 에러 메시지
    val filterErrorMessage: String? = null,
    // 답장 중인 메시지
    val replyingTo: ChatMessage? = null,
    // 온보딩 완료 여부
    val onboardingCompleted: Boolean = false,
    // 계급 복구 가능 여부 (이전 세션 시간)
    val restorableSessionDuration: Long? = null,
    // 업적 관련
    val newlyUnlockedAchievement: Achievement? = null,
    val unlockedAchievements: Set<String> = emptySet(),
    // 위기 탈출 축하
    val showCrisisEscapeCelebration: Boolean = false,
    // 실시간 이벤트 알림
    val latestChatEvent: ChatEvent? = null,
    // 콤보 시스템
    val comboState: ComboState = ComboState(),
    // 시간 기반 이벤트
    val personalHourMilestone: Int? = null,  // 달성한 시간 (1, 2, 3...)
    val isHourlyChime: Boolean = false,
    val isMidnightSpecial: Boolean = false
) {
    companion object {
        const val DANGER_COUNTDOWN_SECONDS = 10  // 10초 카운트다운
        // 봇 발동 시간 범위 (2~4분 랜덤)
        const val BOT_SILENCE_MIN_MS = 2 * 60 * 1000L
        const val BOT_SILENCE_MAX_MS = 4 * 60 * 1000L
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val batteryManager = BatteryStatusManager(application)
    private val chatRepository = ChatRepository()
    private val preferences = (application as ElitesApplication).preferences

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var sessionTimerJob: Job? = null
    private var batteryMonitorJob: Job? = null
    private var dangerCountdownJob: Job? = null
    private var activityUpdateJob: Job? = null
    private var timeEventJob: Job? = null
    private var botContentJob: Job? = null
    private var reportsObserverJob: Job? = null

    // 시간 기반 이벤트 추적
    private var lastCheckedHour = -1

    // 봇 콘텐츠 관련
    private var lastMessageTime = 0L
    private var lastPersonalHour = -1L
    private var currentBotThreshold = getRandomBotThreshold()  // 랜덤 타이밍

    private fun getRandomBotThreshold(): Long {
        return (MainUiState.BOT_SILENCE_MIN_MS..MainUiState.BOT_SILENCE_MAX_MS).random()
    }

    // 도배 방지용
    private var lastSentMessage: String = ""
    private var consecutiveCount: Int = 0
    private var lastSentTime: Long = 0L  // 쿨다운용
    private val recentMessageTimes = mutableListOf<Long>()  // 시간당 제한용

    init {
        // 온보딩은 매번 앱 시작 시 표시 (onboardingCompleted = false가 기본값)

        // 해금된 업적 관찰
        viewModelScope.launch {
            preferences.unlockedAchievements.collect { achievements ->
                _uiState.update { it.copy(unlockedAchievements = achievements) }
            }
        }

        // 복구 가능한 세션 관찰
        viewModelScope.launch {
            preferences.restorableSessionDuration.collect { duration ->
                _uiState.update { it.copy(restorableSessionDuration = duration) }
            }
        }

        // 사용자 ID 및 닉네임 초기화
        viewModelScope.launch {
            val userId = preferences.initializeUserId()
            val nickname = preferences.initializeNickname()
            _uiState.update { it.copy(userId = userId, nickname = nickname) }
        }

        // 닉네임 변경 관찰 (관리자 모드가 아닐 때만)
        viewModelScope.launch {
            preferences.nickname.collect { nickname ->
                if (nickname.isNotBlank() && !_uiState.value.isAdminMode) {
                    _uiState.update { it.copy(nickname = nickname) }
                }
            }
        }

        // 온라인 사용자 수 관찰
        viewModelScope.launch {
            chatRepository.getOnlineUserCount().collect { count ->
                _uiState.update { it.copy(onlineUserCount = count) }
            }
        }

        // 역대 랭킹 관찰
        viewModelScope.launch {
            chatRepository.getAllTimeRecords().collect { records ->
                _uiState.update { it.copy(allTimeRecords = records) }
            }
        }

        // 초기 배터리 상태 확인
        updateBatteryStatus()

        // 배터리 상태 관찰
        viewModelScope.launch {
            batteryManager.batteryState.collect { state ->
                val currentState = _uiState.value
                val isInChat = currentState.isInChat
                val isAdminMode = currentState.isAdminMode
                val wasInDanger = currentState.isInDanger

                _uiState.update { it.copy(batteryState = state) }

                // 관리자 모드는 배터리 체크 안 함
                if (isInChat && !isAdminMode) {
                    // 배터리가 100% 미만 + 충전 안 하면 위험 모드 시작
                    if (state.level < 100 && !state.isCharging && !wasInDanger) {
                        startDangerCountdown()
                    }

                    // 다시 충전 시작하면 위험 모드 해제 (살았다!)
                    if (state.isCharging && wasInDanger) {
                        cancelDangerCountdown()
                    }
                }
            }
        }
    }

    private fun startDangerCountdown() {
        dangerCountdownJob?.cancel()

        val startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isInDanger = true,
                dangerStartTime = startTime,
                dangerCountdown = MainUiState.DANGER_COUNTDOWN_SECONDS
            )
        }

        dangerCountdownJob = viewModelScope.launch {
            var remaining = MainUiState.DANGER_COUNTDOWN_SECONDS
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(dangerCountdown = remaining) }

                // 충전 시작하면 카운트다운 취소됨 (batteryState collect에서 처리)
            }

            // 카운트다운 종료 - 퇴장
            triggerExile(reason = "시간 초과!")
        }
    }

    private fun cancelDangerCountdown() {
        dangerCountdownJob?.cancel()

        val currentState = _uiState.value
        val dangerDuration = System.currentTimeMillis() - currentState.dangerStartTime

        // 위기 상태가 최소 2초 이상 지속된 경우에만 축하 이펙트 표시
        // (입장 직후 배터리 상태 변동으로 인한 잘못된 감지 방지)
        val showCelebration = currentState.dangerStartTime > 0 && dangerDuration >= 2000

        if (showCelebration) {
            // 위기 탈출 성공! 축하 이펙트 표시 및 업적 체크
            val nickname = currentState.nickname
            viewModelScope.launch {
                val escapeCount = preferences.incrementCrisisEscapeCount()
                checkCrisisEscapeAchievements(escapeCount)

                // 다른 사용자에게도 보이는 시스템 메시지 전송
                chatRepository.sendSystemMessage("${nickname}님이 위기에서 탈출했습니다! ⚡")
            }

            // 로컬 축하 이벤트 (본인에게만)
            emitChatEvent(ChatEvent.UserCrisisEscape(nickname))
        }

        _uiState.update {
            it.copy(
                isInDanger = false,
                dangerCountdown = 0,
                dangerStartTime = 0L,
                showCrisisEscapeCelebration = showCelebration
            )
        }
    }

    fun dismissCrisisEscapeCelebration() {
        _uiState.update { it.copy(showCrisisEscapeCelebration = false) }
    }

    fun updateBatteryStatus() {
        batteryManager.updateBatteryStatus()
    }

    fun updateBatteryFromIntent(intent: android.content.Intent) {
        batteryManager.updateFromIntent(intent)
    }

    fun enterChat() {
        val state = _uiState.value
        // UI에서 버튼이 활성화된 상태로 클릭됐으면 입장 허용
        // (isElite 체크는 UI에서 이미 함)

        viewModelScope.launch {
            // 세션 시작 시간 설정
            val startTime = System.currentTimeMillis()

            // 입장 시간 설정 (이 시간 이후 메시지만 표시)
            chatRepository.setJoinedTime(startTime)

            // Firebase에 입장 등록
            chatRepository.joinChat(state.userId, state.nickname)

            // FCM 토큰 등록
            registerFcmToken(state.userId)

            // 입장 알림 시스템 메시지 전송
            chatRepository.sendSystemMessage("${state.nickname}님이 전우회에 합류했습니다")

            // 세션 시작
            preferences.startSession()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.CHAT,
                    isInChat = true,
                    sessionStartTime = startTime
                )
            }

            // 메시지 관찰 시작
            startMessageObservation()

            // 온라인 사용자 관찰 시작
            startOnlineUsersObservation()

            // 세션 타이머 시작
            startSessionTimer()

            // 배터리 모니터링 강화
            startBatteryMonitoring()

            // 활동 시간 주기적 업데이트 (온라인 상태 유지)
            startActivityUpdate()

            // 시간 기반 이벤트 모니터링 시작
            startTimeEventMonitoring()

            // 봇 콘텐츠 타이머 시작
            startBotContentTimer()
        }
    }

    /**
     * 광고 시청 후 이전 계급을 유지하며 입장
     */
    fun enterChatWithRestore(previousDuration: Long) {
        val state = _uiState.value

        viewModelScope.launch {
            // 복구 정보 삭제 (한 번만 사용 가능)
            preferences.clearRestorableSession()

            // 세션 시작 시간을 과거로 설정 (이전 세션 시간만큼 더해서)
            val adjustedStartTime = System.currentTimeMillis() - previousDuration

            // 입장 시간 설정 (이 시간 이후 메시지만 표시)
            chatRepository.setJoinedTime(System.currentTimeMillis())

            // Firebase에 입장 등록
            chatRepository.joinChat(state.userId, state.nickname)

            // 입장 알림 시스템 메시지 전송
            val rank = EliteRank.fromDuration(previousDuration)
            chatRepository.sendSystemMessage("${state.nickname}(${rank.koreanName})님이 전우회에 복귀했습니다")

            // 세션 시작 (조정된 시간으로)
            preferences.startSession()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.CHAT,
                    isInChat = true,
                    sessionStartTime = adjustedStartTime,
                    sessionDuration = previousDuration
                )
            }

            // 메시지 관찰 시작
            startMessageObservation()

            // 온라인 사용자 관찰 시작
            startOnlineUsersObservation()

            // 세션 타이머 시작
            startSessionTimer()

            // 배터리 모니터링 강화
            startBatteryMonitoring()

            // 활동 시간 주기적 업데이트
            startActivityUpdate()

            // 시간 기반 이벤트 모니터링 시작
            startTimeEventMonitoring()

            // 봇 콘텐츠 타이머 시작
            startBotContentTimer()
        }
    }

    /**
     * 복구 가능한 세션 포기 (광고 안 보고 그냥 입장)
     */
    fun dismissRestorableSession() {
        viewModelScope.launch {
            preferences.clearRestorableSession()
        }
    }

    private fun startMessageObservation() {
        viewModelScope.launch {
            var previousMessageCount = 0
            chatRepository.getMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }

                // 새 메시지가 있으면 봇 타이머 리셋 (시스템 메시지 제외)
                val nonSystemMessages = messages.filter { !it.isSystemMessage }
                if (nonSystemMessages.size > previousMessageCount) {
                    resetBotTimer()
                }
                previousMessageCount = nonSystemMessages.size

                // 다른 사람의 메시지를 읽음 처리
                val userId = _uiState.value.userId
                val unreadMessageIds = messages
                    .filter { it.userId != userId && !it.readBy.contains(userId) && !it.isSystemMessage }
                    .map { it.id }

                if (unreadMessageIds.isNotEmpty()) {
                    markMessagesAsRead(unreadMessageIds)
                }
            }
        }
    }

    private fun markMessagesAsRead(messageIds: List<String>) {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            chatRepository.markMessagesAsRead(messageIds, userId)
        }
    }

    private fun startOnlineUsersObservation() {
        viewModelScope.launch {
            chatRepository.getOnlineUsers().collect { users ->
                _uiState.update { it.copy(onlineUsers = users) }
            }
        }
    }

    private var lastCheckedMinute = -1L
    private var previousRank: EliteRank? = null

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        lastCheckedMinute = -1L
        previousRank = null

        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val startTime = _uiState.value.sessionStartTime
                if (startTime > 0) {
                    val duration = System.currentTimeMillis() - startTime
                    val currentRank = EliteRank.fromDuration(duration)

                    _uiState.update { it.copy(sessionDuration = duration) }

                    // 승급 이벤트 발생 (다른 사용자에게 보이는 알림)
                    if (previousRank != null && currentRank != previousRank && currentRank.ordinal > previousRank!!.ordinal) {
                        val nickname = _uiState.value.nickname
                        emitChatEvent(ChatEvent.UserRankUp(nickname, currentRank))
                    }
                    previousRank = currentRank

                    // 1분마다 업적 체크 (성능 최적화)
                    val currentMinute = duration / 60000
                    if (currentMinute > lastCheckedMinute) {
                        lastCheckedMinute = currentMinute
                        checkSurvivalAchievements(duration)
                        checkRankAchievements(currentRank)
                    }
                }
            }
        }
    }

    private fun startBatteryMonitoring() {
        batteryMonitorJob?.cancel()
        batteryMonitorJob = viewModelScope.launch {
            while (_uiState.value.isInChat) {
                updateBatteryStatus()
                delay(500) // 0.5초마다 배터리 체크
            }
        }
    }

    private fun startActivityUpdate() {
        activityUpdateJob?.cancel()
        activityUpdateJob = viewModelScope.launch {
            while (_uiState.value.isInChat) {
                // 30초마다 활동 시간 업데이트
                chatRepository.updateUserActivity(_uiState.value.userId)
                delay(30_000)
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentTime = System.currentTimeMillis()

        // 도배 방지 1: 쿨다운 (1초)
        if (currentTime - lastSentTime < COOLDOWN_MS) {
            _uiState.update { it.copy(filterErrorMessage = "메시지를 너무 빨리 보내고 있어요") }
            return
        }

        // 도배 방지 2: 시간당 제한 (10초에 5개)
        recentMessageTimes.removeAll { currentTime - it > RATE_LIMIT_WINDOW_MS }
        if (recentMessageTimes.size >= RATE_LIMIT_MAX_MESSAGES) {
            val waitTime = (RATE_LIMIT_WINDOW_MS - (currentTime - recentMessageTimes.first())) / 1000
            _uiState.update { it.copy(filterErrorMessage = "잠시 후 다시 시도해주세요 (${waitTime}초)") }
            return
        }

        // 도배 방지 3: 같은 메시지 연속 3회 이상 차단
        val trimmedText = text.trim()
        if (trimmedText == lastSentMessage) {
            consecutiveCount++
            if (consecutiveCount >= 3) {
                _uiState.update { it.copy(filterErrorMessage = "같은 메시지를 연속으로 보낼 수 없어요") }
                return
            }
        } else {
            lastSentMessage = trimmedText
            consecutiveCount = 1
        }

        // 콘텐츠 필터링
        val filterResult = ContentFilter.filterMessage(text)
        if (!filterResult.isAllowed) {
            _uiState.update { it.copy(filterErrorMessage = filterResult.reason) }
            return
        }

        val state = _uiState.value
        val rank = EliteRank.fromDuration(state.sessionDuration)
        val replyingTo = state.replyingTo

        // @멘션 파싱 - @닉네임 형태에서 userId 추출
        val mentionPattern = Regex("@([^\\s]+)")
        val mentionedNicknames = mentionPattern.findAll(text).map { it.groupValues[1] }.toList()
        val mentionedUserIds = state.onlineUsers
            .filter { user -> mentionedNicknames.contains(user.nickname) }
            .map { it.userId }

        // 도배 방지 기록 업데이트
        lastSentTime = currentTime
        recentMessageTimes.add(currentTime)

        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                userId = state.userId,
                nickname = state.nickname,
                message = text,
                timestamp = System.currentTimeMillis(),
                rank = rank.name,
                replyToId = replyingTo?.id,
                replyToNickname = replyingTo?.nickname,
                replyToMessage = replyingTo?.message?.take(50),  // 미리보기는 50자까지만
                mentions = mentionedUserIds,
                warning = filterResult.warning  // 스팸/광고 경고
            )
            chatRepository.sendMessage(message)

            // 메시지 카운트 증가 및 업적 체크
            val messageCount = preferences.incrementMessageCount()
            checkMessageAchievements(messageCount)

            // 콤보 업데이트
            updateCombo()

            // 봇 타이머 리셋 (새 메시지 전송 시)
            resetBotTimer()

            // 봇 반응 체크 (유저 메시지에 봇이 반응)
            checkBotReaction(text)

            // 답장 상태 초기화
            _uiState.update { it.copy(replyingTo = null) }
        }
    }

    fun setReplyingTo(message: ChatMessage) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun clearReplyingTo() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun clearFilterError() {
        _uiState.update { it.copy(filterErrorMessage = null) }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            chatRepository.toggleReaction(messageId, emoji, _uiState.value.userId)
        }
    }

    fun createPoll(question: String, options: List<String>, durationMinutes: Int = 5) {
        if (question.isBlank() || options.size < 2) return

        val state = _uiState.value
        val rank = EliteRank.fromDuration(state.sessionDuration)

        viewModelScope.launch {
            chatRepository.sendPollMessage(
                userId = state.userId,
                nickname = state.nickname,
                rank = rank.name,
                question = question,
                options = options,
                durationMinutes = durationMinutes
            )
        }
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        viewModelScope.launch {
            chatRepository.votePoll(messageId, optionIndex, _uiState.value.userId)
        }
    }

    fun reportMessage(
        message: ChatMessage,
        reason: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val state = _uiState.value

            // 이미 신고했는지 체크
            if (chatRepository.hasAlreadyReported(message.id, state.userId)) {
                onResult(false)
                return@launch
            }

            val report = Report(
                messageId = message.id,
                messageContent = message.message,
                reportedUserId = message.userId,
                reportedNickname = message.nickname,
                reporterUserId = state.userId,
                reporterNickname = state.nickname,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )

            val success = chatRepository.reportMessage(report)
            onResult(success)
        }
    }

    private fun triggerExile(reason: String = "") {
        dangerCountdownJob?.cancel()

        viewModelScope.launch {
            val state = _uiState.value
            val wasAdminMode = state.isAdminMode

            // 관리자가 아닌 경우만 역대 기록 업데이트
            if (!wasAdminMode && state.sessionDuration > 0) {
                chatRepository.updateAllTimeRecord(
                    userId = state.userId,
                    nickname = state.nickname,
                    durationMillis = state.sessionDuration
                )
            }

            // 관리자가 아닌 경우만 세션 복구용으로 저장
            if (!wasAdminMode && state.sessionDuration >= ElitePreferences.MIN_RESTORE_DURATION_MS) {
                preferences.saveSessionForRestore(state.sessionDuration)
            }

            // 퇴장 알림 시스템 메시지 전송
            chatRepository.sendSystemMessage("${state.nickname}님이 전우회를 배신했습니다")

            // Firebase에서 퇴장 처리
            chatRepository.leaveChat(state.userId)

            // 세션 종료
            preferences.endSession()

            // 타이머 정지
            sessionTimerJob?.cancel()
            batteryMonitorJob?.cancel()
            activityUpdateJob?.cancel()
            timeEventJob?.cancel()
            botContentJob?.cancel()
            reportsObserverJob?.cancel()

            // 추방 화면으로 이동
            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.EXILE,
                    isInChat = false,
                    isInDanger = false,
                    dangerCountdown = 0
                )
            }
        }
    }

    fun returnToGatekeeper() {
        // 배터리 상태 먼저 업데이트
        updateBatteryStatus()

        _uiState.update {
            it.copy(
                currentScreen = AppScreen.GATEKEEPER,
                messages = emptyList(),
                sessionStartTime = 0L,
                sessionDuration = 0L
            )
        }
    }

    fun leaveChat() {
        viewModelScope.launch {
            val state = _uiState.value
            val wasAdminMode = state.isAdminMode

            // 관리자가 아닌 경우만 역대 기록 업데이트
            if (!wasAdminMode && state.sessionDuration > 0) {
                chatRepository.updateAllTimeRecord(
                    userId = state.userId,
                    nickname = state.nickname,
                    durationMillis = state.sessionDuration
                )
            }

            // 관리자가 아닌 경우만 세션 복구용으로 저장
            if (!wasAdminMode && state.sessionDuration >= ElitePreferences.MIN_RESTORE_DURATION_MS) {
                preferences.saveSessionForRestore(state.sessionDuration)
            }

            // 관리자가 아닌 경우만 퇴장 알림 시스템 메시지 전송
            if (!wasAdminMode) {
                chatRepository.sendSystemMessage("${state.nickname}님이 퇴장했습니다")
            }

            // FCM 토큰 삭제
            unregisterFcmToken(state.userId)

            // Firebase에서 퇴장 처리
            chatRepository.leaveChat(state.userId)

            // 세션 종료
            preferences.endSession()

            // 타이머 정지
            sessionTimerJob?.cancel()
            batteryMonitorJob?.cancel()
            dangerCountdownJob?.cancel()
            activityUpdateJob?.cancel()
            timeEventJob?.cancel()
            botContentJob?.cancel()
            reportsObserverJob?.cancel()

            // 관리자였으면 원래 닉네임 복원
            val restoredNickname = if (wasAdminMode) {
                preferences.getNickname()
            } else {
                state.nickname
            }

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.GATEKEEPER,
                    isInChat = false,
                    isInDanger = false,
                    dangerCountdown = 0,
                    messages = emptyList(),
                    sessionStartTime = 0L,
                    sessionDuration = 0L,
                    comboState = ComboState(),  // 콤보 리셋
                    isAdminMode = false,  // 관리자 모드 비활성화
                    nickname = restoredNickname  // 닉네임 복원
                )
            }
        }
    }

    fun completeOnboarding() {
        // 로컬 상태만 업데이트 (매번 앱 시작 시 온보딩 표시를 위해 저장하지 않음)
        _uiState.update { it.copy(onboardingCompleted = true) }
    }

    fun changeNickname(newNickname: String) {
        // 관리자 모드에서는 닉네임 변경 불가
        if (_uiState.value.isAdminMode) {
            _uiState.update { it.copy(filterErrorMessage = "관리자 모드에서는 닉네임을 변경할 수 없습니다") }
            return
        }

        // 닉네임 필터링
        val filterResult = ContentFilter.filterNickname(newNickname)
        if (!filterResult.isAllowed) {
            _uiState.update { it.copy(filterErrorMessage = filterResult.reason) }
            return
        }

        viewModelScope.launch {
            preferences.setNickname(newNickname)

            // 채팅 중이면 Firebase도 업데이트
            val state = _uiState.value
            if (state.isInChat) {
                chatRepository.updateUserNickname(state.userId, newNickname)
            }
        }
    }

    // ========== 업적 관련 ==========

    private suspend fun checkMessageAchievements(messageCount: Int) {
        val achievementToCheck = when (messageCount) {
            1 -> Achievement.FIRST_MESSAGE
            10 -> Achievement.CHAT_10
            50 -> Achievement.CHAT_50
            100 -> Achievement.CHAT_100
            else -> null
        }

        achievementToCheck?.let { unlockAchievement(it) }
    }

    private suspend fun checkCrisisEscapeAchievements(escapeCount: Int) {
        val achievementToCheck = when (escapeCount) {
            1 -> Achievement.CRISIS_ESCAPE_1
            5 -> Achievement.CRISIS_ESCAPE_5
            10 -> Achievement.CRISIS_ESCAPE_10
            else -> null
        }

        achievementToCheck?.let { unlockAchievement(it) }
    }

    private suspend fun checkSurvivalAchievements(durationMs: Long) {
        val minutes = durationMs / 60000

        val achievementsToCheck = mutableListOf<Achievement>()

        if (minutes >= 30) achievementsToCheck.add(Achievement.SURVIVOR_30M)
        if (minutes >= 60) achievementsToCheck.add(Achievement.SURVIVOR_1H)
        if (minutes >= 180) achievementsToCheck.add(Achievement.SURVIVOR_3H)
        if (minutes >= 360) achievementsToCheck.add(Achievement.SURVIVOR_6H)

        achievementsToCheck.forEach { unlockAchievement(it) }
    }

    private suspend fun checkRankAchievements(rank: EliteRank) {
        preferences.updateHighestRank(rank)

        val achievementToCheck = when (rank) {
            EliteRank.STAFF_SERGEANT -> Achievement.RANK_NCO
            EliteRank.SECOND_LIEUTENANT -> Achievement.RANK_OFFICER
            EliteRank.BRIGADIER_GENERAL -> Achievement.RANK_GENERAL
            else -> null
        }

        achievementToCheck?.let { unlockAchievement(it) }
    }

    private suspend fun unlockAchievement(achievement: Achievement) {
        val isNew = preferences.unlockAchievement(achievement)
        if (isNew) {
            _uiState.update { it.copy(newlyUnlockedAchievement = achievement) }
        }
    }

    fun dismissAchievementPopup() {
        _uiState.update { it.copy(newlyUnlockedAchievement = null) }
    }

    // ========== 실시간 이벤트 ==========

    private fun emitChatEvent(event: ChatEvent) {
        _uiState.update { it.copy(latestChatEvent = event) }
        // 3초 후 자동 dismiss
        viewModelScope.launch {
            delay(3000)
            if (_uiState.value.latestChatEvent == event) {
                _uiState.update { it.copy(latestChatEvent = null) }
            }
        }
    }

    fun dismissChatEvent() {
        _uiState.update { it.copy(latestChatEvent = null) }
    }

    // ========== 콤보 시스템 ==========

    private fun updateCombo() {
        val currentTime = System.currentTimeMillis()
        val state = _uiState.value.comboState

        val newCombo = if (currentTime - state.lastMessageTime <= ComboState.COMBO_TIMEOUT_MS) {
            state.currentCombo + 1
        } else {
            1
        }

        val showEffect = newCombo >= 3 && (newCombo == 3 || newCombo == 5 || newCombo == 10 || newCombo % 10 == 0)

        _uiState.update {
            it.copy(
                comboState = ComboState(
                    currentCombo = newCombo,
                    lastMessageTime = currentTime,
                    showComboEffect = showEffect
                )
            )
        }

        // 콤보 달성 이벤트 발생
        if (showEffect) {
            emitChatEvent(ChatEvent.ComboAchieved(newCombo))
        }

        // 콤보 이펙트 1초 후 숨기기
        if (showEffect) {
            viewModelScope.launch {
                delay(1000)
                _uiState.update {
                    it.copy(comboState = it.comboState.copy(showComboEffect = false))
                }
            }
        }
    }

    // ========== 시간 기반 이벤트 ==========

    private fun startTimeEventMonitoring() {
        timeEventJob?.cancel()
        timeEventJob = viewModelScope.launch {
            while (_uiState.value.isInChat) {
                checkTimeBasedEvents()
                delay(1000)  // 1초마다 체크
            }
        }
    }

    private fun checkTimeBasedEvents() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentSecond = calendar.get(Calendar.SECOND)

        // 정각 이벤트 (매 시 정각, 초가 0~2초일 때 한 번만)
        if (currentMinute == 0 && currentSecond in 0..2 && currentHour != lastCheckedHour) {
            lastCheckedHour = currentHour
            _uiState.update { it.copy(isHourlyChime = true) }
            emitChatEvent(ChatEvent.HourlyChime)

            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(isHourlyChime = false) }
            }

            // 자정 특별 이벤트
            if (currentHour == 0) {
                _uiState.update { it.copy(isMidnightSpecial = true) }
                emitChatEvent(ChatEvent.MidnightSpecial)

                viewModelScope.launch {
                    delay(5000)
                    _uiState.update { it.copy(isMidnightSpecial = false) }
                }
            }
        }

        // 개인 시간 마일스톤 (1시간, 2시간, 3시간...)
        val sessionDuration = _uiState.value.sessionDuration
        val sessionHours = sessionDuration / (60 * 60 * 1000)
        if (sessionHours > 0 && sessionHours != lastPersonalHour) {
            lastPersonalHour = sessionHours
            _uiState.update { it.copy(personalHourMilestone = sessionHours.toInt()) }
            emitChatEvent(ChatEvent.PersonalHourMilestone(sessionHours.toInt()))

            viewModelScope.launch {
                delay(3000)
                _uiState.update { it.copy(personalHourMilestone = null) }
            }
        }
    }

    fun dismissTimeEvent() {
        _uiState.update {
            it.copy(
                isHourlyChime = false,
                isMidnightSpecial = false,
                personalHourMilestone = null
            )
        }
    }

    // ========== 봇 콘텐츠 시스템 ==========

    private fun startBotContentTimer() {
        botContentJob?.cancel()
        lastMessageTime = System.currentTimeMillis()
        currentBotThreshold = getRandomBotThreshold()

        botContentJob = viewModelScope.launch {
            while (_uiState.value.isInChat) {
                // 10~20초 랜덤 체크 간격 (더 자연스럽게)
                delay((10_000L..20_000L).random())

                val timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime
                if (timeSinceLastMessage >= currentBotThreshold) {
                    sendBotContent()
                    lastMessageTime = System.currentTimeMillis()
                    currentBotThreshold = getRandomBotThreshold()  // 다음 발동 시간도 랜덤
                }
            }
        }
    }

    /**
     * 메시지 수신 시 타이머 리셋 (외부에서 호출)
     */
    fun resetBotTimer() {
        lastMessageTime = System.currentTimeMillis()
    }

    private suspend fun sendBotContent() {
        val content = BotContentRepository.getRandomMessage()
        chatRepository.sendBotMessageWithCharacter(content.character, content.message)
    }

    /**
     * 유저 메시지에 봇이 반응
     */
    private suspend fun checkBotReaction(userMessage: String) {
        val reaction = BotContentRepository.getReactionToMessage(userMessage)
        if (reaction != null) {
            // 1~5초 랜덤 딜레이 후 반응 (더 자연스럽게)
            delay((1000L..5000L).random())
            chatRepository.sendBotMessageWithCharacter(reaction.character, reaction.message)
        }
    }

    // ========== 관리자 모드 ==========

    companion object {
        private const val COOLDOWN_MS = 1000L
        private const val RATE_LIMIT_WINDOW_MS = 10000L
        private const val RATE_LIMIT_MAX_MESSAGES = 5
        // 관리자 비밀번호 (실제 운영에서는 더 안전한 방법 권장)
        private const val ADMIN_PASSWORD = "elite2024!"
        private const val ADMIN_NICKNAME = "전우회장"
    }

    fun showAdminLoginDialog() {
        _uiState.update { it.copy(showAdminLoginDialog = true) }
    }

    fun dismissAdminLoginDialog() {
        _uiState.update { it.copy(showAdminLoginDialog = false) }
    }

    fun tryAdminLogin(password: String): Boolean {
        val isValid = password == ADMIN_PASSWORD
        if (isValid) {
            _uiState.update {
                it.copy(
                    isAdminMode = true,
                    showAdminLoginDialog = false
                )
            }
        }
        return isValid
    }

    fun logoutAdmin() {
        viewModelScope.launch {
            // 원래 닉네임 복원
            val originalNickname = preferences.getNickname()
            _uiState.update {
                it.copy(
                    isAdminMode = false,
                    nickname = originalNickname
                )
            }
        }
    }

    /**
     * 관리자: 배터리 상관없이 입장
     */
    fun enterChatAsAdmin() {
        if (!_uiState.value.isAdminMode) return

        val state = _uiState.value

        viewModelScope.launch {
            // 관리자는 대장 계급으로 시작 (10080분 = 7일)
            val generalDuration = EliteRank.GENERAL.minMinutes * 60 * 1000L
            val adjustedStartTime = System.currentTimeMillis() - generalDuration

            chatRepository.setJoinedTime(System.currentTimeMillis())
            chatRepository.joinChat(state.userId, ADMIN_NICKNAME, isAdmin = true)

            preferences.startSession()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.CHAT,
                    isInChat = true,
                    sessionStartTime = adjustedStartTime,
                    sessionDuration = generalDuration,
                    nickname = ADMIN_NICKNAME
                )
            }

            startMessageObservation()
            startOnlineUsersObservation()
            startSessionTimer()
            startActivityUpdate()
            startTimeEventMonitoring()
            startBotContentTimer()
            startReportsObservation()  // 관리자: 신고 목록 관찰
            // 관리자는 배터리 모니터링 안 함 (강퇴 방지)
        }
    }

    /**
     * 관리자: 신고 목록 관찰 시작
     */
    private fun startReportsObservation() {
        reportsObserverJob?.cancel()
        reportsObserverJob = viewModelScope.launch {
            chatRepository.getReports().collect { reports ->
                _uiState.update { it.copy(reports = reports) }
            }
        }
    }

    /**
     * 관리자: 사용자 강퇴
     */
    fun kickUser(userId: String, nickname: String) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.kickUser(userId)
            chatRepository.sendSystemMessage("${nickname}님이 관리자에 의해 퇴장되었습니다")
        }
    }

    /**
     * 관리자: 사용자 계급 변경
     */
    fun changeUserRank(userId: String, nickname: String, newRank: EliteRank) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.changeUserRank(userId, newRank)
            chatRepository.sendSystemMessage("${nickname}님이 ${newRank.koreanName}(으)로 임명되었습니다")
        }
    }

    /**
     * 관리자: 공지 메시지 전송
     */
    fun sendAdminNotice(message: String) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.sendSystemMessage("[공지] $message")
        }
    }

    /**
     * 관리자: 메시지 삭제
     */
    fun deleteMessage(messageId: String) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.deleteMessage(messageId)
        }
    }

    /**
     * 관리자: 신고 상태 업데이트
     */
    fun updateReportStatus(reportId: String, status: ReportStatus) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.updateReportStatus(reportId, status)
        }
    }

    /**
     * 관리자: 신고 삭제 (기각)
     */
    fun dismissReport(reportId: String) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            chatRepository.deleteReport(reportId)
        }
    }

    /**
     * 관리자: 신고 처리 - 메시지 삭제 후 신고 완료 처리
     */
    fun handleReport(reportId: String, messageId: String, deleteMessage: Boolean = true) {
        if (!_uiState.value.isAdminMode) return

        viewModelScope.launch {
            if (deleteMessage) {
                chatRepository.deleteMessage(messageId)
            }
            chatRepository.updateReportStatus(reportId, ReportStatus.ACTIONED)
        }
    }

    // ========== FCM 토큰 관리 ==========

    /**
     * FCM 토큰 등록
     */
    private suspend fun registerFcmToken(userId: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            chatRepository.saveFcmToken(userId, token)

            // 토큰 갱신 콜백 설정
            EliteMessagingService.onTokenRefresh = { newToken ->
                viewModelScope.launch {
                    chatRepository.saveFcmToken(userId, newToken)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * FCM 토큰 삭제
     */
    private suspend fun unregisterFcmToken(userId: String) {
        try {
            chatRepository.removeFcmToken(userId)
            EliteMessagingService.onTokenRefresh = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 앱 라이프사이클 ==========

    /**
     * 앱이 백그라운드로 전환될 때 호출
     * - 배터리 모니터링 일시 중지
     * - 위험 카운트다운 일시 중지
     */
    fun onAppBackground() {
        if (_uiState.value.isInChat) {
            // 배터리 모니터링 일시 중지
            batteryMonitorJob?.cancel()

            // 위험 카운트다운 일시 중지
            if (_uiState.value.isInDanger) {
                dangerCountdownJob?.cancel()
            }
        }
    }

    /**
     * 앱이 포그라운드로 복귀할 때 호출
     * - 배터리 상태 재확인
     * - 충전 중이면 위험 모드 해제
     * - 배터리 모니터링 재개
     */
    fun onAppForeground() {
        if (_uiState.value.isInChat) {
            // 관리자 모드는 배터리 체크 안 함
            if (_uiState.value.isAdminMode) {
                return
            }

            // 배터리 상태 즉시 업데이트
            updateBatteryStatus()

            val batteryState = _uiState.value.batteryState

            // 충전 중이면 위험 모드 해제
            if (batteryState.isCharging && _uiState.value.isInDanger) {
                _uiState.update {
                    it.copy(
                        isInDanger = false,
                        dangerCountdown = 0,
                        dangerStartTime = 0L
                    )
                }
            }
            // 100%이고 충전 중이면 안전
            else if (batteryState.level == 100 && batteryState.isCharging) {
                _uiState.update {
                    it.copy(
                        isInDanger = false,
                        dangerCountdown = 0,
                        dangerStartTime = 0L
                    )
                }
            }
            // 위험 상태가 아니었는데 복귀 시 배터리가 100% 미만이고 충전 안 하면 위험 시작
            else if (!_uiState.value.isInDanger && batteryState.level < 100 && !batteryState.isCharging) {
                startDangerCountdown()
            }
            // 기존 위험 상태였으면 카운트다운 재시작
            else if (_uiState.value.isInDanger) {
                startDangerCountdown()
            }

            // 배터리 모니터링 재개
            startBatteryMonitoring()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        batteryMonitorJob?.cancel()
        dangerCountdownJob?.cancel()
        activityUpdateJob?.cancel()
        timeEventJob?.cancel()
        botContentJob?.cancel()
        reportsObserverJob?.cancel()
    }
}
