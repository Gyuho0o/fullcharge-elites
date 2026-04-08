package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    var showRanking by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showNicknameDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
                onLeaveChat = { showLeaveConfirmDialog = true }
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
                        message.isSystemMessage -> SystemMessage(message = message)
                        else -> UserMessage(
                            message = message,
                            isMine = message.userId == currentUserId
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
    onLeaveChat: () -> Unit
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

                // Ranking + Online Count (v0: flex items-center gap-3)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

// v0: Announcement - mx-2 my-3 p-3 rounded-lg bg-destructive/15 border-l-4 border-destructive
@Composable
private fun AnnouncementMessage(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(8.dp))
            .background(CrisisRed.copy(alpha = 0.15f))
    ) {
        // Left border (v0: border-l-4)
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(CrisisRed)
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(text = "⚠️", fontSize = 16.sp)
                Text(
                    text = "긴급 알림",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = CrisisRed,
                    letterSpacing = 2.sp
                )
                Text(
                    text = formatTime(message.timestamp),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CrisisRed.copy(alpha = 0.6f)
                )
            }
            Text(
                text = message.message,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = CrisisRed,
                lineHeight = 22.sp,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

// v0 User Message:
// - Container: flex flex-col gap-1, items-end for mine
// - Bubble: max-w-[85%] p-3 rounded-2xl shadow-sm
// - Mine: mr-1 ml-8 rounded-tr-sm bg-primary
// - Others: ml-6 mr-8 rounded-tl-sm bg-card border border-primary/20
@Composable
private fun UserMessage(
    message: ChatMessage,
    isMine: Boolean
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMine) 48.dp else 4.dp,   // 타인 메시지: 왼쪽 4dp (내 메시지 오른쪽과 동일)
                end = if (isMine) 4.dp else 48.dp      // 내 메시지: 오른쪽 4dp
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

        // Message Bubble - 내용에 맞게 크기 조절, 최대 너비 제한
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 260.dp)  // 약 85% of typical screen
                .clip(bubbleShape)
                .background(if (isMine) EliteGreen else CardBlack)
                .then(
                    if (!isMine) Modifier.border(1.dp, EliteGreen.copy(alpha = 0.2f), bubbleShape)
                    else Modifier
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

@Composable
private fun NicknameEditDialog(
    currentNickname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nickname by remember { mutableStateOf(currentNickname) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "✎", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
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
