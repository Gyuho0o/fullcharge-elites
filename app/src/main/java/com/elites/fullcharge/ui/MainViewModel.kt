package com.elites.fullcharge.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elites.fullcharge.ElitesApplication
import com.elites.fullcharge.data.*
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
    val dangerStartTime: Long = 0L
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

    init {
        // 사용자 ID 초기화
        viewModelScope.launch {
            val userId = preferences.initializeUserId()
            _uiState.update { it.copy(userId = userId) }
        }

        // 닉네임 관찰
        viewModelScope.launch {
            preferences.nickname.collect { nickname ->
                _uiState.update { it.copy(nickname = nickname) }
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
        }
    }

    private fun startMessageObservation() {
        viewModelScope.launch {
            chatRepository.getMessages().collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
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

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val state = _uiState.value
        val rank = EliteRank.fromDuration(state.sessionDuration)

        viewModelScope.launch {
            val message = ChatMessage(
                id = UUID.randomUUID().toString(),
                userId = state.userId,
                nickname = state.nickname,
                message = text,
                timestamp = System.currentTimeMillis(),
                rank = rank.name
            )
            chatRepository.sendMessage(message)
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

            // Firebase에서 퇴장 처리
            chatRepository.leaveChat(state.userId)

            // 세션 종료
            preferences.endSession()

            // 타이머 정지
            sessionTimerJob?.cancel()
            batteryMonitorJob?.cancel()

            _uiState.update {
                it.copy(
                    currentScreen = AppScreen.GATEKEEPER,
                    isInChat = false,
                    messages = emptyList(),
                    sessionStartTime = 0L,
                    sessionDuration = 0L
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionTimerJob?.cancel()
        batteryMonitorJob?.cancel()
        dangerCountdownJob?.cancel()
    }
}
