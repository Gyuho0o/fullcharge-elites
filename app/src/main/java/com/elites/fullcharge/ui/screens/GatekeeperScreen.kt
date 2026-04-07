package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.components.BackgroundLightning
import com.elites.fullcharge.ui.components.ChargingProgressBar
import com.elites.fullcharge.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GatekeeperScreen(
    batteryState: BatteryState,
    onlineUserCount: Int,
    onEnterPortal: () -> Unit,
    restorableSessionDuration: Long? = null,
    onRestoreWithAd: (Long) -> Unit = {},
    onDismissRestore: () -> Unit = {},
    // 관리자 모드
    isAdminMode: Boolean = false,
    showAdminLoginDialog: Boolean = false,
    onAdminTapDetected: () -> Unit = {},
    onAdminLogin: (String) -> Boolean = { false },
    onDismissAdminDialog: () -> Unit = {},
    onEnterAsAdmin: () -> Unit = {},
    onAdminLogout: () -> Unit = {},
    // 온보딩 다시보기
    onShowOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isElite = batteryState.isElite
    val isCharging = batteryState.isCharging
    val batteryLevel = batteryState.level

    // 비밀 탭 카운트
    var secretTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // 계급 복구 다이얼로그
    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onLogin = onAdminLogin,
            onDismiss = onDismissAdminDialog
        )
    }

    LaunchedEffect(restorableSessionDuration, isElite) {
        if (restorableSessionDuration != null && isElite) {
            showRestoreDialog = true
        }
    }

    if (showRestoreDialog && restorableSessionDuration != null) {
        RankRestoreDialog(
            previousDuration = restorableSessionDuration,
            onRestoreWithAd = {
                showRestoreDialog = false
                onRestoreWithAd(restorableSessionDuration)
            },
            onStartFresh = {
                showRestoreDialog = false
                onDismissRestore()
                onEnterPortal()
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // 배경 파티클 효과
        if (batteryLevel == 100 || isCharging) {
            FloatingParticles(
                particleCount = if (isElite) 50 else 25,
                speedMultiplier = if (isElite) 1.2f else 0.8f
            )
        }

        if (isElite) {
            BackgroundLightning(enabled = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 상단 헤더 영역
            HeaderSection(
                isAdminMode = isAdminMode,
                isElite = isElite,
                onlineUserCount = onlineUserCount,
                onSecretTap = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 2000) {
                        secretTapCount++
                        if (secretTapCount >= 5) {
                            secretTapCount = 0
                            if (isAdminMode) onAdminLogout() else onAdminTapDetected()
                        }
                    } else {
                        secretTapCount = 1
                    }
                    lastTapTime = currentTime
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 중앙 배터리 인디케이터
            ImprovedBatteryIndicator(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isElite = isElite
            )

            Spacer(modifier = Modifier.weight(1f))

            // 하단 CTA 영역
            BottomActionSection(
                isAdminMode = isAdminMode,
                isElite = isElite,
                isCharging = isCharging,
                batteryLevel = batteryLevel,
                onlineUserCount = onlineUserCount,
                onEnterPortal = onEnterPortal,
                onEnterAsAdmin = onEnterAsAdmin,
                onShowOnboarding = onShowOnboarding
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HeaderSection(
    isAdminMode: Boolean,
    isElite: Boolean,
    onlineUserCount: Int,
    onSecretTap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSecretTap() }
    ) {
        // 타이틀
        Text(
            text = if (isAdminMode) "완충 전우회" else "완충 전우회",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAdminMode) StatusRed else TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 접속자 수 칩
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isElite) TossBlue.copy(alpha = 0.1f) else BackgroundGray
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 온라인 표시 점
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (isAdminMode) StatusRed else StatusGreen,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                AnimatedContent(
                    targetState = onlineUserCount,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "userCount"
                ) { count ->
                    Text(
                        text = if (isAdminMode) "관리자 모드" else "${count}명 접속 중",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isAdminMode) StatusRed else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ImprovedBatteryIndicator(
    batteryLevel: Int,
    isCharging: Boolean,
    isElite: Boolean
) {
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel / 100f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "batteryLevel"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // 100%일 때 빠른 펄스 효과
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isElite) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isElite) 450 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isElite) 450 else 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val batteryColor by animateColorAsState(
        targetValue = when {
            isElite -> TossBlue
            batteryLevel >= 80 -> TossBlueDark
            batteryLevel >= 50 -> StatusGreen
            batteryLevel >= 20 -> StatusYellow
            else -> StatusRed
        },
        animationSpec = tween(500),
        label = "batteryColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(pulseScale)
    ) {
        // 메인 원형 인디케이터
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 16f
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // 배경 원
            drawCircle(
                color = DividerGray,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 충전 중일 때 펄스 링
            if (isCharging && !isElite) {
                drawCircle(
                    color = batteryColor.copy(alpha = glowAlpha * 0.3f),
                    radius = radius + 12f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }

            // 100%일 때 외부 글로우 효과
            if (isElite) {
                // 외부 글로우 링 1 (가장 바깥, 연한)
                drawCircle(
                    color = TossBlue.copy(alpha = glowAlpha * 0.15f),
                    radius = radius + 24f,
                    center = center,
                    style = Stroke(width = 8f)
                )
                // 외부 글로우 링 2 (중간)
                drawCircle(
                    color = TossBlue.copy(alpha = glowAlpha * 0.25f),
                    radius = radius + 14f,
                    center = center,
                    style = Stroke(width = 4f)
                )
            }

            // 배터리 레벨 원호
            if (isElite) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            TossBlueDark,
                            TossBlue,
                            TossBlueLight,
                            TossBlue,
                            TossBlueDark
                        ),
                        center = center
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                )
            } else {
                drawArc(
                    color = batteryColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedLevel,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokeWidth,
                        size.height - strokeWidth
                    )
                )
            }
        }

        // 중앙 텍스트
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$batteryLevel%",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = if (isElite) TossBlue else TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = when {
                    isElite -> "입장 가능"
                    isCharging -> "충전 중"
                    else -> "충전 필요"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isElite) TossBlue else TextSecondary
            )
        }
    }
}

@Composable
private fun BottomActionSection(
    isAdminMode: Boolean,
    isElite: Boolean,
    isCharging: Boolean,
    batteryLevel: Int,
    onlineUserCount: Int,
    onEnterPortal: () -> Unit,
    onEnterAsAdmin: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        when {
            isAdminMode -> AdminEntryContent(
                onlineUserCount = onlineUserCount,
                onEnterAsAdmin = onEnterAsAdmin
            )
            isElite -> EliteWelcomeContent(
                onlineUserCount = onlineUserCount,
                onEnterPortal = onEnterPortal
            )
            else -> UnworthyContent(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                onlineUserCount = onlineUserCount
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 이용 방법 보기 링크
        TextButton(
            onClick = onShowOnboarding,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "이용 방법 보기",
                fontSize = 13.sp,
                color = TextTertiary
            )
        }
    }
}

@Composable
private fun EliteWelcomeContent(
    onlineUserCount: Int,
    onEnterPortal: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "buttonPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 접속자 수 안내
        AnimatedContent(
            targetState = onlineUserCount,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "userCount"
        ) { count ->
            Text(
                text = if (count > 0) "현재 ${count}명의 전우회원이 함께하고 있어요"
                       else "첫 번째 전우회원이 되어보세요",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 경고 카드
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StatusRed.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "99%가 되면 10초 카운트다운이 시작돼요",
                fontSize = 13.sp,
                color = StatusRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 입장 버튼
        Button(
            onClick = onEnterPortal,
            modifier = Modifier
                .scale(scale)
                .height(56.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = TossBlue),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Text(
                text = "입장하기",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun UnworthyContent(
    batteryLevel: Int,
    isCharging: Boolean,
    onlineUserCount: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // 접속자 수 안내
        AnimatedContent(
            targetState = onlineUserCount,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
            },
            label = "userCount"
        ) { count ->
            Text(
                text = if (count > 0) "현재 ${count}명의 전우회원이 함께하고 있어요"
                       else "첫 번째 전우회원이 되어보세요",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isCharging) {
            // 충전 중 상태
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TossBlue.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    ChargingProgressBar(
                        batteryLevel = batteryLevel,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val remaining = 100 - batteryLevel
                    val encouragement = when {
                        batteryLevel >= 95 -> "거의 다 됐어요!"
                        batteryLevel >= 80 -> "조금만 더!"
                        batteryLevel >= 50 -> "절반 넘었어요"
                        else -> "충전 중..."
                    }
                    Text(
                        text = "${remaining}% 남음 · $encouragement",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TossBlue
                    )
                }
            }
        } else {
            // 충전 안 함 상태
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BackgroundGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "100% 완충 시 입장할 수 있어요",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 비활성화된 버튼
        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = DividerGray,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (isCharging) "충전 완료 대기 중" else "충전이 필요해요",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdminEntryContent(
    onlineUserCount: Int,
    onEnterAsAdmin: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 접속자 수 안내
        Text(
            text = "현재 ${onlineUserCount}명의 전우회원이 접속 중",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = StatusRed.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "배터리 상태와 관계없이 입장 가능",
                fontSize = 13.sp,
                color = StatusRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onEnterAsAdmin,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "관리자로 입장",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun FloatingParticles(
    particleCount: Int = 40,
    speedMultiplier: Float = 1f
) {
    data class Particle(
        val id: Int,
        var x: Float,
        var y: Float,
        val size: Float,
        val speed: Float,
        val alpha: Float,
        val wobbleOffset: Float,
        val wobbleSpeed: Float
    )

    var particles by remember(particleCount) {
        mutableStateOf(
            List(particleCount) { index ->
                Particle(
                    id = index,
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = Random.nextFloat() * 5f + 2f,
                    speed = (Random.nextFloat() * 0.0015f + 0.0008f) * speedMultiplier,
                    alpha = Random.nextFloat() * 0.35f + 0.1f,
                    wobbleOffset = Random.nextFloat() * 100f,
                    wobbleSpeed = Random.nextFloat() * 0.04f + 0.02f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    LaunchedEffect(time) {
        particles = particles.map { p ->
            val newY = p.y - p.speed
            val wobble = sin((time + p.wobbleOffset) * p.wobbleSpeed) * 0.0015f
            val newX = p.x + wobble

            if (newY < -0.05f) {
                p.copy(
                    x = Random.nextFloat(),
                    y = 1.05f,
                    alpha = Random.nextFloat() * 0.35f + 0.1f
                )
            } else {
                p.copy(x = newX.coerceIn(0f, 1f), y = newY)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2.5f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            drawCircle(
                color = Color.White.copy(alpha = p.alpha * 0.6f),
                radius = p.size * 0.35f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

@Composable
private fun AdminLoginDialog(
    onLogin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🔐", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "관리자 로그인",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        showError = false
                    },
                    label = { Text("비밀번호") },
                    singleLine = true,
                    isError = showError,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TossBlue,
                        focusedLabelColor = TossBlue,
                        errorBorderColor = StatusRed
                    )
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "비밀번호가 틀렸습니다",
                        color = StatusRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onLogin(password)) onDismiss() else showError = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = TossBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("로그인")
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
private fun RankRestoreDialog(
    previousDuration: Long,
    onRestoreWithAd: () -> Unit,
    onStartFresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val previousRank = EliteRank.fromDuration(previousDuration)
    val formattedDuration = EliteRank.fromDurationFormatted(previousDuration)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "🎖️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "계급 복구",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "이전 세션에서 달성한 계급이 있어요",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TossBlue.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = previousRank.koreanName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TossBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDuration,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "광고를 시청하면 이전 계급으로\n바로 시작할 수 있어요",
                    fontSize = 13.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRestoreWithAd,
                colors = ButtonDefaults.buttonColors(containerColor = TossBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "광고 보고 복구", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onStartFresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "새로 시작", color = TextSecondary)
            }
        }
    )
}
