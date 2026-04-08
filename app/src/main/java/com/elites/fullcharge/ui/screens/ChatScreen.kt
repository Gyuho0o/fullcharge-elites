package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.*
import com.elites.fullcharge.ui.components.ChargingParticles
import com.elites.fullcharge.ui.components.JoinLeaveIndicator
import com.elites.fullcharge.ui.components.RankInsignia
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// ChatScreen - v0 Tactical Military Design (완전 재작성)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    batteryState: BatteryState,
    messages: List<ChatMessage>,
    onlineUsers: List<EliteUser>,
    onlineUsersLoaded: Boolean = true,
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
    blockedUserIds: Set<String> = emptySet(),
    onBlockUser: (String) -> Unit = {},
    onUnblockUser: (String) -> Unit = {},
    onCreatePoll: (String, List<String>, Int) -> Unit = { _, _, _ -> },
    onVotePoll: (String, Int) -> Unit = { _, _ -> },
    isInDanger: Boolean = false,
    dangerCountdown: Int = 0,
    supplyUsed: Boolean = false,
    onRequestSupply: () -> Unit = {},
    bannerAdContent: @Composable () -> Unit = {},
    newlyUnlockedAchievement: Achievement? = null,
    onDismissAchievement: () -> Unit = {},
    unlockedAchievements: Set<String> = emptySet(),
    showCrisisEscapeCelebration: Boolean = false,
    onDismissCrisisEscape: () -> Unit = {},
    latestChatEvent: ChatEvent? = null,
    onDismissChatEvent: () -> Unit = {},
    comboState: ComboState = ComboState(),
    personalHourMilestone: Int? = null,
    isHourlyChime: Boolean = false,
    isMidnightSpecial: Boolean = false,
    onDismissTimeEvent: () -> Unit = {},
    isAdminMode: Boolean = false,
    onKickUser: (String, String) -> Unit = { _, _ -> },
    onChangeUserRank: (String, String, EliteRank) -> Unit = { _, _, _ -> },
    onSendAdminNotice: (String) -> Unit = {},
    onDeleteMessage: (String) -> Unit = {},
    reports: List<Report> = emptyList(),
    onHandleReport: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onDismissReport: (String) -> Unit = {},
    recentJoinCount: Int = 0,
    recentLeaveCount: Int = 0,
    showJoinLeaveIndicator: Boolean = false,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    var showRanking by remember { mutableStateOf(false) }
    var showAdmin by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // 대기 중인 신고 수 계산
    val pendingReportsCount = reports.count { it.status == ReportStatus.PENDING.name }

    val currentRank = EliteRank.fromDuration(sessionDuration)
    val batteryLevel = batteryState.level
    val isFullCharge = batteryLevel == 100
    val isCrisis = isInDanger

    // 이전 메시지 수 추적
    var previousMessageCount by remember { mutableIntStateOf(0) }

    // 새 메시지 자동 스크롤 (사용자가 맨 아래 근처에 있을 때만)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && messages.size > previousMessageCount) {
            // 새 메시지가 추가된 경우에만
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val isNearBottom = lastVisibleIndex >= messages.size - 3

            if (isNearBottom || previousMessageCount == 0) {
                delay(50)
                listState.animateScrollToItem(messages.size - 1)
            }
        }
        previousMessageCount = messages.size
    }

    // 나가기 확인 다이얼로그
    if (showLeaveConfirmDialog) {
        LeaveConfirmDialog(
            currentRank = currentRank,
            sessionDuration = sessionDuration,
            onConfirm = {
                showLeaveConfirmDialog = false
                onLeaveChat()
            },
            onDismiss = { showLeaveConfirmDialog = false }
        )
    }

    // 닉네임 수정 다이얼로그
    if (showNicknameDialog) {
        NicknameEditDialog(
            currentNickname = currentUserNickname,
            currentRank = currentRank,
            onConfirm = { newNickname ->
                showNicknameDialog = false
                onNicknameChange(newNickname)
            },
            onDismiss = { showNicknameDialog = false }
        )
    }

    // 승진 축하 모달
    if (latestChatEvent is ChatEvent.UserRankUp) {
        val rankUpEvent = latestChatEvent as ChatEvent.UserRankUp
        // 본인의 승진일 때만 모달 표시
        if (rankUpEvent.nickname == currentUserNickname) {
            RankUpDialog(
                newRank = rankUpEvent.newRank,
                bannerAdContent = bannerAdContent,
                onDismiss = onDismissChatEvent
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // 배경 파티클 효과 (충전 중일 때만)
        if (batteryState.isCharging) {
            ChargingParticles()
        }

        // ===== MAIN CONTENT =====
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // ===== HUD HEADER (v0: header) =====
            HudHeader(
                batteryLevel = batteryLevel,
                isCharging = batteryState.isCharging,
                onlineCount = onlineUsers.size,
                onlineCountLoaded = onlineUsersLoaded,
                showRanking = showRanking,
                onToggleRanking = { showRanking = !showRanking },
                currentRank = currentRank,
                sessionDuration = sessionDuration,
                currentUserNickname = currentUserNickname,
                onShowNicknameDialog = { showNicknameDialog = true },
                onLeaveChat = { showLeaveConfirmDialog = true },
                recentJoinCount = recentJoinCount,
                recentLeaveCount = recentLeaveCount,
                showJoinLeaveIndicator = showJoinLeaveIndicator,
                // 관리자 모드
                isAdminMode = isAdminMode,
                showAdmin = showAdmin,
                onToggleAdmin = { showAdmin = !showAdmin },
                pendingReportsCount = pendingReportsCount
            )

            // ===== RANKING BOARD PANEL (v0: toggleable) =====
            AnimatedVisibility(
                visible = showRanking,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RankingBoardPanel(
                    liveRanking = onlineUsers,
                    allTimeRanking = allTimeRecords,
                    currentUserId = currentUserId
                )
            }

            // ===== ADMIN PANEL (관리자 모드에서만) =====
            AnimatedVisibility(
                visible = showAdmin && isAdminMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AdminPanel(
                    reports = reports,
                    onlineUsers = onlineUsers,
                    currentUserId = currentUserId,
                    onHandleReport = onHandleReport,
                    onDismissReport = onDismissReport,
                    onKickUser = onKickUser,
                    onChangeUserRank = onChangeUserRank,
                    onSendAdminNotice = onSendAdminNotice,
                    onDeleteMessage = onDeleteMessage
                )
            }

            // ===== CHAT MESSAGES (v0: ScrollArea flex-1) =====
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    when {
                        // 공지 메시지는 AnnouncementMessage로 표시
                        message.isSystemMessage && message.message.startsWith("[공지]") ->
                            AnnouncementMessage(message = message)
                        // 일반 시스템 메시지
                        message.isSystemMessage -> SystemMessage(message = message)
                        else -> UserMessage(
                            message = message,
                            isMine = message.userId == currentUserId,
                            currentUserId = currentUserId,
                            onToggleReaction = onToggleReaction,
                            isBlocked = blockedUserIds.contains(message.userId),
                            onBlockUser = { onBlockUser(message.userId) },
                            onUnblockUser = { onUnblockUser(message.userId) }
                        )
                    }
                }
            }

            // ===== MESSAGE INPUT (v0: border-t) =====
            MessageInputSection(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        }

        // ===== CRISIS OVERLAY (v0: absolute inset-0 z-50) =====
        // Box에서 나중에 그려지는 요소가 위에 표시됨 (z-index)
        if (isCrisis) {
            CrisisOverlay(
                countdown = dangerCountdown,
                batteryLevel = batteryLevel,
                supplyUsed = supplyUsed,
                onRequestSupply = onRequestSupply
            )
        }
    }
}

// ===== CRISIS OVERLAY =====
// v0: absolute inset-0 z-50 bg-destructive/90 flex flex-col items-center justify-center animate-pulse
@Composable
private fun CrisisOverlay(
    countdown: Int,
    batteryLevel: Int,
    supplyUsed: Boolean = false,
    onRequestSupply: () -> Unit = {}
) {
    // 펄스 애니메이션들
    val infiniteTransition = rememberInfiniteTransition(label = "crisis_pulse")

    // 아이콘 스케일 펄스
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 아이콘 알파 펄스
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // 배경 펄스 (더 강렬한 효과)
    val bgPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_pulse"
    )

    // 카운트다운 글로우 펄스
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CrisisRed.copy(alpha = bgPulse)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Alert Triangle Icon with scale pulse
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer { alpha = pulseAlpha }
                ) {
                    val width = size.width
                    val height = size.height
                    val strokeWidth = 4.dp.toPx()

                    // Triangle path (외곽선)
                    val trianglePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width / 2, 0f)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }

                    drawPath(
                        path = trianglePath,
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )

                    // Exclamation mark
                    val centerX = width / 2
                    // 세로줄
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(centerX, height * 0.3f),
                        end = androidx.compose.ui.geometry.Offset(centerX, height * 0.6f),
                        strokeWidth = strokeWidth
                    )
                    // 점
                    drawCircle(
                        color = Color.White,
                        radius = strokeWidth / 2 + 1.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(centerX, height * 0.75f)
                    )
                }
            }

            // Title (v0: text-4xl font-black)
            Text(
                text = "비상 상황!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Subtitle (v0: text-xl text-destructive-foreground/90 mb-8)
            Text(
                text = "${countdown}초 내로 충전기를 연결하십시오!",
                fontSize = 20.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Large Countdown with glow pulse
            Box(contentAlignment = Alignment.Center) {
                // 글로우 효과 (뒤에 흐린 텍스트)
                Text(
                    text = "$countdown",
                    fontSize = 100.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = glowAlpha),
                    modifier = Modifier.graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                    }
                )
                // 메인 카운트다운 숫자
                Text(
                    text = "$countdown",
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pulseScale * 0.95f
                        scaleY = pulseScale * 0.95f
                    }
                )
            }

            // Battery Status (v0: mt-8 text-sm text-destructive-foreground/70 font-mono)
            Text(
                text = "BATTERY CRITICAL: ${batteryLevel}%",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 32.dp)
            )

            // 보급 요청 버튼 (한 번만 사용 가능)
            if (!supplyUsed) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📦 긴급 보급 요청",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "광고 시청 시 1분 추가",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestSupply,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = CrisisRed
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "보급 요청하기",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== HUD HEADER =====
// v0: border-b border-border/50 bg-card/50 backdrop-blur-sm sticky top-0 z-40
@Composable
private fun HudHeader(
    batteryLevel: Int,
    isCharging: Boolean,
    onlineCount: Int,
    onlineCountLoaded: Boolean,
    showRanking: Boolean,
    onToggleRanking: () -> Unit,
    currentRank: EliteRank,
    sessionDuration: Long,
    currentUserNickname: String,
    onShowNicknameDialog: () -> Unit,
    onLeaveChat: () -> Unit,
    recentJoinCount: Int = 0,
    recentLeaveCount: Int = 0,
    showJoinLeaveIndicator: Boolean = false,
    // 관리자 모드
    isAdminMode: Boolean = false,
    showAdmin: Boolean = false,
    onToggleAdmin: () -> Unit = {},
    pendingReportsCount: Int = 0
) {
    val isFullCharge = batteryLevel == 100
    val batteryColor = if (isFullCharge) EliteGreen else CrisisRed

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBlack.copy(alpha = 0.5f)  // v0: bg-card/50
    ) {
        Column {
            // ===== Top Status Bar =====
            // v0: flex items-center justify-between px-3 py-2 border-b border-border/30
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leave Button + Battery + Percentage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 나가기 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onLeaveChat() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "← 나가기",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForegroundMuted
                        )
                    }

                    // 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(BorderMuted.copy(alpha = 0.5f))
                    )

                    BatteryIconSmall(
                        level = batteryLevel,
                        charging = isCharging
                    )
                    Text(
                        text = "${batteryLevel}%",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = batteryColor
                    )
                }

                // Admin + Ranking + Online Count (v0: flex items-center gap-3)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Admin Button (관리자 모드일 때만 표시)
                    if (isAdminMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (showAdmin) CrisisRed.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable { onToggleAdmin() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 신고 배지 (대기 중인 신고가 있을 때)
                                Box {
                                    Text(text = "🛡️", fontSize = 12.sp)
                                    if (pendingReportsCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(CrisisRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (pendingReportsCount > 9) "9+" else "$pendingReportsCount",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "관리",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (showAdmin) CrisisRed else ForegroundMuted
                                )
                                Text(
                                    text = if (showAdmin) "▲" else "▼",
                                    fontSize = 10.sp,
                                    color = if (showAdmin) CrisisRed else ForegroundMuted
                                )
                            }
                        }
                    }

                    // Ranking Button (v0: button with px-2 py-1 rounded-md)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (showRanking) EliteGreen.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .clickable { onToggleRanking() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🏆", fontSize = 12.sp)
                            Text(
                                text = "랭킹",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (showRanking) EliteGreen else ForegroundMuted
                            )
                            Text(
                                text = if (showRanking) "▲" else "▼",
                                fontSize = 10.sp,
                                color = if (showRanking) EliteGreen else ForegroundMuted
                            )
                        }
                    }

                    // Online Count (v0: flex items-center gap-1)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "👥", fontSize = 12.sp)
                        Text(
                            text = if (onlineCountLoaded) "$onlineCount" else "...",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForegroundMuted
                        )
                    }
                }
            }

            // Divider (v0: border-b border-border/30)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.3f))
            )

            // ===== Rank & Stats Bar =====
            // v0: px-3 py-2
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // v0: flex items-center justify-between mb-2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Info (v0: flex items-center gap-2)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onShowNicknameDialog() }
                            .padding(4.dp)
                    ) {
                        // 실제 계급장 UI
                        RankInsignia(rank = currentRank, size = 24.dp)
                        Column {
                            // 닉네임 (터치하면 수정 가능)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentUserNickname,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ForegroundWhite
                                )
                                Text(
                                    text = "✎",
                                    fontSize = 10.sp,
                                    color = ForegroundMuted
                                )
                            }
                            // 계급명 (한글)
                            Text(
                                text = currentRank.koreanName,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = ForegroundMuted
                            )
                        }
                    }

                    // 다음 진급까지 남은 시간
                    val remainingTime = getRemainingTimeToNextRank(sessionDuration, currentRank)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "⏱️", fontSize = 12.sp)
                        Text(
                            text = if (remainingTime != null) "다음 진급까지 $remainingTime" else "최고 계급 달성!",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = EliteGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ===== Rank Progress Bar (실시간 애니메이션) =====
                val progress = getRankProgress(sessionDuration, currentRank)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                    label = "rank_progress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MutedBlack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(50))
                            .background(EliteGreen)
                    )
                }

                // 실시간 합류/퇴장 인디케이터
                JoinLeaveIndicator(
                    joinCount = recentJoinCount,
                    leaveCount = recentLeaveCount,
                    isVisible = showJoinLeaveIndicator,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

// ===== RANKING BOARD PANEL =====
// v0: bg-card/80 border border-border/50 rounded-lg overflow-hidden
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun RankingBoardPanel(
    liveRanking: List<EliteUser>,
    allTimeRanking: List<AllTimeRecord>,
    currentUserId: String
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBlack.copy(alpha = 0.8f))
            .border(1.dp, BorderMuted.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
    ) {
        // ===== Tab Header =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            RankingTabButton(
                text = "실시간 랭킹",
                icon = "⏱️",
                isActive = pagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            RankingTabButton(
                text = "역대 랭킹",
                icon = "🏆",
                isActive = pagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderMuted.copy(alpha = 0.5f))
        )

        // ===== Swipe Indicator =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Chevron
            Text(
                text = "‹",
                fontSize = 16.sp,
                color = if (pagerState.currentPage == 0) ForegroundMuted.copy(alpha = 0.2f)
                else EliteGreen.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == 0) EliteGreen else ForegroundMuted)
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == 1) EliteGreen else ForegroundMuted)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Chevron
            Text(
                text = "›",
                fontSize = 16.sp,
                color = if (pagerState.currentPage == 1) ForegroundMuted.copy(alpha = 0.2f)
                else EliteGreen.copy(alpha = 0.6f)
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderMuted.copy(alpha = 0.3f))
        )

        // ===== HorizontalPager for Ranking Lists =====
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) { page ->
            // 실시간: 모든 접속자, 역대: 50위까지
            val items = if (page == 0) {
                liveRanking.sortedByDescending { it.sessionDuration }
            } else {
                allTimeRanking.sortedByDescending { it.durationMillis }.take(50)
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "랭킹 데이터가 없습니다",
                        fontSize = 14.sp,
                        color = ForegroundMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        val (nickname, duration, isMe) = when (item) {
                            is EliteUser -> Triple(
                                item.nickname,
                                item.sessionDuration,
                                item.userId == currentUserId
                            )
                            is AllTimeRecord -> Triple(
                                item.nickname,
                                item.durationMillis,
                                item.oderId == currentUserId
                            )
                            else -> Triple("", 0L, false)
                        }
                        val rank = EliteRank.fromDuration(duration)

                        RankingListItem(
                            position = index,
                            nickname = nickname,
                            rank = rank,
                            duration = formatDuration(duration),
                            isMe = isMe,
                            showBestRecord = page == 1  // 역대 랭킹일 때만 "최고 기록" 표시
                        )
                    }
                }
            }
        }

        // ===== Footer Hint =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MutedBlack.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (pagerState.currentPage == 0) "현재 접속 중인 전우들의 실시간 순위"
                else "완충 전우회 역대 명예의 전당",
                fontSize = 12.sp,
                color = ForegroundMuted
            )
        }
    }
}

// v0 Tab Button:
// Active: bg-primary/10 text-primary border-b-2 border-primary
// Inactive: text-muted-foreground
@Composable
private fun RankingTabButton(
    text: String,
    icon: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isActive) EliteGreen.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) EliteGreen else ForegroundMuted
            )
        }

        // Bottom border for active tab (v0: border-b-2 border-primary)
        if (isActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(EliteGreen)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// v0 Ranking Item:
// flex items-center gap-3 px-2 py-2 rounded-md
// isMe: bg-primary/15 border border-primary/30
// top3: bg-muted/30
@Composable
private fun RankingListItem(
    position: Int,
    nickname: String,
    rank: EliteRank,
    duration: String,
    isMe: Boolean,
    showBestRecord: Boolean = false
) {
    val medal = when (position) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "${position + 1}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isMe -> EliteGreen.copy(alpha = 0.15f)
                    position < 3 -> MutedBlack.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isMe) Modifier.border(1.dp, EliteGreen.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                else Modifier
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Position (v0: w-6)
        Box(
            modifier = Modifier.width(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = medal,
                fontSize = if (position < 3) 18.sp else 14.sp
            )
        }

        // 실제 계급장 UI
        RankInsignia(rank = rank, size = 20.dp)

        // User Info (v0: flex-1 min-w-0)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = nickname,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) EliteGreen else ForegroundWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isMe) {
                    Text(
                        text = "(나)",
                        fontSize = 11.sp,
                        color = EliteGreen.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = rank.koreanName,
                fontSize = 12.sp,
                color = ForegroundMuted
            )
        }

        // Survival Time (v0: text-right)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = duration,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isMe) EliteGreen else ForegroundWhite
            )
            // v0: 역대 랭킹일 때만 "최고 기록" 표시
            if (showBestRecord) {
                Text(
                    text = "최고 기록",
                    fontSize = 10.sp,
                    color = ForegroundMuted
                )
            }
        }
    }
}

// ===== CHAT MESSAGES =====
// 시스템 메시지 - 어두운 pill 형태, 초록색 번개 아이콘, 흰색 텍스트
@Composable
private fun SystemMessage(message: ChatMessage) {
    // 배신 메시지는 특별한 경고 디자인 적용
    val isBetrayalMessage = message.message.contains("배신했습니다")

    if (isBetrayalMessage) {
        BetrayalAlertMessage(message = message)
    } else {
        // 일반 시스템 메시지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 340.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF2A2A2A))  // 어두운 회색 배경
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 초록색 번개 아이콘 (Canvas로 그리기)
                androidx.compose.foundation.Canvas(
                    modifier = Modifier.size(14.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        // 번개 모양
                        moveTo(w * 0.55f, 0f)
                        lineTo(w * 0.2f, h * 0.5f)
                        lineTo(w * 0.45f, h * 0.5f)
                        lineTo(w * 0.35f, h)
                        lineTo(w * 0.8f, h * 0.4f)
                        lineTo(w * 0.55f, h * 0.4f)
                        lineTo(w * 0.7f, 0f)
                        close()
                    }
                    drawPath(path, color = EliteGreen)
                }
                // 흰색 텍스트
                Text(
                    text = message.message,
                    fontSize = 13.sp,
                    color = ForegroundWhite,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ===== BETRAYAL ALERT MESSAGE =====
// 배신(추방) 메시지 - 긴급 알림 스타일, 빨간색 경고 디자인
@Composable
private fun BetrayalAlertMessage(message: ChatMessage) {
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
    val timeString = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF3D1515))  // 어두운 빨간색 배경
            .border(1.dp, CrisisRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 헤더: ⚠ 긴급 알림 + 시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚠",
                fontSize = 14.sp,
                color = WarningAmber
            )
            Text(
                text = "긴급 알림",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ForegroundWhite
            )
            Text(
                text = timeString,
                fontSize = 12.sp,
                color = ForegroundMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 본문 메시지 (빨간색)
        Text(
            text = message.message,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = CrisisRed,
            lineHeight = 20.sp
        )
    }
}

// 관리자 공지 메시지 - 긴급 알림 스타일 (초록색)
@Composable
private fun AnnouncementMessage(message: ChatMessage) {
    val noticeGreen = Color(0xFF00FF66)
    val timeFormat = SimpleDateFormat("a hh:mm", Locale.KOREA)
    val timeString = timeFormat.format(Date(message.timestamp))

    // "[공지] " 접두사 제거
    val displayMessage = message.message.removePrefix("[공지] ").removePrefix("[공지]")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF153D1F))  // 어두운 초록색 배경
            .border(1.dp, noticeGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // 헤더: 📢 공지사항 + 시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📢",
                fontSize = 14.sp
            )
            Text(
                text = "공지사항",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ForegroundWhite
            )
            Text(
                text = timeString,
                fontSize = 12.sp,
                color = ForegroundMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 본문 메시지 (초록색)
        Text(
            text = displayMessage,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = noticeGreen,
            lineHeight = 20.sp
        )
    }
}

// v0 User Message:
// - Container: flex flex-col gap-1, items-end for mine
// - Bubble: max-w-[85%] p-3 rounded-2xl shadow-sm
// - Mine: mr-1 ml-8 rounded-tr-sm bg-primary
// - Others: ml-6 mr-8 rounded-tl-sm bg-card border border-primary/20
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserMessage(
    message: ChatMessage,
    isMine: Boolean,
    currentUserId: String = "",
    onToggleReaction: (String, String) -> Unit = { _, _ -> },
    isBlocked: Boolean = false,
    onBlockUser: () -> Unit = {},
    onUnblockUser: () -> Unit = {}
) {
    val rank = try {
        EliteRank.valueOf(message.rank)
    } catch (e: Exception) {
        EliteRank.TRAINEE
    }

    val bubbleShape = RoundedCornerShape(
        topStart = if (isMine) 16.dp else 4.dp,
        topEnd = if (isMine) 4.dp else 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    // 리액션 피커 표시 여부
    var showReactionPicker by remember { mutableStateOf(false) }

    // 차단 해제 확인 다이얼로그 표시 여부
    var showUnblockDialog by remember { mutableStateOf(false) }

    // 커스텀 리액션 목록
    val customReactions = listOf("악!", "확인!", "삼돠!", "슴돠?")

    // 차단 해제 확인 다이얼로그
    if (showUnblockDialog) {
        UnblockConfirmDialog(
            nickname = message.nickname,
            onConfirm = {
                onUnblockUser()
                showUnblockDialog = false
            },
            onDismiss = { showUnblockDialog = false }
        )
    }

    // 블라인드 처리된 메시지인 경우
    if (isBlocked && !isMine) {
        BlindedMessage(
            nickname = message.nickname,
            timestamp = message.timestamp,
            onLongPress = { showUnblockDialog = true }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMine) 48.dp else 4.dp,
                end = if (isMine) 4.dp else 48.dp
            ),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // Author Info
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isMine) {
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForegroundMuted
                )
                Text(
                    text = "나",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForegroundWhite
                )
                RankInsignia(rank = rank, size = 16.dp)
            } else {
                RankInsignia(rank = rank, size = 16.dp)
                Text(
                    text = message.nickname,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EliteGreen
                )
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForegroundMuted
                )
            }
        }

        // Message Bubble + Reactions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 내 메시지: 리액션이 왼쪽에
            if (isMine && message.reactions.isNotEmpty()) {
                ReactionDisplay(
                    reactions = message.reactions,
                    currentUserId = currentUserId,
                    onToggleReaction = { emoji -> onToggleReaction(message.id, emoji) },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // Message Bubble with long press
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(max = 240.dp)
                    .clip(bubbleShape)
                    .background(if (isMine) EliteGreen else CardBlack)
                    .then(
                        if (!isMine) Modifier.border(1.dp, EliteGreen.copy(alpha = 0.2f), bubbleShape)
                        else Modifier
                    )
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showReactionPicker = true }
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.message,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = if (isMine) BackgroundBlack else ForegroundWhite
                )
            }

            // 타인 메시지: 리액션이 오른쪽에
            if (!isMine && message.reactions.isNotEmpty()) {
                ReactionDisplay(
                    reactions = message.reactions,
                    currentUserId = currentUserId,
                    onToggleReaction = { emoji -> onToggleReaction(message.id, emoji) },
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        // 리액션 피커 (롱프레스 시 표시)
        // 각 리액션별 색상 정의
        val reactionColors = mapOf(
            "악!" to CrisisRed,
            "확인!" to EliteGreen,
            "삼돠!" to Color(0xFF4D94FF),  // 파랑
            "슴돠?" to WarningAmber
        )

        AnimatedVisibility(
            visible = showReactionPicker,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f)
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceBlack)
                    .border(1.dp, BorderMuted, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                customReactions.forEach { emoji ->
                    val isSelected = message.reactions[emoji]?.contains(currentUserId) == true
                    val emojiColor = reactionColors[emoji] ?: ForegroundWhite
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) emojiColor.copy(alpha = 0.3f)
                                else Color.Transparent
                            )
                            .clickable {
                                onToggleReaction(message.id, emoji)
                                showReactionPicker = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Serif,  // 궁서체 스타일
                            fontWeight = FontWeight.Bold,
                            color = emojiColor
                        )
                    }
                }

                // 타인 메시지일 경우 신고 버튼 추가
                if (!isMine) {
                    // 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(BorderMuted.copy(alpha = 0.5f))
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrisisRed.copy(alpha = 0.15f))
                            .clickable {
                                onBlockUser()
                                showReactionPicker = false
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🚨 신고",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                    }
                }

                // 닫기 버튼
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showReactionPicker = false }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 12.sp,
                        color = ForegroundMuted
                    )
                }
            }
        }
    }
}

// 리액션 표시 컴포넌트
@Composable
private fun ReactionDisplay(
    reactions: Map<String, List<String>>,
    currentUserId: String,
    onToggleReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 각 리액션별 색상 정의
    val reactionColors = mapOf(
        "악!" to CrisisRed,
        "확인!" to EliteGreen,
        "삼돠!" to Color(0xFF4D94FF),
        "슴돠?" to WarningAmber
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        reactions.forEach { (emoji, userIds) ->
            if (userIds.isNotEmpty()) {
                val isSelected = userIds.contains(currentUserId)
                val emojiColor = reactionColors[emoji] ?: ForegroundMuted
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) emojiColor.copy(alpha = 0.25f)
                            else MutedBlack
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) emojiColor.copy(alpha = 0.5f) else BorderMuted,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onToggleReaction(emoji) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = emojiColor
                        )
                        if (userIds.size > 1) {
                            Text(
                                text = "${userIds.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = emojiColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ===== MESSAGE INPUT =====
// v0: border-t border-border/50 bg-card/50 backdrop-blur-sm p-3
@Composable
private fun MessageInputSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasText = value.isNotBlank()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardBlack.copy(alpha = 0.5f)  // v0: bg-card/50
    ) {
        Column {
            // Divider (v0: border-t border-border/50)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.5f))
            )

            // Input Row (v0: form flex gap-2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),  // v0: p-3
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)  // v0: gap-2
            ) {
                // v0: Input flex-1 bg-input border-border/50
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "메시지를 입력하십시오...",
                            color = ForegroundMuted,
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EliteGreen,
                        unfocusedBorderColor = BorderMuted.copy(alpha = 0.5f),
                        cursorColor = EliteGreen,
                        focusedTextColor = ForegroundWhite,
                        unfocusedTextColor = ForegroundWhite,
                        focusedContainerColor = MutedBlack,
                        unfocusedContainerColor = MutedBlack
                    ),
                    shape = RoundedCornerShape(8.dp),  // v0 uses default input shape
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                // Send Button (v0: Button size="icon" bg-primary)
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EliteGreen)  // v0: bg-primary always
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "전송",
                        tint = BackgroundBlack,  // v0: text-primary-foreground
                        modifier = Modifier.size(16.dp)  // v0: w-4 h-4
                    )
                }
            }

            // Footer: "전우회 통신 활성화"
            // v0: flex items-center justify-center gap-2 mt-2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing dot (v0: w-2 h-2 rounded-full bg-primary animate-pulse)
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_pulse"
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EliteGreen.copy(alpha = alpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "전우회 통신 활성화",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ForegroundMuted
                )
            }
        }
    }
}

// ===== BATTERY ICON (Small) =====
@Composable
private fun BatteryIconSmall(
    level: Int,
    charging: Boolean
) {
    val isFullCharge = level == 100
    val color = if (isFullCharge) EliteGreen else CrisisRed

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(12.dp)
                .border(1.dp, color, RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level / 100f)
                    .padding(1.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(6.dp)
                .background(color, RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp))
        )
    }
}

private fun getRankEnglishName(rank: EliteRank): String {
    return when (rank) {
        EliteRank.TRAINEE -> "TRAINEE"
        EliteRank.PRIVATE_SECOND -> "PRIVATE"
        EliteRank.PRIVATE_FIRST -> "PFC"
        EliteRank.CORPORAL -> "CORPORAL"
        EliteRank.SERGEANT -> "SERGEANT"
        EliteRank.STAFF_SERGEANT -> "STAFF SGT"
        EliteRank.SERGEANT_FIRST -> "SFC"
        EliteRank.MASTER_SERGEANT -> "MSG"
        EliteRank.SERGEANT_MAJOR -> "SGM"
        EliteRank.SECOND_LIEUTENANT -> "2LT"
        EliteRank.FIRST_LIEUTENANT -> "1LT"
        EliteRank.CAPTAIN -> "CPT"
        EliteRank.MAJOR -> "MAJ"
        EliteRank.LIEUTENANT_COLONEL -> "LTC"
        EliteRank.COLONEL -> "COL"
        EliteRank.BRIGADIER_GENERAL -> "BG"
        EliteRank.MAJOR_GENERAL -> "MG"
        EliteRank.LIEUTENANT_GENERAL -> "LTG"
        EliteRank.GENERAL -> "GEN"
    }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val totalMinutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return when {
        totalMinutes < 1 -> "${seconds}초"
        totalMinutes < 60 -> "${totalMinutes}분 ${seconds}초"
        totalMinutes < 1440 -> {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins > 0) "${hours}시간 ${mins}분" else "${hours}시간"
        }
        else -> {
            val days = totalMinutes / 1440
            val hours = (totalMinutes % 1440) / 60
            if (hours > 0) "${days}일 ${hours}시간" else "${days}일"
        }
    }
}

private fun getRankProgress(sessionDuration: Long, currentRank: EliteRank): Float {
    // 초 단위로 계산하여 부드러운 프로그레스바
    val currentSeconds = sessionDuration / 1000
    val nextRank = EliteRank.entries.getOrNull(currentRank.ordinal + 1) ?: return 1f
    val currentRankSeconds = currentRank.minMinutes * 60
    val nextRankSeconds = nextRank.minMinutes * 60
    val progress = (currentSeconds - currentRankSeconds).toFloat() /
            (nextRankSeconds - currentRankSeconds).toFloat()
    return progress.coerceIn(0f, 1f)
}

private fun getRemainingTimeToNextRank(sessionDuration: Long, currentRank: EliteRank): String? {
    val nextRank = EliteRank.entries.getOrNull(currentRank.ordinal + 1) ?: return null
    val currentSeconds = sessionDuration / 1000
    val nextRankSeconds = nextRank.minMinutes * 60L
    val remainingTotalSeconds = nextRankSeconds - currentSeconds

    if (remainingTotalSeconds <= 0) return null

    val remainingMinutes = remainingTotalSeconds / 60
    val remainingSeconds = remainingTotalSeconds % 60

    return when {
        remainingMinutes == 0L -> "${remainingSeconds}초"
        remainingMinutes < 60 -> "${remainingMinutes}분 ${remainingSeconds}초"
        remainingMinutes < 1440 -> {
            val hours = remainingMinutes / 60
            val mins = remainingMinutes % 60
            "${hours}시간 ${mins}분"
        }
        else -> {
            val days = remainingMinutes / 1440
            val hours = (remainingMinutes % 1440) / 60
            "${days}일 ${hours}시간"
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun LeaveConfirmDialog(
    currentRank: EliteRank,
    sessionDuration: Long,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🚪", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "정말 나가시겠습니까?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForegroundWhite
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EliteGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "현재 계급: ${currentRank.koreanName}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EliteGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "생존 시간: ${formatDuration(sessionDuration)}",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ForegroundMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "나가면 현재 세션이 종료되고\n계급이 초기화됩니다.",
                    fontSize = 13.sp,
                    color = ForegroundDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "계속 생존하기", fontWeight = FontWeight.SemiBold, color = BackgroundBlack)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "나가기", color = CrisisRed.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun RankUpDialog(
    newRank: EliteRank,
    bannerAdContent: @Composable () -> Unit,
    onDismiss: () -> Unit
) {
    var countdown by remember { mutableIntStateOf(10) }

    // 10초 카운트다운 후 자동 닫기
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onDismiss()
    }

    // AlertDialog 대신 Dialog 사용하여 더 나은 레이아웃 제어
    androidx.compose.ui.window.Dialog(
        onDismissRequest = {},  // 바깥 터치로 닫히지 않음
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false  // 커스텀 너비 사용
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)  // 화면 너비의 90%
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = CardBlack
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // X 버튼 (우측 상단)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ForegroundMuted.copy(alpha = 0.2f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 14.sp,
                            color = ForegroundMuted
                        )
                    }
                }

                // 타이틀
                Text(text = "🎖️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "진급을 축하합니다!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = EliteGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 계급 정보
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EliteGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        RankInsignia(rank = newRank, size = 48.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = newRank.koreanName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = EliteGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = newRank.description,
                            fontSize = 13.sp,
                            color = ForegroundMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 배너 광고 영역 - 고정 크기로 명시적 지정
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)  // 배너 광고 표준 높이 + 여유
                        .clip(RoundedCornerShape(8.dp))
                        .background(MutedBlack),
                    contentAlignment = Alignment.Center
                ) {
                    bannerAdContent()
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 확인 버튼
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "계속 생존하기 ($countdown)",
                        fontWeight = FontWeight.SemiBold,
                        color = BackgroundBlack
                    )
                }
            }
        }
    }
}

// 계급별 색상 반환
private fun getRankColor(rank: EliteRank): Color {
    return when (rank) {
        EliteRank.GENERAL, EliteRank.LIEUTENANT_GENERAL,
        EliteRank.MAJOR_GENERAL, EliteRank.BRIGADIER_GENERAL -> Color(0xFFD97706)  // 장성: 금색
        EliteRank.COLONEL, EliteRank.LIEUTENANT_COLONEL, EliteRank.MAJOR -> Color(0xFF7C3AED)  // 영관: 보라색
        EliteRank.CAPTAIN, EliteRank.FIRST_LIEUTENANT, EliteRank.SECOND_LIEUTENANT -> Color(0xFF10B981)  // 위관: 녹색
        EliteRank.SERGEANT_MAJOR, EliteRank.MASTER_SERGEANT,
        EliteRank.SERGEANT_FIRST, EliteRank.STAFF_SERGEANT -> Color(0xFF3B82F6)  // 부사관: 파랑
        else -> Color(0xFFFEE500)  // 병사/훈련병: 노란색
    }
}

@Composable
private fun NicknameEditDialog(
    currentNickname: String,
    currentRank: EliteRank,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val rankColor = getRankColor(currentRank)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 현재 계급장 표시 (큰 사이즈)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BorderMuted.copy(alpha = 0.3f))
                        .border(2.dp, rankColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    RankInsignia(
                        rank = currentRank,
                        size = 48.dp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentRank.koreanName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = rankColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "닉네임 변경",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForegroundWhite
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = {
                        if (it.length <= 10) {
                            nickname = it
                            errorMessage = null
                        }
                    },
                    label = { Text("닉네임", color = ForegroundMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EliteGreen,
                        unfocusedBorderColor = BorderMuted,
                        focusedTextColor = ForegroundWhite,
                        unfocusedTextColor = ForegroundWhite,
                        cursorColor = EliteGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${nickname.length}/10",
                    fontSize = 12.sp,
                    color = ForegroundMuted,
                    modifier = Modifier.align(Alignment.End)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = CrisisRed,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = nickname.trim()
                    when {
                        trimmed.isEmpty() -> errorMessage = "닉네임을 입력해주세요"
                        trimmed.length < 2 -> errorMessage = "2글자 이상 입력해주세요"
                        else -> onConfirm(trimmed)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "변경하기", fontWeight = FontWeight.SemiBold, color = BackgroundBlack)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "취소", color = ForegroundMuted)
            }
        }
    )
}

// ===== BLINDED MESSAGE =====
// 신고된 유저의 메시지를 블라인드 처리하는 UI
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BlindedMessage(
    nickname: String,
    timestamp: Long,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 48.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Author Info (흐리게)
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🚫",
                fontSize = 14.sp
            )
            Text(
                text = nickname,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ForegroundMuted.copy(alpha = 0.5f)
            )
            Text(
                text = formatTime(timestamp),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = ForegroundMuted.copy(alpha = 0.3f)
            )
        }

        // 블라인드 메시지 박스
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 240.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(MutedBlack.copy(alpha = 0.6f))
                .border(1.dp, BorderMuted.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                .combinedClickable(
                    onClick = { },
                    onLongClick = onLongPress
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🚫",
                    fontSize = 14.sp
                )
                Column {
                    Text(
                        text = "신고된 사용자입니다",
                        fontSize = 13.sp,
                        color = ForegroundMuted.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "길게 눌러 차단 해제",
                        fontSize = 11.sp,
                        color = ForegroundMuted.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ===== UNBLOCK CONFIRM DIALOG =====
// 차단 해제 확인 다이얼로그
@Composable
private fun UnblockConfirmDialog(
    nickname: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🔓", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "차단 해제",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForegroundWhite
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = nickname,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "이 사용자의 차단을 해제하면\n메시지가 다시 표시됩니다.",
                    fontSize = 13.sp,
                    color = ForegroundDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "차단 해제하기", fontWeight = FontWeight.SemiBold, color = BackgroundBlack)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "취소", color = ForegroundMuted)
            }
        }
    )
}

// ===== ADMIN PANEL =====
// 관리자 전용 패널 - 신고/사용자/공지 관리
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AdminPanel(
    reports: List<Report>,
    onlineUsers: List<EliteUser>,
    currentUserId: String,
    onHandleReport: (String, String, Boolean) -> Unit,
    onDismissReport: (String) -> Unit,
    onKickUser: (String, String) -> Unit,
    onChangeUserRank: (String, String, EliteRank) -> Unit,
    onSendAdminNotice: (String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    // 대기 중인 신고 수
    val pendingReportsCount = reports.count { it.status == ReportStatus.PENDING.name }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CardBlack.copy(alpha = 0.8f))
            .border(1.dp, CrisisRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        // ===== Tab Header =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            AdminTabButton(
                text = "신고",
                icon = "🚨",
                badgeCount = pendingReportsCount,
                isActive = pagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            AdminTabButton(
                text = "사용자",
                icon = "👥",
                badgeCount = 0,
                isActive = pagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            AdminTabButton(
                text = "공지",
                icon = "📢",
                badgeCount = 0,
                isActive = pagerState.currentPage == 2,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(2)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CrisisRed.copy(alpha = 0.3f))
        )

        // ===== HorizontalPager for Admin Tabs =====
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) { page ->
            when (page) {
                0 -> ReportManagementContent(
                    reports = reports,
                    onHandleReport = onHandleReport,
                    onDismissReport = onDismissReport
                )
                1 -> UserManagementContent(
                    onlineUsers = onlineUsers,
                    currentUserId = currentUserId,
                    onKickUser = onKickUser,
                    onChangeUserRank = onChangeUserRank
                )
                2 -> NoticeContent(
                    onSendNotice = onSendAdminNotice
                )
            }
        }

        // ===== Footer =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrisisRed.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when (pagerState.currentPage) {
                    0 -> "신고 ${reports.size}건 | 대기 ${pendingReportsCount}건"
                    1 -> "접속 중인 사용자 ${onlineUsers.size}명"
                    else -> "공지사항 작성"
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = CrisisRed.copy(alpha = 0.7f)
            )
        }
    }
}

// ===== ADMIN TAB BUTTON =====
@Composable
private fun AdminTabButton(
    text: String,
    icon: String,
    badgeCount: Int = 0,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isActive) CrisisRed.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                Text(text = icon, fontSize = 14.sp)
                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-4).dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(CrisisRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 9) "9+" else "$badgeCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) CrisisRed else ForegroundMuted
            )
        }

        // Bottom border for active tab
        if (isActive) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(CrisisRed)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ===== REPORT MANAGEMENT CONTENT =====
@Composable
private fun ReportManagementContent(
    reports: List<Report>,
    onHandleReport: (String, String, Boolean) -> Unit,
    onDismissReport: (String) -> Unit
) {
    if (reports.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "✅", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "신고가 없습니다",
                    fontSize = 14.sp,
                    color = ForegroundMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports, key = { it.id }) { report ->
                ReportItem(
                    report = report,
                    onDelete = { onHandleReport(report.id, report.messageId, true) },
                    onDismiss = { onDismissReport(report.id) }
                )
            }
        }
    }
}

// ===== REPORT ITEM =====
@Composable
private fun ReportItem(
    report: Report,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val isPending = report.status == ReportStatus.PENDING.name
    val timeAgo = getTimeAgo(report.timestamp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPending) CrisisRed.copy(alpha = 0.1f) else MutedBlack)
            .border(
                1.dp,
                if (isPending) CrisisRed.copy(alpha = 0.3f) else BorderMuted.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🚨", fontSize = 14.sp)
                Text(
                    text = if (isPending) "대기 중" else report.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPending) CrisisRed else ForegroundMuted
                )
            }
            Text(
                text = timeAgo,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = ForegroundMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 신고자/피신고자 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "신고자",
                    fontSize = 10.sp,
                    color = ForegroundMuted
                )
                Text(
                    text = report.reporterNickname,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ForegroundWhite
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "대상",
                    fontSize = 10.sp,
                    color = ForegroundMuted
                )
                Text(
                    text = report.reportedNickname,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CrisisRed
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 신고 메시지 내용
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(MutedBlack)
                .padding(8.dp)
        ) {
            Text(
                text = "\"${report.messageContent.take(100)}${if (report.messageContent.length > 100) "..." else ""}\"",
                fontSize = 12.sp,
                color = ForegroundDim,
                lineHeight = 16.sp
            )
        }

        // 처리 버튼 (대기 중일 때만)
        if (isPending) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = CrisisRed),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text(
                        text = "삭제 처리",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ForegroundMuted
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted)
                ) {
                    Text(
                        text = "기각",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ===== USER MANAGEMENT CONTENT =====
@Composable
private fun UserManagementContent(
    onlineUsers: List<EliteUser>,
    currentUserId: String,
    onKickUser: (String, String) -> Unit,
    onChangeUserRank: (String, String, EliteRank) -> Unit
) {
    var showKickDialog by remember { mutableStateOf<EliteUser?>(null) }
    var showRankDialog by remember { mutableStateOf<EliteUser?>(null) }

    // 강퇴 확인 다이얼로그
    showKickDialog?.let { user ->
        KickConfirmDialog(
            nickname = user.nickname,
            onConfirm = {
                onKickUser(user.userId, user.nickname)
                showKickDialog = null
            },
            onDismiss = { showKickDialog = null }
        )
    }

    // 계급 변경 다이얼로그
    showRankDialog?.let { user ->
        RankChangeDialog(
            nickname = user.nickname,
            currentRank = EliteRank.fromDuration(user.sessionDuration),
            onConfirm = { newRank ->
                onChangeUserRank(user.userId, user.nickname, newRank)
                showRankDialog = null
            },
            onDismiss = { showRankDialog = null }
        )
    }

    // 본인 제외한 사용자 목록
    val otherUsers = onlineUsers.filter { it.userId != currentUserId }

    if (otherUsers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👤", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "관리할 사용자가 없습니다",
                    fontSize = 14.sp,
                    color = ForegroundMuted
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(otherUsers, key = { it.visibleId }) { user ->
                AdminUserItem(
                    user = user,
                    onKick = { showKickDialog = user },
                    onChangeRank = { showRankDialog = user }
                )
            }
        }
    }
}

// EliteUser에 visibleId 확장 프로퍼티 추가 (key 충돌 방지)
private val EliteUser.visibleId: String
    get() = "${userId}_${sessionStartTime}"

// ===== ADMIN USER ITEM =====
@Composable
private fun AdminUserItem(
    user: EliteUser,
    onKick: () -> Unit,
    onChangeRank: () -> Unit
) {
    val rank = EliteRank.fromDuration(user.sessionDuration)
    val duration = formatDuration(user.sessionDuration)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MutedBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 사용자 정보
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            RankInsignia(rank = rank, size = 20.dp)
            Column {
                Text(
                    text = user.nickname,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForegroundWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${rank.koreanName} • $duration",
                    fontSize = 11.sp,
                    color = ForegroundMuted
                )
            }
        }

        // 액션 버튼
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 강퇴 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CrisisRed.copy(alpha = 0.2f))
                    .clickable { onKick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "강퇴",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrisisRed
                )
            }

            // 계급 변경 버튼
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(WarningAmber.copy(alpha = 0.2f))
                    .clickable { onChangeRank() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "계급",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )
            }
        }
    }
}

// ===== NOTICE CONTENT =====
@Composable
private fun NoticeContent(
    onSendNotice: (String) -> Unit
) {
    var noticeText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    // 성공 메시지 자동 숨기기
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 안내 텍스트
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "📢", fontSize = 18.sp)
            Column {
                Text(
                    text = "공지사항 작성",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForegroundWhite
                )
                Text(
                    text = "모든 접속자에게 전송됩니다",
                    fontSize = 11.sp,
                    color = ForegroundMuted
                )
            }
        }

        // 입력 필드
        OutlinedTextField(
            value = noticeText,
            onValueChange = { noticeText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "공지 내용을 입력하세요...",
                    color = ForegroundMuted,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrisisRed,
                unfocusedBorderColor = BorderMuted.copy(alpha = 0.5f),
                cursorColor = CrisisRed,
                focusedTextColor = ForegroundWhite,
                unfocusedTextColor = ForegroundWhite,
                focusedContainerColor = MutedBlack,
                unfocusedContainerColor = MutedBlack
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // 전송 버튼
        Button(
            onClick = {
                if (noticeText.isNotBlank()) {
                    onSendNotice(noticeText.trim())
                    noticeText = ""
                    showSuccess = true
                }
            },
            enabled = noticeText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CrisisRed,
                disabledContainerColor = CrisisRed.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "공지 전송",
                fontWeight = FontWeight.Bold,
                color = if (noticeText.isNotBlank()) Color.White else ForegroundMuted
            )
        }

        // 성공 메시지
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(EliteGreen.copy(alpha = 0.2f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "✅", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "공지가 전송되었습니다",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = EliteGreen
                )
            }
        }
    }
}

// ===== KICK CONFIRM DIALOG =====
@Composable
private fun KickConfirmDialog(
    nickname: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🚫", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "강퇴 확인",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForegroundWhite
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrisisRed.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = nickname,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "이 사용자를 강제 퇴장시키겠습니까?\n퇴장 알림이 전체 채팅에 표시됩니다.",
                    fontSize = 13.sp,
                    color = ForegroundDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CrisisRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "강퇴하기", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "취소", color = ForegroundMuted)
            }
        }
    )
}

// ===== RANK CHANGE DIALOG =====
@Composable
private fun RankChangeDialog(
    nickname: String,
    currentRank: EliteRank,
    onConfirm: (EliteRank) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRank by remember { mutableStateOf(currentRank) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🎖️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "계급 변경",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ForegroundWhite
                )
                Text(
                    text = nickname,
                    fontSize = 14.sp,
                    color = WarningAmber
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Text(
                    text = "새 계급 선택",
                    fontSize = 12.sp,
                    color = ForegroundMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(EliteRank.entries.toList()) { rank ->
                        val isSelected = rank == selectedRank
                        val isCurrent = rank == currentRank
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> WarningAmber.copy(alpha = 0.2f)
                                        isCurrent -> EliteGreen.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { selectedRank = rank }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RankInsignia(rank = rank, size = 20.dp)
                            Text(
                                text = rank.koreanName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> WarningAmber
                                    isCurrent -> EliteGreen
                                    else -> ForegroundWhite
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (isCurrent) {
                                Text(
                                    text = "현재",
                                    fontSize = 10.sp,
                                    color = EliteGreen
                                )
                            }
                            if (isSelected && !isCurrent) {
                                Text(
                                    text = "✓",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRank) },
                enabled = selectedRank != currentRank,
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarningAmber,
                    disabledContainerColor = WarningAmber.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "계급 변경",
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedRank != currentRank) BackgroundBlack else ForegroundMuted
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "취소", color = ForegroundMuted)
            }
        }
    )
}

// ===== UTILITY: TIME AGO =====
private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "방금 전"
        minutes < 60 -> "${minutes}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> {
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
