package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.components.BackgroundLightning
import com.elites.fullcharge.ui.components.BatteryIndicator
import com.elites.fullcharge.ui.components.ChargingProgressBar
import com.elites.fullcharge.ui.theme.*
import kotlin.math.cos
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
    onAdminLogout: () -> Unit = {},  // 관리자 모드 비활성화
    modifier: Modifier = Modifier
) {
    val isElite = batteryState.isElite  // 100% + 충전 중
    val isCharging = batteryState.isCharging
    val batteryLevel = batteryState.level

    // 비밀 탭 카운트 (타이틀 5번 탭하면 관리자 로그인)
    var secretTapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    // 계급 복구 다이얼로그 표시 여부
    var showRestoreDialog by remember { mutableStateOf(false) }

    // 관리자 로그인 다이얼로그
    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onLogin = onAdminLogin,
            onDismiss = onDismissAdminDialog
        )
    }

    // 복구 가능한 세션이 있고, 입장 가능할 때 다이얼로그 자동 표시
    LaunchedEffect(restorableSessionDuration, isElite) {
        if (restorableSessionDuration != null && isElite) {
            showRestoreDialog = true
        }
    }

    // 계급 복구 다이얼로그
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
            onDismiss = {
                showRestoreDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        // 100%이거나 충전 중일 때 파티클 효과
        if (batteryLevel == 100 || isCharging) {
            FloatingParticles(
                particleCount = if (isElite) 60 else 30,
                speedMultiplier = if (isElite) 1.5f else 1f
            )
        }

        // 입장 가능할 때 번개 효과
        if (isElite) {
            BackgroundLightning(enabled = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 타이틀 (5번 탭하면 관리자 로그인/로그아웃 토글)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        val currentTime = System.currentTimeMillis()
                        // 2초 내 연속 탭만 카운트
                        if (currentTime - lastTapTime < 2000) {
                            secretTapCount++
                            if (secretTapCount >= 5) {
                                secretTapCount = 0
                                if (isAdminMode) {
                                    // 이미 관리자 모드면 로그아웃
                                    onAdminLogout()
                                } else {
                                    // 관리자 모드가 아니면 로그인 시도
                                    onAdminTapDetected()
                                }
                            }
                        } else {
                            secretTapCount = 1
                        }
                        lastTapTime = currentTime
                    }
            ) {
                Text(
                    text = if (isAdminMode) "🔐 완충 전우회" else "완충 전우회",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdminMode) StatusRed else if (isElite) TossBlue else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAdminMode) "관리자 모드 (5탭으로 해제)"
                           else if (isElite) "100% 완충된 자만이 입장 가능"
                           else "100%가 되면 입장할 수 있어요",
                    fontSize = 14.sp,
                    color = if (isAdminMode) StatusRed else TextSecondary
                )
            }

            // 배터리 표시
            BatteryIndicator(
                batteryLevel = batteryState.level,
                isCharging = batteryState.isCharging
            )

            // 메시지 및 버튼 영역
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                if (isAdminMode) {
                    // 관리자 모드: 배터리 상관없이 입장 가능 (최우선)
                    AdminEntryContent(
                        onlineUserCount = onlineUserCount,
                        onEnterAsAdmin = onEnterAsAdmin
                    )
                } else if (isElite) {
                    EliteWelcomeContent(
                        onlineUserCount = onlineUserCount,
                        onEnterPortal = onEnterPortal
                    )
                } else {
                    UnworthyContent(
                        batteryLevel = batteryState.level,
                        isCharging = batteryState.isCharging
                    )
                }
            }
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
                    size = Random.nextFloat() * 6f + 2f,
                    speed = (Random.nextFloat() * 0.002f + 0.001f) * speedMultiplier,
                    alpha = Random.nextFloat() * 0.4f + 0.1f,
                    wobbleOffset = Random.nextFloat() * 100f,
                    wobbleSpeed = Random.nextFloat() * 0.05f + 0.02f
                )
            }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
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
            val wobble = sin((time + p.wobbleOffset) * p.wobbleSpeed) * 0.002f
            val newX = p.x + wobble

            if (newY < -0.05f) {
                p.copy(
                    x = Random.nextFloat(),
                    y = 1.05f,
                    alpha = Random.nextFloat() * 0.4f + 0.1f
                )
            } else {
                p.copy(x = newX.coerceIn(0f, 1f), y = newY)
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { p ->
            // 글로우 효과
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            // 메인 파티클
            drawCircle(
                color = TossBlue.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * size.width, p.y * size.height)
            )
            // 밝은 중심
            drawCircle(
                color = Color.White.copy(alpha = p.alpha * 0.5f),
                radius = p.size * 0.4f,
                center = Offset(p.x * size.width, p.y * size.height)
            )
        }
    }
}

@Composable
private fun EliteWelcomeContent(
    onlineUserCount: Int,
    onEnterPortal: () -> Unit
) {
    // 버튼 펄스 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "buttonPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "입장 준비 완료",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TossBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 접속자 수 애니메이션
        AnimatedContent(
            targetState = onlineUserCount,
            transitionSpec = {
                slideInVertically { -it } + fadeIn() togetherWith
                        slideOutVertically { it } + fadeOut()
            },
            label = "userCount"
        ) { count ->
            Text(
                text = "현재 ${count}명의 전우회원이 대화 중",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 경고 문구
        Text(
            text = "99%가 되면 10초 카운트다운!\n충전하면 살아남아요",
            fontSize = 12.sp,
            color = StatusRed.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 글로우 효과가 있는 버튼
        Box(
            contentAlignment = Alignment.Center
        ) {
            // 글로우 배경
            Box(
                modifier = Modifier
                    .scale(scale * 1.05f)
                    .height(52.dp)
                    .fillMaxWidth(0.8f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TossBlue.copy(alpha = glowAlpha),
                                TossBlue.copy(alpha = 0f)
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            )

            Button(
                onClick = onEnterPortal,
                modifier = Modifier
                    .scale(scale)
                    .height(52.dp)
                    .fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossBlue
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "입장하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun UnworthyContent(
    batteryLevel: Int,
    isCharging: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isCharging) {
            // 충전 안 하고 있음
            Text(
                text = "100%까지 충전하세요",
                fontSize = 18.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "100% 완충일 때만 입장할 수 있어요",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        } else {
            // 충전 중이지만 아직 100% 아님
            Text(
                text = "좋아요, 충전 중이에요",
                fontSize = 18.sp,
                color = TossBlue,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            ChargingProgressBar(
                batteryLevel = batteryLevel,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 남은 퍼센트 표시 - 배터리 레벨에 따른 멘트
            val remaining = 100 - batteryLevel
            val encouragement = when {
                batteryLevel >= 95 -> "거의 다 됐어요!"
                batteryLevel >= 80 -> "조금만 더요"
                batteryLevel >= 50 -> "절반 넘었네요"
                else -> "천천히 기다려요"
            }
            Text(
                text = "${remaining}% 남음 · $encouragement",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TossBlue
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 비활성화된 버튼
        Button(
            onClick = { },
            enabled = false,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = DividerGray,
                disabledContentColor = TextTertiary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isCharging) "100% 완충 대기 중" else "100%가 되면 입장 가능",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 관리자 입장 콘텐츠
 */
@Composable
private fun AdminEntryContent(
    onlineUserCount: Int,
    onEnterAsAdmin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔐 관리자 모드",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = StatusRed,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "현재 ${onlineUserCount}명의 전우회원이 대화 중",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "배터리 상태와 관계없이 입장 가능\n강퇴/계급 변경 권한 보유",
            fontSize = 12.sp,
            color = TextTertiary,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onEnterAsAdmin,
            modifier = Modifier
                .height(52.dp)
                .fillMaxWidth(0.8f),
            colors = ButtonDefaults.buttonColors(
                containerColor = StatusRed
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "관리자로 입장",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

/**
 * 관리자 로그인 다이얼로그
 */
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
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔐",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "관리자 로그인",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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
                    if (onLogin(password)) {
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossBlue
                ),
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

/**
 * 계급 복구 다이얼로그
 */
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
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎖️",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "이전 계급 복구",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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

                // 이전 계급 정보
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = TossBlue.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = previousRank.koreanName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TossBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "접속 시간: $formattedDuration",
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = TossBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "광고 보고 계급 복구",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onStartFresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "새로 시작하기",
                    color = TextSecondary
                )
            }
        }
    )
}
