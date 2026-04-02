package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.ChatMessage
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.data.EliteUser
import com.elites.fullcharge.ui.components.*
import com.elites.fullcharge.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    batteryState: BatteryState,
    messages: List<ChatMessage>,
    onlineUsers: List<EliteUser>,
    currentUserId: String,
    currentUserNickname: String,
    sessionDuration: Long,
    onSendMessage: (String) -> Unit,
    isInDanger: Boolean = false,
    dangerCountdown: Int = 0,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showLeaderboard by remember { mutableStateOf(false) }
    var showRankUpCelebration by remember { mutableStateOf(false) }
    var newRank by remember { mutableStateOf(EliteRank.NEWBIE) }
    var messageSentTrigger by remember { mutableIntStateOf(0) }

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

    // 새 메시지가 오면 자동 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // 배경 파티클 효과 (더 많이)
        DenseBackgroundParticles()

        // 배경 번개 (간헐적)
        BackgroundLightning(enabled = true)

        // 메시지 전송 시 번개 효과
        MessageSendLightning(trigger = messageSentTrigger)

        // 위험 경고 오버레이
        if (isInDanger) {
            DangerWarningOverlay(countdown = dangerCountdown)
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 위험 상태일 때 상단 경고 바
            AnimatedVisibility(
                visible = isInDanger,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DangerWarningBar(countdown = dangerCountdown)
            }

            // 상단 바
            ChatTopBar(
                batteryLevel = batteryState.level,
                userCount = onlineUsers.size,
                rank = currentRank,
                nickname = currentUserNickname,
                onToggleLeaderboard = { showLeaderboard = !showLeaderboard }
            )

            // 등급까지 남은 시간 (위험 모드일 땐 숨김)
            if (!isInDanger) {
                진급카운트다운(
                    현재시간 = sessionDuration,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 리더보드 (토글)
            AnimatedVisibility(
                visible = showLeaderboard,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                실시간리더보드(
                    사용자들 = onlineUsers,
                    현재사용자ID = currentUserId,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 채팅 메시지 목록
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    if (message.isSystemMessage) {
                        SystemMessageItem(message = message)
                    } else {
                        ChatMessageItem(
                            message = message,
                            isOwnMessage = message.userId == currentUserId
                        )
                    }
                }
            }

            // 메시지 입력창
            MessageInputBar(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                        messageSentTrigger++
                    }
                }
            )
        }

        // 등급 상승 축하
        RankUpCelebration(
            newRank = newRank,
            visible = showRankUpCelebration,
            onDismiss = { showRankUpCelebration = false }
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
    onToggleLeaderboard: () -> Unit
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

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onToggleLeaderboard) {
                        Text(
                            text = "랭킹",
                            fontSize = 14.sp,
                            color = TossBlue
                        )
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundGray)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            }
        }
    }
}

@Composable
private fun RankBadge(rank: EliteRank) {
    val bgColor = when (rank) {
        EliteRank.NEWBIE -> TextTertiary
        EliteRank.PRIVATE -> TossBlueLight.copy(alpha = 0.7f)
        EliteRank.SERGEANT -> TossBlueDark
        EliteRank.GOD -> TossBlue
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (rank == EliteRank.GOD) {
                    Brush.horizontalGradient(
                        colors = listOf(TossBlueDark, TossBlue, TossBlueLight)
                    )
                } else {
                    Brush.horizontalGradient(listOf(bgColor, bgColor))
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = rank.koreanName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    val messageRank = try {
        EliteRank.valueOf(message.rank)
    } catch (e: Exception) {
        EliteRank.NEWBIE
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

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
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                if (!isOwnMessage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        RankBadge(rank = messageRank)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.nickname,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // 등급별 채팅 버블 색상
                val bubbleColor = if (isOwnMessage) {
                    TossBlue
                } else {
                    when (messageRank) {
                        EliteRank.GOD -> Color(0xFF1A365D)        // 전설: 진한 네이비
                        EliteRank.SERGEANT -> Color(0xFF2D4A6F)   // 터줏대감: 네이비
                        EliteRank.PRIVATE -> Color(0xFF3D5A80)    // 정회원: 블루그레이
                        else -> ChatBubbleOther                    // 뉴비: 기본
                    }
                }
                val textColor = if (isOwnMessage || messageRank.ordinal >= EliteRank.PRIVATE.ordinal) {
                    Color.White
                } else {
                    TextPrimary
                }

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                            )
                        )
                        .background(bubbleColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = message.message,
                        fontSize = 15.sp,
                        color = textColor
                    )
                }

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
private fun SystemMessageItem(
    message: ChatMessage
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(StatusRed.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.message,
                    fontSize = 13.sp,
                    color = StatusRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BackgroundWhite.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "메시지를 입력하세요",
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
                maxLines = 3,
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

@Composable
private fun RankUpCelebration(
    newRank: EliteRank,
    visible: Boolean,
    onDismiss: () -> Unit
) {
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(40.dp)
            ) {
                Text(
                    text = "레벨 업!",
                    fontSize = 16.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(TossBlueDark, TossBlue, TossBlueLight)
                            )
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = newRank.koreanName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = newRank.description,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        LaunchedEffect(visible) {
            if (visible) {
                delay(2500)
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
