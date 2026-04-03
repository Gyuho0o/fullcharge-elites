package com.elites.fullcharge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elites.fullcharge.ElitesApplication
import com.elites.fullcharge.data.*
import com.elites.fullcharge.util.ContentFilter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val onlineUserCount: Int = 0,
    val sessionStartTime: Long = 0L,
    val sessionDuration: Long = 0L,
    val isInChat: Boolean = false,
    // 위험 카운트다운 관련
    val isInDanger: Boolean = false,
    val dangerCountdown: Int = 0,  // 남은 초
    val dangerStartTime: Long = 0L,
    // 필터링 에러 메시지
    val filterErrorMessage: String? = null,
    // 답장 중인 메시지
    val replyingTo: ChatMessage? = null
) {
    companion object {
        const val DANGER_COUNTDOWN_SECONDS = 10  // 10초 카운트다운
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

    // 도배 방지용
    private var lastSentMessage: String = ""
    private var consecutiveCount: Int = 0
    private var lastSentTime: Long = 0L  // 쿨다운용
    private val recentMessageTimes = mutableListOf<Long>()  // 시간당 제한용

    companion object {
        private const val COOLDOWN_MS = 1000L  // 1초 쿨다운
        private const val RATE_LIMIT_WINDOW_MS = 10000L  // 10초 윈도우
        private const val RATE_LIMIT_MAX_MESSAGES = 5  // 10초에 최대 5개
    }

    init {
        // 사용자 ID 및 닉네임 초기화
        viewModelScope.launch {
            val userId = preferences.initializeUserId()
            val nickname = preferences.initializeNickname()
            _uiState.update { it.copy(userId = userId, nickname = nickname) }
        }

        // 닉네임 변경 관찰
        viewModelScope.launch {
            preferences.nickname.collect { nickname ->
                if (nickname.isNotBlank()) {
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

        // 초기 배터리 상태 확인
        updateBatteryStatus()

        // 배터리 상태 관찰
        viewModelScope.launch {
            batteryManager.batteryState.collect { state ->
                val currentState = _uiState.value
                val isInChat = currentState.isInChat
                val wasInDanger = currentState.isInDanger

                _uiState.update { it.copy(batteryState = state) }

                if (isInChat) {
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
        _uiState.update {
            it.copy(
                isInDanger = false,
                dangerCountdown = 0,
                dangerStartTime = 0L
            )
        }
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
        }
    }

    private fun startMessageObservation() {
        viewModelScope.launch {
            chatRepository.getMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }

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

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val startTime = _uiState.value.sessionStartTime
                if (startTime > 0) {
                    val duration = System.currentTimeMillis() - startTime
                    _uiState.update { it.copy(sessionDuration = duration) }
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

    fun createPoll(question: String, options: List<String>) {
        if (question.isBlank() || options.size < 2) return

        val state = _uiState.value
        val rank = EliteRank.fromDuration(state.sessionDuration)

        viewModelScope.launch {
            chatRepository.sendPollMessage(
                userId = state.userId,
                nickname = state.nickname,
                rank = rank.name,
                question = question,
                options = options
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

            // 퇴장 알림 시스템 메시지 전송
            chatRepository.sendSystemMessage("${state.nickname}님이 전우회를 배신했습니다")

            // Firebase에서 퇴장 처리
            chatRepository.leaveChat(state.userId)

            // 세션 종료
            preferences.endSession()

            // 타이머 정지
            sessionTimerJob?.cancel()
            batteryMonitorJob?.cancel()

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

            // 퇴장 알림 시스템 메시지 전송
            chatRepository.sendSystemMessage("${state.nickname}님이 퇴장했습니다")

            // Firebase에서 퇴장 처리
            chatRepository.leaveChat(state.userId)

            // 세션 종료
            preferences.endSession()

            // 타이머 정지
            sessionTimerJob?.cancel()
            batteryMonitorJob?.cancel()
            dangerCountdownJob?.cancel()
            activityUpdateJob?.cancel()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.GATEKEEPER,
                    isInChat = false,
                    isInDanger = false,
                    dangerCountdown = 0,
                    messages = emptyList(),
                    sessionStartTime = 0L,
                    sessionDuration = 0L
                )
            }
        }
    }

    fun changeNickname(newNickname: String) {
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

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        batteryMonitorJob?.cancel()
        dangerCountdownJob?.cancel()
        activityUpdateJob?.cancel()
    }
}
