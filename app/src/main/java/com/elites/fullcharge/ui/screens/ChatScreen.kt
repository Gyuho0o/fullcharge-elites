package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.elites.fullcharge.data.Achievement
import com.elites.fullcharge.data.AllTimeRecord
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.ChatEvent
import com.elites.fullcharge.data.ChatMessage
import com.elites.fullcharge.data.ComboState
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.data.EliteUser
import com.elites.fullcharge.data.Report
import com.elites.fullcharge.data.ReportReason
import com.elites.fullcharge.data.ReportStatus
import com.elites.fullcharge.ui.components.*
import com.elites.fullcharge.ui.theme.*
import com.elites.fullcharge.util.LinkPreviewFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    batteryState: BatteryState,
    messages: List<ChatMessage>,
    onlineUsers: List<EliteUser>,
    allTimeRecords: List<AllTimeRecord> = emptyList(),
    currentUserId: String,
    currentUserNickname: String,
    sessionDuration: Long,
    onSendMessage: (String) -> Unit,
    onLeaveChat: () -> Unit = {},
    onNicknameChange: (String) -> Unit = {},
    onReportMessage: (ChatMessage, String, (Boolean) -> Unit) -> Unit = { _, _, _ -> },
    filterErrorMessage: String? = null,
    onClearFilterError: () -> Unit = {},
    replyingTo: ChatMessage? = null,
    onReply: (ChatMessage) -> Unit = {},
    onClearReply: () -> Unit = {},
    onToggleReaction: (String, String) -> Unit = { _, _ -> },
    onCreatePoll: (String, List<String>, Int) -> Unit = { _, _, _ -> },
    onVotePoll: (String, Int) -> Unit = { _, _ -> },
    isInDanger: Boolean = false,
    dangerCountdown: Int = 0,
    bannerAdContent: @Composable () -> Unit = {},
    // 업적 관련
    newlyUnlockedAchievement: Achievement? = null,
    onDismissAchievement: () -> Unit = {},
    unlockedAchievements: Set<String> = emptySet(),
    // 위기 탈출 관련
    showCrisisEscapeCelebration: Boolean = false,
    onDismissCrisisEscape: () -> Unit = {},
    // 실시간 이벤트
    latestChatEvent: ChatEvent? = null,
    onDismissChatEvent: () -> Unit = {},
    // 콤보 시스템
    comboState: ComboState = ComboState(),
    // 시간 기반 이벤트
    personalHourMilestone: Int? = null,
    isHourlyChime: Boolean = false,
    isMidnightSpecial: Boolean = false,
    onDismissTimeEvent: () -> Unit = {},
    // 관리자 모드
    isAdminMode: Boolean = false,
    onKickUser: (String, String) -> Unit = { _, _ -> },
    onChangeUserRank: (String, String, EliteRank) -> Unit = { _, _, _ -> },
    onSendAdminNotice: (String) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    // 신고 관리
    reports: List<Report> = emptyList(),
    onHandleReport: (String, String, Boolean) -> Unit = { _, _, _ -> },  // reportId, messageId, deleteMessage
    onDismissReport: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showLeaderboard by remember { mutableStateOf(false) }
    var showPromotionCountdown by remember { mutableStateOf(false) }
    var showRankUpCelebration by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var messageToReport by remember { mutableStateOf<ChatMessage?>(null) }
    var newRank by remember { mutableStateOf(EliteRank.TRAINEE) }
    var messageSentTrigger by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 관리자 패널
    var showAdminPanel by remember { mutableStateOf(false) }
    var selectedUserForAdmin by remember { mutableStateOf<EliteUser?>(null) }
    var showAdminNoticeDialog by remember { mutableStateOf(false) }

    // 필터 에러 메시지 표시
    LaunchedEffect(filterErrorMessage) {
        filterErrorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            onClearFilterError()
        }
    }

    val currentRank = EliteRank.fromDuration(sessionDuration)
    val previousRank = remember { mutableStateOf(currentRank) }

    // 등급 상승 감지 (카운트다운 중에는 표시 안 함)
    LaunchedEffect(currentRank, isInDanger) {
        if (!isInDanger && currentRank != previousRank.value && currentRank.ordinal > previousRank.value.ordinal) {
            newRank = currentRank
            showRankUpCelebration = true
        }
        previousRank.value = currentRank
    }

    // 카운트다운 시작하면 승급 UI 숨기기
    LaunchedEffect(isInDanger) {
        if (isInDanger) {
            showRankUpCelebration = false
        }
    }

    // 키보드 상태 감지
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = imeBottom > 0

    // 새 메시지가 오면 자동 스크롤 (마지막 메시지 ID 기준으로 더 안정적)
    val lastMessageId = messages.lastOrNull()?.id
    LaunchedEffect(lastMessageId) {
        if (messages.isNotEmpty() && lastMessageId != null) {
            // 약간의 딜레이로 리스트 안정화 후 스크롤
            delay(50)
            // 마지막 아이템이 화면 하단에 완전히 보이도록 충분한 offset 적용
            listState.animateScrollToItem(
                index = messages.size - 1,
                scrollOffset = Int.MAX_VALUE  // 아이템 끝까지 스크롤
            )
        }
    }

    // 키보드 상태 변경 시 최신 메시지로 스크롤
    LaunchedEffect(isKeyboardVisible) {
        if (messages.isNotEmpty()) {
            // 약간의 딜레이 후 스크롤 (키보드 애니메이션 완료 대기)
            delay(150)
            // 마지막 아이템이 화면 하단에 완전히 보이도록 충분한 offset 적용
            listState.animateScrollToItem(
                index = messages.size - 1,
                scrollOffset = Int.MAX_VALUE  // 아이템 끝까지 스크롤
            )
        }
    }

    // 현재 사용자 계급 계산
    val currentRankForEffects = EliteRank.fromDuration(sessionDuration)
    val isHighRank = currentRankForEffects.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal
    val isGeneral = currentRankForEffects.ordinal >= EliteRank.BRIGADIER_GENERAL.ordinal

    // 충전 중일 때만 화려한 효과 표시
    val showEffects = batteryState.isCharging

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // 배경 파티클 효과 (충전 중일 때만)
        if (showEffects) {
            DenseBackgroundParticles()
        }

        // 배경 번개 (충전 중일 때만, 고계급일수록 더 자주)
        BackgroundLightning(
            enabled = showEffects,
            intensityLevel = if (isGeneral) 3 else if (isHighRank) 2 else 1
        )

        // 장성급 전용 골드 파티클 (충전 중일 때만)
        if (isGeneral && showEffects) {
            GoldAuraParticles()
        }

        // 메시지 전송 시 번개 효과 (충전 중일 때만)
        if (showEffects) {
            MessageSendLightning(trigger = messageSentTrigger)
        }

        // 위험 경고 오버레이
        if (isInDanger) {
            DangerWarningOverlay(countdown = dangerCountdown)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 위험 상태일 때 상단 경고 바
            AnimatedVisibility(
                visible = isInDanger,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DangerWarningBar(countdown = dangerCountdown)
            }

            // 상단 바 (키보드 올라오면 내 정보 숨김)
            ChatTopBar(
                batteryLevel = batteryState.level,
                userCount = onlineUsers.size,
                rank = currentRank,
                nickname = currentUserNickname,
                isLeaderboardExpanded = showLeaderboard,
                onToggleLeaderboard = { showLeaderboard = !showLeaderboard },
                onEditNickname = { showNicknameDialog = true },
                onLeaveClick = { showLeaveDialog = true },
                showUserInfo = !isKeyboardVisible
            )

            // 등급까지 남은 시간 (위험 모드 또는 키보드 올라왔을 때 숨김)
            AnimatedVisibility(
                visible = !isInDanger && !isKeyboardVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                진급카운트다운(
                    현재시간 = sessionDuration,
                    isExpanded = showPromotionCountdown,
                    onToggle = { showPromotionCountdown = !showPromotionCountdown },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 리더보드 (토글, 고정 영역)
            AnimatedVisibility(
                visible = showLeaderboard,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                스와이프리더보드(
                    사용자들 = onlineUsers,
                    역대기록들 = allTimeRecords,
                    현재사용자ID = currentUserId,
                    onCollapse = { showLeaderboard = false },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 채팅 영역 (키보드에 반응하는 영역)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .imePadding()
            ) {
                // 채팅 메시지 목록
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        when {
                            message.isSystemMessage -> {
                                SystemMessageItem(
                                    message = message,
                                    currentUserNickname = currentUserNickname,
                                    onWelcomeTap = { welcomeMessage ->
                                        onSendMessage(welcomeMessage)
                                    }
                                )
                            }
                            message.isPoll -> {
                                PollMessageItem(
                                    message = message,
                                    isOwnMessage = message.userId == currentUserId,
                                    currentUserId = currentUserId,
                                    onVote = { optionIndex -> onVotePoll(message.id, optionIndex) }
                                )
                            }
                            else -> {
                                ChatMessageItem(
                                    message = message,
                                    isOwnMessage = message.userId == currentUserId,
                                    currentUserId = currentUserId,
                                    onlineUserCount = onlineUsers.size,
                                    onReply = { onReply(message) },
                                    onToggleReaction = { emoji -> onToggleReaction(message.id, emoji) },
                                    onLongPress = {
                                        // 다른 사람의 메시지만 신고 가능
                                        if (message.userId != currentUserId) {
                                            messageToReport = message
                                            showReportDialog = true
                                        }
                                    },
                                    isAdminMode = isAdminMode,
                                    onDelete = { onDeleteMessage(message.id) }
                                )
                            }
                        }
                    }
                }

                // 답장 미리보기 (입력창 위)
                AnimatedVisibility(
                    visible = replyingTo != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    replyingTo?.let { replyMessage ->
                        ReplyPreviewBar(
                            replyingTo = replyMessage,
                            onClear = onClearReply
                        )
                    }
                }

                // 메시지 입력창 (네비게이션 바 위로)
                MessageInputBar(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    onSend = {
                        if (messageInput.isNotBlank()) {
                            onSendMessage(messageInput.trim())
                            messageInput = ""
                            messageSentTrigger++
                        }
                    },
                    onPollClick = { showPollDialog = true },
                    onlineUsers = onlineUsers,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        }

        // 등급 상승 축하
        RankUpCelebration(
            newRank = newRank,
            visible = showRankUpCelebration,
            onDismiss = { showRankUpCelebration = false },
            bannerAdView = bannerAdContent
        )

        // 업적 달성 팝업
        AchievementUnlockedPopup(
            achievement = newlyUnlockedAchievement,
            onDismiss = onDismissAchievement
        )

        // 위기 탈출 축하 이펙트
        CrisisEscapeCelebration(
            visible = showCrisisEscapeCelebration,
            onDismiss = onDismissCrisisEscape
        )

        // 실시간 이벤트 알림 배너
        ChatEventBanner(
            event = latestChatEvent,
            onDismiss = onDismissChatEvent
        )

        // 콤보 이펙트
        ComboEffectOverlay(
            comboState = comboState
        )

        // 자정 특별 이벤트
        MidnightSpecialOverlay(
            visible = isMidnightSpecial,
            onDismiss = onDismissTimeEvent
        )

        // 정각 알림
        HourlyChimeOverlay(
            visible = isHourlyChime
        )

        // 개인 시간 마일스톤
        PersonalMilestoneOverlay(
            hours = personalHourMilestone,
            onDismiss = onDismissTimeEvent
        )

        // 닉네임 수정 다이얼로그
        if (showNicknameDialog) {
            NicknameEditDialog(
                currentNickname = currentUserNickname,
                onDismiss = { showNicknameDialog = false },
                onConfirm = { newNickname ->
                    onNicknameChange(newNickname)
                    showNicknameDialog = false
                }
            )
        }

        // 신고 다이얼로그
        if (showReportDialog && messageToReport != null) {
            ReportDialog(
                message = messageToReport!!,
                onDismiss = {
                    showReportDialog = false
                    messageToReport = null
                },
                onReport = { reason ->
                    onReportMessage(messageToReport!!, reason) { success ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = if (success) "신고가 접수되었어요" else "이미 신고한 메시지예요",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                    showReportDialog = false
                    messageToReport = null
                }
            )
        }

        // 투표 생성 다이얼로그
        if (showPollDialog) {
            CreatePollDialog(
                onDismiss = { showPollDialog = false },
                onCreatePoll = { question, options, durationMinutes ->
                    onCreatePoll(question, options, durationMinutes)
                    showPollDialog = false
                }
            )
        }

        // 나가기 확인 다이얼로그
        if (showLeaveDialog) {
            LeaveConfirmDialog(
                onDismiss = { showLeaveDialog = false },
                onConfirm = {
                    showLeaveDialog = false
                    onLeaveChat()
                }
            )
        }

        // 관리자 플로팅 버튼 (상단 우측에 작게 표시)
        if (isAdminMode) {
            SmallFloatingActionButton(
                onClick = { showAdminPanel = true },
                containerColor = StatusRed.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 8.dp)
                    .size(40.dp)
            ) {
                Text("🔐", fontSize = 16.sp)
            }
        }

        // 관리자 패널 다이얼로그
        if (showAdminPanel) {
            AdminPanelDialog(
                onlineUsers = onlineUsers,
                currentUserId = currentUserId,
                onKickUser = { user ->
                    onKickUser(user.userId, user.nickname)
                    showAdminPanel = false
                },
                onChangeUserRank = { user, rank ->
                    onChangeUserRank(user.userId, user.nickname, rank)
                    showAdminPanel = false
                },
                onSendNotice = { showAdminNoticeDialog = true },
                onDismiss = { showAdminPanel = false },
                reports = reports,
                onHandleReport = { reportId, messageId, deleteMsg ->
                    onHandleReport(reportId, messageId, deleteMsg)
                },
                onDismissReport = { reportId ->
                    onDismissReport(reportId)
                }
            )
        }

        // 관리자 공지 다이얼로그
        if (showAdminNoticeDialog) {
            AdminNoticeDialog(
                onSend = { notice ->
                    onSendAdminNotice(notice)
                    showAdminNoticeDialog = false
                    showAdminPanel = false
                },
                onDismiss = { showAdminNoticeDialog = false }
            )
        }

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DenseBackgroundParticles() {
    data class Particle(
        val id: Int,
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val alpha: Float,
        val wobble: Float
    )

    var particles by remember {
        mutableStateOf(
            List(50) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 5f + 2f,
                    speed = Random.nextFloat() * 0.0015f + 0.0005f,
                    alpha = Random.nextFloat() * 0.25f + 0.05f,
                    wobble = Random.nextFloat() * 100f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bgParticles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            val newY = p.y - p.speed
            val newX = p.x + sin((time + p.wobble) * 0.03f) * 0.001f
            if (newY < -0.05f) {
                p.copy(x = Random.nextFloat(), y = 1.05f)
            } else {
                p.copy(x = newX.coerceIn(0f, 1f), y = newY)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            // 글로우
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            // 메인
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

@Composable
private fun MessageSendLightning(trigger: Int) {
    var bolts by remember { mutableStateOf<List<QuickBolt>>(emptyList()) }
    var flashAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            // 빠른 번개 연속 발생
            repeat(4) {
                bolts = List(Random.nextInt(3, 6)) { generateQuickBolt() }
                flashAlpha = 0.15f
                delay(40)
                flashAlpha = 0f
                delay(30)
            }
            bolts = emptyList()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 플래시
        if (flashAlpha > 0) {
            drawRect(color = TossBlueLight.copy(alpha = flashAlpha))
        }

        // 번개들
        bolts.forEach { bolt ->
            drawQuickBolt(bolt)
        }
    }
}

private data class QuickBolt(
    val startX: Float,
    val startY: Float,
    val points: List<Offset>,
    val alpha: Float
)

private fun generateQuickBolt(): QuickBolt {
    val startX = Random.nextFloat()
    val startY = Random.nextFloat() * 0.5f + 0.25f
    val points = mutableListOf<Offset>()

    var x = 0f
    var y = 0f
    val angle = Random.nextFloat() * Math.PI.toFloat() * 2

    repeat(Random.nextInt(4, 8)) {
        x += cos(angle + (Random.nextFloat() - 0.5f) * 0.5f) * 0.04f
        y += sin(angle + (Random.nextFloat() - 0.5f) * 0.5f) * 0.04f
        points.add(Offset(x, y))
    }

    return QuickBolt(startX, startY, points, Random.nextFloat() * 0.5f + 0.5f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuickBolt(bolt: QuickBolt) {
    val start = Offset(bolt.startX * size.width, bolt.startY * size.height)
    var prev = start

    bolt.points.forEach { point ->
        val current = Offset(
            start.x + point.x * size.width,
            start.y + point.y * size.height
        )

        // 글로우
        drawLine(
            color = TossBlueLight.copy(alpha = bolt.alpha * 0.4f),
            start = prev,
            end = current,
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )

        // 메인
        drawLine(
            color = TossBlueLight.copy(alpha = bolt.alpha * 0.8f),
            start = prev,
            end = current,
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // 밝은 중심
        drawLine(
            color = Color.White.copy(alpha = bolt.alpha),
            start = prev,
            end = current,
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )

        prev = current
    }
}

@Composable
private fun ChatTopBar(
    batteryLevel: Int,
    userCount: Int,
    rank: EliteRank,
    nickname: String,
    isLeaderboardExpanded: Boolean,
    onToggleLeaderboard: () -> Unit,
    onEditNickname: () -> Unit,
    onLeaveClick: () -> Unit,
    showUserInfo: Boolean = true  // 키보드 올라오면 숨김
) {
    val infiniteTransition = rememberInfiniteTransition(label = "batteryPulse")
    val batteryAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "batteryAlpha"
    )

    // 화살표 회전 애니메이션
    val arrowRotation by animateFloatAsState(
        targetValue = if (isLeaderboardExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "arrowRotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BackgroundWhite.copy(alpha = 0.95f),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 나가기 버튼
                    IconButton(
                        onClick = onLeaveClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "나가기",
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column {
                        Text(
                            text = "완충 전우회",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        AnimatedContent(
                            targetState = userCount,
                            transitionSpec = {
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            },
                            label = "userCount"
                        ) { count ->
                            Text(
                                text = "지금 ${count}명이 풀배터리",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 랭킹 버튼 (클릭 가능한 모양으로)
                    Surface(
                        onClick = onToggleLeaderboard,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLeaderboardExpanded) TossBlue.copy(alpha = 0.1f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "랭킹",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TossBlue
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isLeaderboardExpanded) "접기" else "펼치기",
                                tint = TossBlue,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(arrowRotation)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${batteryLevel}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TossBlue.copy(alpha = batteryAlpha)
                    )
                }
            }

            // 내 정보 영역 (키보드 올라오면 숨김)
            AnimatedVisibility(
                visible = showUserInfo,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundGray)
                            .clickable { onEditNickname() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "내 정보",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        RankBadge(rank = rank)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = nickname,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "닉네임 수정",
                            tint = TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankBadge(rank: EliteRank) {
    // 병사/훈련병 여부 확인
    val isEnlisted = rank in listOf(
        EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
        EliteRank.CORPORAL, EliteRank.SERGEANT
    )

    // 버블 색상과 통일된 배지 색상
    val badgeColors = when (rank) {
        // 장성 (골드 그라데이션)
        EliteRank.GENERAL -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
        EliteRank.LIEUTENANT_GENERAL -> listOf(Color(0xFF92400E), Color(0xFFD97706))
        EliteRank.MAJOR_GENERAL -> listOf(Color(0xFFB45309), Color(0xFFD97706))
        EliteRank.BRIGADIER_GENERAL -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
        // 영관급 (보라색 그라데이션)
        EliteRank.COLONEL -> listOf(Color(0xFF581C87), Color(0xFF7C3AED))
        EliteRank.LIEUTENANT_COLONEL -> listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6))
        EliteRank.MAJOR -> listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
        // 위관급 (초록색 그라데이션)
        EliteRank.CAPTAIN -> listOf(Color(0xFF047857), Color(0xFF10B981))
        EliteRank.FIRST_LIEUTENANT -> listOf(Color(0xFF059669), Color(0xFF34D399))
        EliteRank.SECOND_LIEUTENANT -> listOf(Color(0xFF10B981), Color(0xFF6EE7B7))
        // 부사관 (파란색 그라데이션)
        EliteRank.SERGEANT_MAJOR -> listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
        EliteRank.MASTER_SERGEANT -> listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))
        EliteRank.SERGEANT_FIRST -> listOf(Color(0xFF2563EB), Color(0xFF93C5FD))
        EliteRank.STAFF_SERGEANT -> listOf(Color(0xFF3B82F6), Color(0xFFBFDBFE))
        // 병사/훈련병 (카카오톡 스타일 노란색)
        else -> listOf(Color(0xFFFEE500), Color(0xFFFEE500))
    }

    // 텍스트 색상 (병사/훈련병은 검은색)
    val textColor = if (isEnlisted) Color.Black else Color.White

    // 계급별 아이콘
    val rankIcon = when (rank) {
        EliteRank.GENERAL -> "⭐"
        EliteRank.LIEUTENANT_GENERAL, EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> "★"
        EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> "◆"
        EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> "▲"
        EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT, EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> "■"
        else -> null
    }

    Box(
        modifier = Modifier
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(6.dp),
                spotColor = badgeColors.first().copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.horizontalGradient(badgeColors))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (rankIcon != null) {
                Text(
                    text = rankIcon,
                    fontSize = 8.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = rank.koreanName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isOwnMessage: Boolean,
    currentUserId: String = "",
    onlineUserCount: Int = 0,
    onReply: () -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onLongPress: () -> Unit = {},
    isAdminMode: Boolean = false,
    onDelete: () -> Unit = {}
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val availableReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
    val haptic = LocalHapticFeedback.current
    // 안 읽은 사람 수 계산 (내 메시지에만 표시)
    val unreadCount = if (isOwnMessage) {
        message.getUnreadCount(onlineUserCount)
    } else 0
    val messageRank = try {
        EliteRank.valueOf(message.rank)
    } catch (e: Exception) {
        EliteRank.TRAINEE
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    // 전설 등급 반짝이 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "godShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // 터줏대감 글로우 애니메이션
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
            // 닉네임 + 계급 (모든 메시지에 표시)
            // 고계급(장성) 스파클 효과 포함
            val isGeneral = messageRank.ordinal >= EliteRank.BRIGADIER_GENERAL.ordinal
            val isOfficer = messageRank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp),
                    horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
                ) {
                    // 장성급 앞에 별 아이콘
                    if (isGeneral) {
                        Text(
                            text = "⭐",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                    RankBadge(rank = messageRank)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOwnMessage) "${message.nickname} (나)" else message.nickname,
                        fontSize = 12.sp,
                        color = if (isOwnMessage) TossBlue else TextSecondary,
                        // 장성급은 골드 글로우 효과
                        style = if (isGeneral) {
                            androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 8f
                                )
                            )
                        } else {
                            androidx.compose.ui.text.TextStyle.Default
                        }
                    )
                    // 영관급 이상은 번개 아이콘
                    if (isOfficer && !isGeneral) {
                        Text(
                            text = "⚡",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                // 장성급 스파클 파티클 (작은 크기로)
                if (isGeneral) {
                    HighRankSparkles(shimmerOffset)
                }
            }

            // 말풍선 + 시간 (가로 배치)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
            ) {
                // 내 메시지: 읽음 수 + 시간 → 말풍선
                if (isOwnMessage) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TossBlue
                            )
                        }
                        Text(
                            text = timeString,
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                }

                // 등급별 채팅 버블 색상 (19단계)
                val bubbleColor = when (messageRank) {
                    // 장성 (골드 계열)
                    EliteRank.GENERAL -> Color(0xFF78350F)
                    EliteRank.LIEUTENANT_GENERAL -> Color(0xFF92400E)
                    EliteRank.MAJOR_GENERAL -> Color(0xFFB45309)
                    EliteRank.BRIGADIER_GENERAL -> Color(0xFFD97706)
                    // 영관급 (보라색 계열)
                    EliteRank.COLONEL -> Color(0xFF581C87)
                    EliteRank.LIEUTENANT_COLONEL -> Color(0xFF6D28D9)
                    EliteRank.MAJOR -> Color(0xFF7C3AED)
                    // 위관급 (초록색 계열)
                    EliteRank.CAPTAIN -> Color(0xFF047857)
                    EliteRank.FIRST_LIEUTENANT -> Color(0xFF059669)
                    EliteRank.SECOND_LIEUTENANT -> Color(0xFF10B981)
                    // 부사관 (파란색 계열)
                    EliteRank.SERGEANT_MAJOR -> Color(0xFF1E40AF)
                    EliteRank.MASTER_SERGEANT -> Color(0xFF1D4ED8)
                    EliteRank.SERGEANT_FIRST -> Color(0xFF2563EB)
                    EliteRank.STAFF_SERGEANT -> Color(0xFF3B82F6)
                    // 병사 (카카오톡 스타일 노란색)
                    EliteRank.SERGEANT -> Color(0xFFFEE500)      // 병장
                    EliteRank.CORPORAL -> Color(0xFFFEE500)      // 상병
                    EliteRank.PRIVATE_FIRST -> Color(0xFFFEE500) // 일병
                    EliteRank.PRIVATE_SECOND -> Color(0xFFFEE500) // 이병
                    // 훈련병 (카카오톡 스타일 노란색)
                    EliteRank.TRAINEE -> Color(0xFFFEE500)
                }

                // 병사/훈련병은 검은색 텍스트, 나머지는 흰색
                val isEnlistedRank = messageRank in listOf(
                    EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
                    EliteRank.CORPORAL, EliteRank.SERGEANT
                )
                val textColor = if (isEnlistedRank) Color.Black else Color.White

                // 등급별 버블 모디파이어
                val bubbleShape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                    bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                )

                val bubbleModifier = when (messageRank) {
                    // 대장: 최고급 골드 반짝이
                    EliteRank.GENERAL -> {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                                    cornerRadius = CornerRadius(22.dp.toPx()),
                                    size = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()),
                                    topLeft = Offset(-6.dp.toPx(), -6.dp.toPx())
                                )
                            }
                            .clip(bubbleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF78350F), Color(0xFFFFD700).copy(alpha = 0.5f + shimmerOffset * 0.5f),
                                        Color(0xFF78350F)
                                    ),
                                    start = Offset(shimmerOffset * 600f - 150f, 0f),
                                    end = Offset(shimmerOffset * 600f + 150f, 100f)
                                )
                            )
                    }
                    // 중장~준장: 골드 글로우
                    EliteRank.LIEUTENANT_GENERAL, EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0xFFFBBF24).copy(alpha = glowAlpha * 0.4f),
                                    cornerRadius = CornerRadius(20.dp.toPx()),
                                    size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx())
                                )
                            }
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                    // 대령~소령: 보라색 글로우
                    EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0xFFA78BFA).copy(alpha = glowAlpha * 0.35f),
                                    cornerRadius = CornerRadius(18.dp.toPx()),
                                    size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                                    topLeft = Offset(-3.dp.toPx(), -3.dp.toPx())
                                )
                            }
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                    // 대위~소위: 초록색 글로우
                    EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0xFF34D399).copy(alpha = 0.25f),
                                    cornerRadius = CornerRadius(17.dp.toPx()),
                                    size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx())
                                )
                            }
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                    // 원사~하사: 파란색 그림자
                    EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT, EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> {
                        Modifier
                            .drawBehind {
                                drawRoundRect(
                                    color = Color(0xFF60A5FA).copy(alpha = 0.2f),
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    size = Size(size.width + 3.dp.toPx(), size.height + 3.dp.toPx()),
                                    topLeft = Offset(-1.dp.toPx(), -1.dp.toPx())
                                )
                            }
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                    // 병장~훈련병: 기본
                    else -> {
                        Modifier
                            .clip(bubbleShape)
                            .background(bubbleColor)
                    }
                }

                // URL 포함 여부 확인
                val containsUrl = LinkPreviewFetcher.containsUrl(message.message)
                val extractedUrl = if (containsUrl) LinkPreviewFetcher.extractUrl(message.message) else null

                // 말풍선 (롱프레스로 이모지 선택)
                Box(
                    modifier = Modifier
                        .widthIn(max = 250.dp)
                        .combinedClickable(
                            onClick = onReply,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showReactionPicker = true
                            }
                        )
                        .then(bubbleModifier)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        // 답장 대상 표시
                        if (message.replyToNickname != null && message.replyToMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = message.replyToNickname,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = message.replyToMessage,
                                        fontSize = 12.sp,
                                        color = textColor.copy(alpha = 0.6f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // 대장 등급은 텍스트에도 골드 발광 효과
                        if (messageRank == EliteRank.GENERAL) {
                            Text(
                                text = message.message,
                                fontSize = 15.sp,
                                color = Color(0xFFFFF7ED),  // 밝은 크림색
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 12f
                                    )
                                )
                            )
                        } else {
                            Text(
                                text = message.message,
                                fontSize = 15.sp,
                                color = textColor
                            )
                        }

                        // 링크 미리보기
                        extractedUrl?.let { url ->
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isOwnMessage || messageRank.ordinal >= EliteRank.PRIVATE_SECOND.ordinal) {
                                LinkPreviewCard(url = url)
                            } else {
                                LinkPreviewCardLight(url = url)
                            }
                        }
                    }
                } // Box 끝

                // 다른 사람 메시지: 말풍선 → 시간
                if (!isOwnMessage) {
                    Text(
                        text = timeString,
                        fontSize = 10.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            } // Row (말풍선 + 시간) 끝

            // 리액션 표시
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.reactions.forEach { (emoji, userIds) ->
                        val isReacted = userIds.contains(currentUserId)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isReacted) TossBlue.copy(alpha = 0.2f)
                                    else BackgroundGray
                                )
                                .clickable { onToggleReaction(emoji) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 12.sp)
                                if (userIds.size > 1) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${userIds.size}",
                                        fontSize = 11.sp,
                                        color = if (isReacted) TossBlue else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 롱프레스 이모지 선택 버블 (Popup)
            if (showReactionPicker) {
                Popup(
                    alignment = if (isOwnMessage) Alignment.TopEnd else Alignment.TopStart,
                    offset = IntOffset(0, -120),
                    onDismissRequest = { showReactionPicker = false }
                ) {
                    Row(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableReactions.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onToggleReaction(emoji)
                                        showReactionPicker = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                        // 신고 버튼 (다른 사람 메시지에만)
                        if (!isOwnMessage) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(DividerGray)
                            )
                            Text(
                                text = "🚨",
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        showReactionPicker = false
                                        onLongPress()
                                    }
                                    .padding(4.dp)
                            )
                        }
                        // 관리자 삭제 버튼
                        if (isAdminMode && !isOwnMessage) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(DividerGray)
                            )
                            Text(
                                text = "🗑️",
                                fontSize = 22.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        showReactionPicker = false
                                        showDeleteConfirm = true
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            // 관리자 메시지 삭제 확인 다이얼로그
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("메시지 삭제") },
                    text = { Text("이 메시지를 삭제하시겠습니까?\n삭제된 메시지는 복구할 수 없습니다.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDelete()
                                showDeleteConfirm = false
                            }
                        ) {
                            Text("삭제", color = Color.Red)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("취소")
                        }
                    }
                )
            }

            // 스팸/광고 경고 표시 (말풍선 아래)
            if (message.warning != null) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️ ${message.warning}",
                        fontSize = 11.sp,
                        color = StatusYellow
                    )
                }
            }
        }
}

@Composable
private fun SystemMessageItem(
    message: ChatMessage,
    currentUserNickname: String = "",  // 현재 유저 닉네임
    onWelcomeTap: ((String) -> Unit)? = null  // 환영 메시지 전송 콜백
) {
    // 메시지 타입에 따른 스타일 결정
    val messageText = message.message
    val isEntryMessage = messageText.contains("합류했습니다")

    // 닉네임 추출 (입장 메시지에서)
    val nickname = if (isEntryMessage) {
        messageText.replace("님이 전우회에 합류했습니다", "").trim()
    } else null

    // 본인 입장 메시지인지 확인
    val isOwnEntry = nickname == currentUserNickname

    // 환영 가능 여부 (다른 사람 입장 메시지만)
    val canWelcome = isEntryMessage && !isOwnEntry && onWelcomeTap != null && nickname != null

    val (icon, bgColor, textColor, borderColor) = when {
        isEntryMessage -> {
            // 입장 - 흰 배경 + 초록 테두리 + 진한 텍스트
            listOf("👋", Color.White, TextPrimary, StatusGreen)
        }
        messageText.contains("복귀했습니다") -> {
            // 복귀 - 흰 배경 + 파란 테두리 + 진한 텍스트
            listOf("⚡", Color.White, TextPrimary, TossBlue)
        }
        messageText.contains("퇴장했습니다") || messageText.contains("추방되었습니다") -> {
            // 퇴장 - 흰 배경 + 회색 테두리 + 연한 텍스트
            listOf("👋", Color.White, TextTertiary, DividerGray)
        }
        else -> {
            // 기타 시스템 메시지
            listOf("📢", Color.White, TextSecondary, DividerGray)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bgColor as Color,
                border = BorderStroke(1.dp, borderColor as Color),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon as String,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = messageText,
                        fontSize = 13.sp,
                        color = textColor as Color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 환영하기 버튼 (다른 사람 입장 메시지에만 표시)
            if (canWelcome) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusGreen,
                    modifier = Modifier.clickable { onWelcomeTap!!("환영한다 전우여, $nickname!") }
                ) {
                    Text(
                        text = "🎉 환영하기",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPollClick: () -> Unit = {},
    onlineUsers: List<EliteUser> = emptyList(),
    modifier: Modifier = Modifier
) {
    // @멘션 자동완성
    val mentionQuery = remember(value) {
        val lastAtIndex = value.lastIndexOf('@')
        if (lastAtIndex >= 0 && !value.substring(lastAtIndex).contains(' ')) {
            value.substring(lastAtIndex + 1)
        } else null
    }
    val filteredUsers = remember(mentionQuery, onlineUsers) {
        if (mentionQuery != null && mentionQuery.isNotEmpty()) {
            onlineUsers.filter { it.nickname.contains(mentionQuery, ignoreCase = true) }.take(5)
        } else emptyList()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BackgroundWhite.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Column {
            // 멘션 자동완성 목록
            AnimatedVisibility(
                visible = filteredUsers.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundGray)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "멘션할 사용자",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    filteredUsers.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val lastAtIndex = value.lastIndexOf('@')
                                    val newValue = value.substring(0, lastAtIndex + 1) + user.nickname + " "
                                    onValueChange(newValue)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "@${user.nickname}",
                                fontSize = 14.sp,
                                color = TossBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 투표 버튼
                IconButton(
                    onClick = onPollClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(text = "🗳️", fontSize = 20.sp)
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            // Enter 키: 전송 (Shift 안 누른 상태)
                            // Shift+Enter: 줄바꿈
                            if (keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown) {
                                if (!keyEvent.isShiftPressed) {
                                    // Enter만 누르면 전송
                                    if (value.isNotBlank()) {
                                        onSend()
                                    }
                                    true // 이벤트 소비
                                } else {
                                    // Shift+Enter는 줄바꿈 (기본 동작)
                                    false
                                }
                            } else {
                                false
                            }
                        },
                    placeholder = {
                        Text(
                            text = "메시지를 입력하세요 (@멘션)",
                            color = TextTertiary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TossBlue,
                        unfocusedBorderColor = BorderGray,
                        cursorColor = TossBlue,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

            Spacer(modifier = Modifier.width(8.dp))

            val hasText = value.isNotBlank()
            val buttonScale by animateFloatAsState(
                targetValue = if (hasText) 1.1f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "sendScale"
            )

                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (hasText) TossBlue else DividerGray)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "전송",
                        tint = if (hasText) Color.White else TextTertiary,
                        modifier = Modifier.size((20 * buttonScale).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RankUpCelebration(
    newRank: EliteRank,
    visible: Boolean,
    onDismiss: () -> Unit,
    bannerAdView: @Composable () -> Unit = {}
) {
    // 자동 닫힘 카운트다운
    var countdown by remember { mutableIntStateOf(5) }

    // visible이 true가 될 때 카운트다운 리셋
    LaunchedEffect(visible) {
        if (visible) {
            countdown = 5
        }
    }

    // 병사/훈련병 여부 확인
    val isEnlisted = newRank in listOf(
        EliteRank.TRAINEE, EliteRank.PRIVATE_SECOND, EliteRank.PRIVATE_FIRST,
        EliteRank.CORPORAL, EliteRank.SERGEANT
    )

    // 버블 색상과 통일된 배지 색상
    val badgeColors = when (newRank) {
        // 장성 (골드 그라데이션)
        EliteRank.GENERAL -> listOf(Color(0xFF78350F), Color(0xFFD97706), Color(0xFFFBBF24))
        EliteRank.LIEUTENANT_GENERAL -> listOf(Color(0xFF92400E), Color(0xFFD97706))
        EliteRank.MAJOR_GENERAL -> listOf(Color(0xFFB45309), Color(0xFFD97706))
        EliteRank.BRIGADIER_GENERAL -> listOf(Color(0xFFD97706), Color(0xFFF59E0B))
        // 영관급 (보라색 그라데이션)
        EliteRank.COLONEL -> listOf(Color(0xFF581C87), Color(0xFF7C3AED))
        EliteRank.LIEUTENANT_COLONEL -> listOf(Color(0xFF6D28D9), Color(0xFF8B5CF6))
        EliteRank.MAJOR -> listOf(Color(0xFF7C3AED), Color(0xFFA78BFA))
        // 위관급 (초록색 그라데이션)
        EliteRank.CAPTAIN -> listOf(Color(0xFF047857), Color(0xFF10B981))
        EliteRank.FIRST_LIEUTENANT -> listOf(Color(0xFF059669), Color(0xFF34D399))
        EliteRank.SECOND_LIEUTENANT -> listOf(Color(0xFF10B981), Color(0xFF6EE7B7))
        // 부사관 (파란색 그라데이션)
        EliteRank.SERGEANT_MAJOR -> listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
        EliteRank.MASTER_SERGEANT -> listOf(Color(0xFF1D4ED8), Color(0xFF60A5FA))
        EliteRank.SERGEANT_FIRST -> listOf(Color(0xFF2563EB), Color(0xFF93C5FD))
        EliteRank.STAFF_SERGEANT -> listOf(Color(0xFF3B82F6), Color(0xFFBFDBFE))
        // 병사/훈련병 (카카오톡 스타일 노란색)
        else -> listOf(Color(0xFFFEE500), Color(0xFFFEE500))
    }

    // 텍스트 색상 (병사/훈련병은 검은색)
    val textColor = if (isEnlisted) Color.Black else Color.White

    // 계급별 아이콘
    val rankIcon = when (newRank) {
        EliteRank.GENERAL -> "⭐"
        EliteRank.LIEUTENANT_GENERAL, EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> "★"
        EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> "◆"
        EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> "▲"
        EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT, EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> "■"
        else -> null
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            CelebrationParticles()
            CelebrationLightning()

            // 모달 카드
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 상단 콘텐츠 영역 (패딩 적용)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                    // X 버튼 + 카운트다운 (우측 상단)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 남은 시간 표시
                        Text(
                            text = "${countdown}초",
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "닫기",
                                tint = TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Text(
                        text = "레벨 업!",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(badgeColors))
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (rankIcon != null) {
                                Text(
                                    text = rankIcon,
                                    fontSize = 20.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = newRank.koreanName,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = newRank.description,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    }

                    // 배너 광고 영역 (패딩 없이 전체 너비 사용)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        bannerAdView()
                    }
                }
            }
        }

        // 5초 후 자동 닫힘 (1초마다 카운트다운 업데이트)
        LaunchedEffect(visible) {
            if (visible) {
                repeat(5) {
                    delay(1000)
                    countdown--
                }
                onDismiss()
            }
        }
    }
}

@Composable
private fun CelebrationParticles() {
    data class CelebrationParticle(
        val id: Int,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Color
    )

    val colors = listOf(TossBlue, TossBlueLight, TossBlueDark, StatusGreen, StatusYellow)

    var particles by remember {
        mutableStateOf(
            List(50) { index ->
                CelebrationParticle(
                    id = index,
                    x = 0.5f,
                    y = 0.5f,
                    vx = (Random.nextFloat() - 0.5f) * 0.025f,
                    vy = (Random.nextFloat() - 0.5f) * 0.025f,
                    size = Random.nextFloat() * 10f + 4f,
                    color = colors.random()
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "celebrateParticles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            p.copy(
                x = p.x + p.vx,
                y = p.y + p.vy,
                vy = p.vy + 0.0004f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            if (p.x in 0f..1f && p.y in 0f..1f) {
                drawCircle(
                    color = p.color.copy(alpha = 0.7f),
                    radius = p.size,
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }
    }
}

@Composable
private fun CelebrationLightning() {
    var bolts by remember { mutableStateOf<List<QuickBolt>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(200, 500))
            bolts = List(Random.nextInt(2, 5)) { generateQuickBolt() }
            delay(80)
            bolts = emptyList()
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        bolts.forEach { bolt ->
            drawQuickBolt(bolt)
        }
    }
}

@Composable
private fun DangerWarningBar(countdown: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "danger")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flash"
    )

    val bgColor = if (countdown <= 10) StatusRed else StatusYellow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = flashAlpha))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (countdown <= 10) "퇴장까지" else "충전 필요!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${countdown}초",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "지금 충전하세요!",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun DangerWarningOverlay(countdown: Int) {
    // 화면 가장자리 빨간 테두리 효과
    val infiniteTransition = rememberInfiniteTransition(label = "dangerBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (countdown <= 10) 0.6f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (countdown <= 10) 200 else 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val borderColor = if (countdown <= 10) StatusRed else StatusYellow

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 상단 그라데이션
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderAlpha),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.15f
            )
        )

        // 하단 그라데이션
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    borderColor.copy(alpha = borderAlpha)
                ),
                startY = size.height * 0.85f,
                endY = size.height
            )
        )

        // 좌측 그라데이션
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    borderColor.copy(alpha = borderAlpha * 0.7f),
                    Color.Transparent
                ),
                startX = 0f,
                endX = size.width * 0.1f
            )
        )

        // 우측 그라데이션
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    borderColor.copy(alpha = borderAlpha * 0.7f)
                ),
                startX = size.width * 0.9f,
                endX = size.width
            )
        )
    }
}

@Composable
private fun NicknameEditDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "닉네임 변경",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "다른 전우들에게 보여질 이름이에요",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        nickname = it
                        isError = it.isBlank() || it.length > 20
                    },
                    label = { Text("닉네임") },
                    placeholder = { Text("2~20자 이내") },
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("2~20자 이내로 입력해주세요", color = StatusRed) }
                    } else null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TossBlue,
                        cursorColor = TossBlue,
                        focusedLabelColor = TossBlue
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nickname.isNotBlank() && nickname.length in 1..20) {
                        onConfirm(nickname.trim())
                    }
                },
                enabled = nickname.isNotBlank() && nickname.length in 1..20
            ) {
                Text("변경", color = if (nickname.isNotBlank() && nickname.length in 1..20) TossBlue else TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ReportDialog(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "메시지 신고",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "신고 사유를 선택해주세요",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 신고 대상 메시지 미리보기
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundGray)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = message.nickname,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = message.message,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 신고 사유 선택
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason }
                            .background(
                                if (selectedReason == reason) TossBlue.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TossBlue
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason.displayName,
                            fontSize = 14.sp,
                            color = if (selectedReason == reason) TossBlue else TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedReason?.let { onReport(it.name) }
                },
                enabled = selectedReason != null
            ) {
                Text(
                    text = "신고하기",
                    color = if (selectedReason != null) StatusRed else TextTertiary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ReplyPreviewBar(
    replyingTo: ChatMessage,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BackgroundGray
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 파란색 바
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TossBlue)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 답장 대상 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${replyingTo.nickname}에게 답장",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TossBlue
                )
                Text(
                    text = replyingTo.message,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 닫기 버튼
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "답장 취소",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PollMessageItem(
    message: ChatMessage,
    isOwnMessage: Boolean,
    currentUserId: String,
    onVote: (Int) -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    // 총 투표 수 계산
    val totalVotes = message.pollVotes.values.sumOf { it.size }

    // 사용자가 투표한 옵션 찾기
    val userVotedOption = message.pollVotes.entries.find { (_, userIds) ->
        userIds.contains(currentUserId)
    }?.key?.toIntOrNull()

    // 투표 종료 여부 및 남은 시간 계산
    var remainingTimeText by remember { mutableStateOf("") }
    var isPollEnded by remember { mutableStateOf(message.isPollEnded) }

    // 1초마다 남은 시간 업데이트
    LaunchedEffect(message.pollEndTime) {
        if (message.pollEndTime > 0) {
            while (true) {
                val remaining = message.pollEndTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    isPollEnded = true
                    remainingTimeText = "종료됨"
                    break
                } else {
                    isPollEnded = false
                    val seconds = (remaining / 1000) % 60
                    val minutes = (remaining / 1000 / 60) % 60
                    remainingTimeText = if (minutes > 0) {
                        "${minutes}분 ${seconds}초 남음"
                    } else {
                        "${seconds}초 남음"
                    }
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInHorizontally(
            initialOffsetX = { if (isOwnMessage) it else -it }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            Column(
                horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                if (!isOwnMessage) {
                    Text(
                        text = message.nickname,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // 투표 카드
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPollEnded) BackgroundGray else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isPollEnded) 0.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 투표 아이콘 + 질문
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isPollEnded) "🔒" else "🗳️",
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message.pollQuestion ?: "",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPollEnded) TextSecondary else TextPrimary
                            )
                        }

                        // 남은 시간 또는 종료 표시
                        if (message.pollEndTime > 0) {
                            Text(
                                text = remainingTimeText,
                                fontSize = 11.sp,
                                color = if (isPollEnded) StatusRed else StatusGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 투표 옵션들
                        message.pollOptions.forEachIndexed { index, option ->
                            val voteCount = message.pollVotes[index.toString()]?.size ?: 0
                            val votePercentage = if (totalVotes > 0) voteCount * 100 / totalVotes else 0
                            val isVoted = userVotedOption == index

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isPollEnded && isVoted -> TossBlue.copy(alpha = 0.1f)
                                            isPollEnded -> DividerGray
                                            isVoted -> TossBlue.copy(alpha = 0.1f)
                                            else -> BackgroundGray
                                        }
                                    )
                                    .then(
                                        if (isPollEnded) Modifier
                                        else Modifier.clickable { onVote(index) }
                                    )
                            ) {
                                // 투표율 배경 바
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(votePercentage / 100f)
                                        .height(40.dp)
                                        .background(
                                            if (isVoted) TossBlue.copy(alpha = 0.2f)
                                            else DividerGray
                                        )
                                )
                                // 옵션 텍스트
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 14.sp,
                                        color = when {
                                            isPollEnded && isVoted -> TossBlue
                                            isPollEnded -> TextTertiary
                                            isVoted -> TossBlue
                                            else -> TextPrimary
                                        },
                                        fontWeight = if (isVoted) FontWeight.Medium else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${votePercentage}%",
                                        fontSize = 12.sp,
                                        color = when {
                                            isPollEnded && isVoted -> TossBlue
                                            isPollEnded -> TextTertiary
                                            isVoted -> TossBlue
                                            else -> TextSecondary
                                        }
                                    )
                                }
                            }
                        }

                        // 총 투표 수
                        Text(
                            text = "${totalVotes}명 참여",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // 시간
                Text(
                    text = timeString,
                    fontSize = 10.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CreatePollDialog(
    onDismiss: () -> Unit,
    onCreatePoll: (String, List<String>, Int) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }

    // 투표 지속 시간 옵션
    val durationOptions = listOf(1, 3, 5, 10)
    var selectedDuration by remember { mutableIntStateOf(5) }  // 기본 5분
    var showDurationDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🗳️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "투표 만들기", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                // 질문 입력
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("질문") },
                    placeholder = { Text("무엇을 물어볼까요?") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TossBlue,
                        cursorColor = TossBlue,
                        focusedLabelColor = TossBlue
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 투표 지속 시간 선택
                Text(
                    text = "투표 종료 시간",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box {
                    OutlinedButton(
                        onClick = { showDurationDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TossBlue
                        )
                    ) {
                        Text("${selectedDuration}분 후 종료")
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showDurationDropdown,
                        onDismissRequest = { showDurationDropdown = false }
                    ) {
                        durationOptions.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text("${duration}분") },
                                onClick = {
                                    selectedDuration = duration
                                    showDurationDropdown = false
                                },
                                leadingIcon = {
                                    if (duration == selectedDuration) {
                                        Text("✓", color = TossBlue)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "선택지 (최소 2개, 최대 4개)",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 선택지 입력
                options.forEachIndexed { index, option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                options = options.toMutableList().apply { this[index] = newValue }
                            },
                            placeholder = { Text("선택지 ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TossBlue,
                                cursorColor = TossBlue
                            ),
                            singleLine = true
                        )
                        // 삭제 버튼 (3개 이상일 때만)
                        if (options.size > 2) {
                            IconButton(
                                onClick = {
                                    options = options.toMutableList().apply { removeAt(index) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "삭제",
                                    tint = TextTertiary
                                )
                            }
                        }
                    }
                }

                // 선택지 추가 버튼
                if (options.size < 4) {
                    TextButton(
                        onClick = { options = options + "" },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("+ 선택지 추가", color = TossBlue)
                    }
                }
            }
        },
        confirmButton = {
            val isValid = question.isNotBlank() && options.count { it.isNotBlank() } >= 2
            TextButton(
                onClick = {
                    if (isValid) {
                        onCreatePoll(question, options.filter { it.isNotBlank() }, selectedDuration)
                    }
                },
                enabled = isValid
            ) {
                Text(
                    text = "투표 시작",
                    color = if (isValid) TossBlue else TextTertiary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun LeaveConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "전우회 나가기",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "정말 나가시겠어요?\n현재 세션 기록은 저장되지 않아요.",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("나가기", color = StatusRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

/**
 * 업적 달성 팝업
 */
@Composable
private fun AchievementUnlockedPopup(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = achievement != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        achievement?.let { ach ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 이모지
                        Text(
                            text = ach.emoji,
                            fontSize = 40.sp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "업적 달성!",
                                fontSize = 12.sp,
                                color = TossBlue,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = ach.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = ach.description,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 3초 후 자동 닫힘
            LaunchedEffect(ach) {
                delay(3000)
                onDismiss()
            }
        }
    }
}

/**
 * 위기 탈출 축하 이펙트
 */
@Composable
private fun CrisisEscapeCelebration(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    // 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "escape")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StatusGreen.copy(alpha = 0.3f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(32.dp)
            ) {
                // 땀방울 이모지 (크게)
                Text(
                    text = "😅",
                    fontSize = (64 * scale).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "휴... 살았다!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "위기를 탈출했어요",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        // 2초 후 자동 닫힘
        LaunchedEffect(visible) {
            if (visible) {
                delay(2000)
                onDismiss()
            }
        }
    }
}

/**
 * 실시간 이벤트 알림 배너
 */
@Composable
private fun ChatEventBanner(
    event: ChatEvent?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = event != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it })
    ) {
        event?.let { chatEvent ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 100.dp)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.TopCenter
            ) {
                when (chatEvent) {
                    // 승급 알림 - 중앙 RankUpCelebration만 사용 (상단 배너 제거)
                    is ChatEvent.UserRankUp -> {
                        // 상단 배너 표시 안 함 (중앙 축하 UI에서 처리)
                    }
                    // 위기 탈출 - UI로 구현
                    is ChatEvent.UserCrisisEscape -> {
                        CrisisEscapeEventBanner(nickname = chatEvent.nickname)
                    }
                    // 기타 이벤트
                    else -> {
                        val (icon, message, bgColor) = when (chatEvent) {
                            is ChatEvent.UserJoined -> Triple(
                                "👋",
                                "${chatEvent.nickname}님이 입장했어요!",
                                TossBlue.copy(alpha = 0.8f)
                            )
                            is ChatEvent.ComboAchieved -> Triple(
                                "🔥",
                                "${chatEvent.count}콤보 달성!",
                                Color(0xFFFF6B35).copy(alpha = 0.9f)
                            )
                            is ChatEvent.PersonalHourMilestone -> Triple(
                                "⏰",
                                "${chatEvent.hours}시간 생존 달성!",
                                Color(0xFF6366F1).copy(alpha = 0.9f)
                            )
                            ChatEvent.HourlyChime -> Triple(
                                "🔔",
                                "정각입니다!",
                                Color(0xFF8B5CF6).copy(alpha = 0.9f)
                            )
                            ChatEvent.MidnightSpecial -> Triple(
                                "🌙",
                                "자정이에요! 심야 전우회 화이팅!",
                                Color(0xFF1E1B4B).copy(alpha = 0.95f)
                            )
                            is ChatEvent.MessageMilestone -> Triple(
                                "💬",
                                "채팅방 ${chatEvent.count}번째 메시지 달성!",
                                TossBlue.copy(alpha = 0.9f)
                            )
                            is ChatEvent.UserCountMilestone -> Triple(
                                "👥",
                                "동시 접속자 ${chatEvent.count}명 달성!",
                                StatusGreen.copy(alpha = 0.9f)
                            )
                            is ChatEvent.NewSurvivalRecord -> Triple(
                                "🏆",
                                "${chatEvent.nickname}님이 새로운 생존 기록 달성!",
                                Color(0xFFD97706).copy(alpha = 0.9f)
                            )
                            else -> Triple("", "", Color.Transparent)
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = icon,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = message,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 승급 이벤트 배너 (UI로 구현)
 */
@Composable
private fun RankUpEventBanner(
    nickname: String,
    newRank: EliteRank
) {
    // 계급에 따른 배경 그라데이션
    val bgGradient = when {
        newRank.ordinal >= EliteRank.BRIGADIER_GENERAL.ordinal -> {
            // 장성급: 골드 그라데이션
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF78350F),
                    Color(0xFFD97706),
                    Color(0xFFFBBF24),
                    Color(0xFFD97706),
                    Color(0xFF78350F)
                )
            )
        }
        newRank.ordinal >= EliteRank.MAJOR.ordinal -> {
            // 영관급: 보라색 그라데이션
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF581C87),
                    Color(0xFF7C3AED),
                    Color(0xFF581C87)
                )
            )
        }
        newRank.ordinal >= EliteRank.SECOND_LIEUTENANT.ordinal -> {
            // 위관급: 초록색 그라데이션
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF047857),
                    Color(0xFF10B981),
                    Color(0xFF047857)
                )
            )
        }
        newRank.ordinal >= EliteRank.STAFF_SERGEANT.ordinal -> {
            // 부사관: 파란색 그라데이션
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF1E40AF),
                    Color(0xFF3B82F6),
                    Color(0xFF1E40AF)
                )
            )
        }
        else -> {
            // 병사: 회색 그라데이션
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF475569),
                    Color(0xFF64748B),
                    Color(0xFF475569)
                )
            )
        }
    }

    // 별 개수 (장성급)
    val starCount = when (newRank) {
        EliteRank.GENERAL -> 4
        EliteRank.LIEUTENANT_GENERAL -> 3
        EliteRank.MAJOR_GENERAL -> 2
        EliteRank.BRIGADIER_GENERAL -> 1
        else -> 0
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgGradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 계급장 영역
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (starCount > 0) {
                        // 장성급: 별 표시
                        Row {
                            repeat(starCount.coerceAtMost(2)) {
                                Text(
                                    text = "★",
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (starCount > 2) {
                            Column {
                                Row {
                                    repeat(2) {
                                        Text(
                                            text = "★",
                                            fontSize = 14.sp,
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row {
                                    repeat(starCount - 2) {
                                        Text(
                                            text = "★",
                                            fontSize = 14.sp,
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // 일반 계급: 계급장 아이콘
                        Text(
                            text = "▲",
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "승급!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = nickname,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = " → ",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        // 계급 배지
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = newRank.koreanName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // 화살표 아이콘 (상승 표시)
                Text(
                    text = "↑",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 위기 탈출 이벤트 배너 (UI로 구현)
 */
@Composable
private fun CrisisEscapeEventBanner(
    nickname: String
) {
    // 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "escapeBanner")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StatusGreen.copy(alpha = pulseAlpha),
                            StatusGreen.copy(alpha = 0.8f),
                            StatusGreen.copy(alpha = pulseAlpha)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 하트 아이콘 (생존 표시)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♥",
                        fontSize = 28.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "위기 탈출!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${nickname}님이 살아남았어요!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 체크 아이콘
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 콤보 이펙트 오버레이
 */
@Composable
private fun ComboEffectOverlay(
    comboState: ComboState
) {
    // 콤보 표시 애니메이션
    val scale by animateFloatAsState(
        targetValue = if (comboState.showComboEffect) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "comboScale"
    )

    AnimatedVisibility(
        visible = comboState.showComboEffect,
        enter = fadeIn() + scaleIn(initialScale = 0.3f),
        exit = fadeOut() + scaleOut(targetScale = 1.5f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 불꽃 파티클 효과
            ComboFireParticles()

            // 콤보 숫자
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔥",
                    fontSize = (48 * scale).sp
                )
                Text(
                    text = "${comboState.currentCombo}",
                    fontSize = (64 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35),
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color(0xFFFF6B35).copy(alpha = 0.6f),
                            offset = Offset(0f, 0f),
                            blurRadius = 20f
                        )
                    )
                )
                Text(
                    text = "COMBO!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
            }
        }
    }

    // 콤보 카운터 (항상 표시, 3 이상일 때)
    AnimatedVisibility(
        visible = comboState.currentCombo >= 3 && !comboState.showComboEffect,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF6B35).copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${comboState.currentCombo}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 콤보 불꽃 파티클
 */
@Composable
private fun ComboFireParticles() {
    data class FireParticle(
        val id: Int,
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Color
    )

    val fireColors = listOf(
        Color(0xFFFF6B35),
        Color(0xFFFF8C00),
        Color(0xFFFFD700),
        Color(0xFFFF4500)
    )

    var particles by remember {
        mutableStateOf(
            List(30) { index ->
                FireParticle(
                    id = index,
                    x = 0.5f,
                    y = 0.5f,
                    vx = (Random.nextFloat() - 0.5f) * 0.03f,
                    vy = -Random.nextFloat() * 0.02f - 0.01f,
                    size = Random.nextFloat() * 15f + 5f,
                    color = fireColors.random()
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(50, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fireTime"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            p.copy(
                x = p.x + p.vx,
                y = p.y + p.vy
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            if (p.x in 0f..1f && p.y in 0f..1f) {
                drawCircle(
                    color = p.color.copy(alpha = 0.7f),
                    radius = p.size,
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }
    }
}

/**
 * 자정 특별 이벤트 오버레이
 */
@Composable
private fun MidnightSpecialOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    // 별 반짝임 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "midnight")
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(1000)),
        exit = fadeOut(animationSpec = tween(1000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0D24).copy(alpha = 0.95f),
                            Color(0xFF1E1B4B).copy(alpha = 0.9f)
                        )
                    )
                )
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // 별 파티클
            MidnightStars(alpha = starAlpha)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1B4B).copy(alpha = 0.8f))
                    .padding(32.dp)
            ) {
                Text(
                    text = "🌙✨",
                    fontSize = 56.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "자정이에요!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "심야 전우회 화이팅!",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        LaunchedEffect(visible) {
            if (visible) {
                delay(5000)
                onDismiss()
            }
        }
    }
}

/**
 * 자정 별 파티클
 */
@Composable
private fun MidnightStars(alpha: Float) {
    data class Star(
        val x: Float,
        val y: Float,
        val size: Float,
        val twinkleOffset: Float
    )

    val stars = remember {
        List(50) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 3f + 1f,
                twinkleOffset = Random.nextFloat()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            val starAlpha = ((sin((alpha * 2 * Math.PI + star.twinkleOffset * Math.PI).toFloat()) + 1) / 2) * 0.8f + 0.2f
            drawCircle(
                color = Color.White.copy(alpha = starAlpha),
                radius = star.size,
                center = Offset(star.x * size.width, star.y * size.height)
            )
        }
    }
}

/**
 * 정각 알림 오버레이
 */
@Composable
private fun HourlyChimeOverlay(
    visible: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chime")
    val bellScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bellScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 0.5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 150.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "🔔",
                fontSize = (48 * bellScale).sp
            )
        }
    }
}

/**
 * 고계급 스파클 효과 (장성급 닉네임 주변)
 */
@Composable
private fun HighRankSparkles(animationProgress: Float) {
    val sparkles = remember {
        List(8) { index ->
            val angle = (index * 45f) * (Math.PI / 180f).toFloat()
            Triple(
                cos(angle) * 0.5f + 0.5f,  // x (0~1 범위)
                sin(angle) * 0.5f + 0.5f,  // y (0~1 범위)
                Random.nextFloat() * 0.5f + 0.3f  // 크기 변동
            )
        }
    }

    Canvas(
        modifier = Modifier
            .size(width = 120.dp, height = 24.dp)
    ) {
        sparkles.forEachIndexed { index, (baseX, baseY, sizeFactor) ->
            // 애니메이션으로 위치 약간 변동
            val offsetPhase = (animationProgress + index * 0.125f) % 1f
            val alpha = (sin(offsetPhase * 2 * Math.PI.toFloat()) + 1) / 2 * 0.8f

            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = alpha),
                radius = 2f * sizeFactor,
                center = Offset(
                    baseX * size.width,
                    baseY * size.height
                )
            )
        }
    }
}

/**
 * 개인 시간 마일스톤 오버레이
 */
@Composable
private fun PersonalMilestoneOverlay(
    hours: Int?,
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (hours != null) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "milestoneScale"
    )

    AnimatedVisibility(
        visible = hours != null,
        enter = fadeIn() + scaleIn(initialScale = 0.5f),
        exit = fadeOut() + scaleOut(targetScale = 1.2f)
    ) {
        hours?.let { h ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6366F1).copy(alpha = 0.3f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(32.dp)
                ) {
                    Text(
                        text = "⏰",
                        fontSize = (56 * scale).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${h}시간 생존!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6366F1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "대단해요! 계속 버텨주세요!",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            LaunchedEffect(h) {
                delay(3000)
                onDismiss()
            }
        }
    }
}

// ========== 관리자 패널 ==========

/**
 * 관리자 패널 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminPanelDialog(
    onlineUsers: List<EliteUser>,
    currentUserId: String,
    onKickUser: (EliteUser) -> Unit,
    onChangeUserRank: (EliteUser, EliteRank) -> Unit,
    onSendNotice: () -> Unit,
    onDismiss: () -> Unit,
    // 신고 관리
    reports: List<Report> = emptyList(),
    onHandleReport: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onDismissReport: (String) -> Unit = {}
) {
    var selectedUser by remember { mutableStateOf<EliteUser?>(null) }
    var showRankDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }  // 0: 사용자, 1: 신고

    // 대기 중인 신고 수
    val pendingReportsCount = reports.count { it.status == ReportStatus.PENDING.name }

    // 계급 선택 다이얼로그
    if (showRankDialog && selectedUser != null) {
        RankSelectionDialog(
            user = selectedUser!!,
            onSelectRank = { rank ->
                onChangeUserRank(selectedUser!!, rank)
                showRankDialog = false
            },
            onDismiss = { showRankDialog = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔐", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "관리자 패널",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = StatusRed
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                // 탭 선택
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 사용자 탭
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("사용자 (${onlineUsers.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TossBlue.copy(alpha = 0.2f),
                            selectedLabelColor = TossBlue
                        )
                    )
                    // 신고 탭
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("신고")
                                if (pendingReportsCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(StatusRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (pendingReportsCount > 9) "9+" else pendingReportsCount.toString(),
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusRed.copy(alpha = 0.2f),
                            selectedLabelColor = StatusRed
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 공지 버튼
                Button(
                    onClick = onSendNotice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TossBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("공지 보내기")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 탭 내용
                when (selectedTab) {
                    0 -> {
                        // 사용자 탭
                        Text(
                            text = "접속 중인 사용자",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(onlineUsers.filter { it.userId != currentUserId }) { user ->
                                AdminUserItem(
                                    user = user,
                                    onKick = { onKickUser(user) },
                                    onChangeRank = {
                                        selectedUser = user
                                        showRankDialog = true
                                    }
                                )
                            }
                        }

                        if (onlineUsers.filter { it.userId != currentUserId }.isEmpty()) {
                            Text(
                                text = "다른 사용자가 없습니다",
                                fontSize = 14.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                    1 -> {
                        // 신고 탭
                        Text(
                            text = "신고 목록",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (reports.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("✅", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "신고가 없습니다",
                                    fontSize = 14.sp,
                                    color = TextTertiary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(reports) { report ->
                                    AdminReportItem(
                                        report = report,
                                        onDelete = { onHandleReport(report.id, report.messageId, true) },
                                        onDismiss = { onDismissReport(report.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AdminReportItem(
    report: Report,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPending = report.status == ReportStatus.PENDING.name
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) StatusRed.copy(alpha = 0.05f) else BackgroundGray
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 상태 및 시간
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPending) StatusRed else StatusGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPending) "대기중" else "처리됨",
                        fontSize = 11.sp,
                        color = if (isPending) StatusRed else StatusGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = dateFormat.format(Date(report.timestamp)),
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 신고 사유
            Row {
                Text(
                    text = "사유: ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = report.reason,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 신고 대상
            Text(
                text = "대상: ${report.reportedNickname}",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 메시지 내용
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Text(
                    text = "\"${report.messageContent}\"",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 신고자
            Text(
                text = "신고자: ${report.reporterNickname}",
                fontSize = 11.sp,
                color = TextTertiary
            )

            // 버튼 (대기중인 경우만)
            if (isPending) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 기각 버튼
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("기각", fontSize = 12.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 삭제 버튼
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("메시지 삭제", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUserItem(
    user: EliteUser,
    onKick: () -> Unit,
    onChangeRank: () -> Unit
) {
    val rank = EliteRank.fromDuration(user.sessionDuration)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = rank.koreanName,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        // 계급 변경 버튼
        IconButton(onClick = onChangeRank) {
            Text("🎖️", fontSize = 20.sp)
        }

        // 강퇴 버튼
        IconButton(onClick = onKick) {
            Text("🚪", fontSize = 20.sp)
        }
    }
}

@Composable
private fun RankSelectionDialog(
    user: EliteUser,
    onSelectRank: (EliteRank) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        title = {
            Text(
                text = "${user.nickname} 계급 변경",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(EliteRank.entries.toList()) { rank ->
                    TextButton(
                        onClick = { onSelectRank(rank) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${rank.koreanName} (${rank.minMinutes}분 이상)",
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun AdminNoticeDialog(
    onSend: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var notice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        title = {
            Text(
                text = "📢 공지 보내기",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            OutlinedTextField(
                value = notice,
                onValueChange = { notice = it },
                label = { Text("공지 내용") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TossBlue,
                    focusedLabelColor = TossBlue
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (notice.isNotBlank()) onSend(notice) },
                colors = ButtonDefaults.buttonColors(containerColor = TossBlue)
            ) {
                Text("보내기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}
