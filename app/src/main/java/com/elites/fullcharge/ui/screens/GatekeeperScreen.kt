package com.elites.fullcharge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.EliteRank
import com.elites.fullcharge.ui.components.AppLogo
import com.elites.fullcharge.ui.components.AppLogoFooter
import com.elites.fullcharge.ui.components.ChargingParticles
import com.elites.fullcharge.ui.components.RankInsignia
import com.elites.fullcharge.ui.components.SecurityBadge
import com.elites.fullcharge.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

// ============================================================
// GatekeeperScreen - v0 Tactical Military Redesign
// ============================================================

@Composable
fun GatekeeperScreen(
    batteryState: BatteryState,
    onlineUserCount: Int,
    onEnterPortal: () -> Unit,
    restorableSessionDuration: Long? = null,
    onRestoreWithAd: (Long) -> Unit = {},
    onDismissRestore: () -> Unit = {},
    isAdminMode: Boolean = false,
    showAdminLoginDialog: Boolean = false,
    onAdminTapDetected: () -> Unit = {},
    onAdminLogin: (String) -> Boolean = { false },
    onDismissAdminDialog: () -> Unit = {},
    onEnterAsAdmin: () -> Unit = {},
    onAdminLogout: () -> Unit = {},
    onShowOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isElite = batteryState.isElite
    val isCharging = batteryState.isCharging
    val batteryLevel = batteryState.level

    var secretTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onLogin = onAdminLogin,
            onDismiss = onDismissAdminDialog
        )
    }

    // 복구 가능한 세션이 있으면 모달 표시 (isElite 조건 제거)
    // 사용자가 충전기를 뺀 상태로 앱에 진입해도 복구 모달을 볼 수 있도록 함
    // 실제 입장 버튼은 isElite일 때만 활성화되므로 문제 없음
    LaunchedEffect(restorableSessionDuration) {
        if (restorableSessionDuration != null) {
            showRestoreDialog = true
        }
    }

    if (showRestoreDialog && restorableSessionDuration != null) {
        RankRestoreDialog(
            previousDuration = restorableSessionDuration,
            isElite = isElite,
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
            .background(BackgroundBlack)
    ) {
        // 배경 그리드 패턴
        TacticalGridBackground()

        // 배경 파티클 효과 (충전 중일 때만)
        if (isCharging) {
            ChargingParticles()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // ===== HEADER =====
            GatekeeperHeader(
                isAdminMode = isAdminMode,
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

            // ===== MAIN CONTENT =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Security Badge (잠금/해제)
                SecurityBadge(
                    isUnlocked = isElite,
                    size = 128.dp,
                    animated = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Battery Icon
                TacticalBatteryIcon(
                    level = batteryLevel,
                    isCharging = isCharging,
                    isElite = isElite
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Status Text
                StatusTextSection(
                    isElite = isElite,
                    isAdminMode = isAdminMode
                )
            }

            // ===== ACTION SECTION =====
            ActionSection(
                isAdminMode = isAdminMode,
                isElite = isElite,
                isCharging = isCharging,
                batteryLevel = batteryLevel,
                onlineUserCount = onlineUserCount,
                onEnterPortal = {
                    // 복구 가능한 세션이 있으면 모달 표시, 없으면 바로 입장
                    if (restorableSessionDuration != null) {
                        showRestoreDialog = true
                    } else {
                        onEnterPortal()
                    }
                },
                onEnterAsAdmin = onEnterAsAdmin,
                onShowOnboarding = onShowOnboarding
            )

            // ===== WARNING NOTICE =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "경고: 99% 이하로 떨어지면 10초 내 추방됩니다",
                    style = MonoTypography.hudSmall,
                    color = ForegroundMuted.copy(alpha = 0.6f)
                )
            }

            // ===== FOOTER =====
            GatekeeperFooter(isActive = isElite)
        }
    }
}

// ===== HEADER =====
@Composable
private fun GatekeeperHeader(
    isAdminMode: Boolean,
    onSecretTap: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onSecretTap() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Shield 아이콘 (v0와 동일)
                Text(
                    text = "🛡️",
                    fontSize = 16.sp,
                    color = if (isAdminMode) CrisisRed else EliteGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAdminMode) "관리자 모드 | ADMIN" else "검문소 | CHECKPOINT",
                    style = MonoTypography.tracking,
                    color = if (isAdminMode) CrisisRed else ForegroundMuted
                )
            }
        }

        // 하단 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BorderMuted.copy(alpha = 0.5f))
        )
    }
}

// ===== TACTICAL BATTERY ICON =====
// v0: BatteryIcon size="xl" = 240x96 with 스캔라인, 글로우
@Composable
private fun TacticalBatteryIcon(
    level: Int,
    isCharging: Boolean,
    isElite: Boolean
) {
    val color = if (isElite) EliteGreen else CrisisRed

    // 글로우 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "battery_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // 배터리 본체 - v0와 동일한 비율 (더 넓고 납작하게)
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(72.dp)
        ) {
            // 글로우 효과 (외부)
            if (isElite) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(x = 0.dp, y = 0.dp)
                ) {
                    drawRoundRect(
                        color = color.copy(alpha = glowAlpha * 0.4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                        size = size
                    )
                }
            }

            // 배터리 테두리
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 3.dp,
                        color = color,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // 배터리 충전량
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(level / 100f)
                        .padding(5.dp)
                        .background(
                            color = color,
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                // 스캔라인 오버레이 (v0와 동일)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineSpacing = 4.dp.toPx()
                    var y = lineSpacing
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f
                        )
                        y += lineSpacing
                    }
                }

                // 퍼센트 텍스트
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level%",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = if (level > 50) BackgroundBlack else ForegroundWhite
                    )
                }
            }

            // 배터리 팁 (오른쪽)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 6.dp)
                    .width(10.dp)
                    .height(28.dp)
                    .background(color, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
            )
        }
    }
}

// ===== STATUS TEXT SECTION =====
@Composable
private fun StatusTextSection(
    isElite: Boolean,
    isAdminMode: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        if (isAdminMode) {
            Text(
                text = "관리자 접근",
                style = MaterialTheme.typography.headlineMedium,
                color = CrisisRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ADMIN ACCESS: GRANTED",
                style = MonoTypography.subtitle,
                color = CrisisRed.copy(alpha = 0.7f)
            )
        } else if (isElite) {
            Text(
                text = "입장 허가",
                style = MaterialTheme.typography.headlineMedium,
                color = EliteGreen,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // v0와 동일하게 한 줄로 표시 (AnnotatedString 사용)
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = ForegroundMuted)) {
                        append("귀하는 ")
                    }
                    withStyle(style = SpanStyle(color = EliteGreen, fontWeight = FontWeight.Bold)) {
                        append("완충 전우회")
                    }
                    withStyle(style = SpanStyle(color = ForegroundMuted)) {
                        append("의 자격을 갖추었습니다.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SECURITY CLEARANCE: GRANTED",
                style = MonoTypography.subtitle,
                color = EliteGreen.copy(alpha = 0.7f)
            )
        } else {
            Text(
                text = "입장 거부",
                style = MaterialTheme.typography.headlineMedium,
                color = CrisisRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // v0와 동일하게 한 줄로 표시
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = ForegroundMuted)) {
                        append("배터리 충전 상태가 ")
                    }
                    withStyle(style = SpanStyle(color = CrisisRed, fontWeight = FontWeight.Bold)) {
                        append("불량")
                    }
                    withStyle(style = SpanStyle(color = ForegroundMuted)) {
                        append("합니다.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "100% 완충 후 재투입하십시오.",
                style = MaterialTheme.typography.bodySmall,
                color = ForegroundMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SECURITY CLEARANCE: DENIED",
                style = MonoTypography.subtitle,
                color = CrisisRed.copy(alpha = 0.7f)
            )
        }
    }
}

// ===== ACTION SECTION =====
@Composable
private fun ActionSection(
    isAdminMode: Boolean,
    isElite: Boolean,
    isCharging: Boolean,
    batteryLevel: Int,
    onlineUserCount: Int,
    onEnterPortal: () -> Unit,
    onEnterAsAdmin: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    // v0: max-w-xs = 320px, 중앙 정렬
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // v0에는 온라인 유저 수가 없음 - 제거

        // 메인 버튼
        Box(
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            when {
                isAdminMode -> AdminButton(onEnterAsAdmin)
                isElite -> EliteEnterButton(onEnterPortal)
                else -> DisabledButton(isCharging, batteryLevel)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 온보딩 버튼
        OutlinedButton(
            onClick = onShowOnboarding,
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ForegroundMuted
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "신병 교육 안내",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ===== BUTTONS =====
// v0: 버튼 높이 lg, max-w-xs (320dp), 그림자 효과
@Composable
private fun EliteEnterButton(onClick: () -> Unit) {
    // Shimmer 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EliteGreen
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Shimmer 오버레이
            Canvas(modifier = Modifier.fillMaxSize()) {
                val shimmerWidth = size.width * 0.3f
                val start = shimmerOffset * size.width - shimmerWidth
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startX = start,
                        endX = start + shimmerWidth
                    )
                )
            }

            Text(
                text = "전선 투입",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = BackgroundBlack
            )
        }
    }
}

@Composable
private fun AdminButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CrisisRed
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "관리자로 입장",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ForegroundWhite
        )
    }
}

@Composable
private fun DisabledButton(isCharging: Boolean, batteryLevel: Int) {
    Button(
        onClick = { },
        enabled = false,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = MutedBlack,
            disabledContentColor = ForegroundMuted
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isCharging) "충전 완료 대기 중... ${100 - batteryLevel}% 남음" else "충전이 필요해요",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

// ===== FOOTER =====
@Composable
private fun GatekeeperFooter(isActive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(BorderMuted.copy(alpha = 0.3f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppLogoFooter(isActive = isActive)
            }
        }
    }
}

// ===== BACKGROUND EFFECTS =====
@Composable
private fun TacticalGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 20.dp.toPx()
        val lineColor = EliteGreen.copy(alpha = 0.03f)

        // 수직선
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += gridSize
        }

        // 수평선
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += gridSize
        }
    }
}
// ===== DIALOGS =====
@Composable
private fun AdminLoginDialog(
    onLogin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
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
                    color = ForegroundWhite
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
                    label = { Text("비밀번호", color = ForegroundMuted) },
                    singleLine = true,
                    isError = showError,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EliteGreen,
                        focusedLabelColor = EliteGreen,
                        unfocusedBorderColor = BorderMuted,
                        errorBorderColor = CrisisRed,
                        focusedTextColor = ForegroundWhite,
                        unfocusedTextColor = ForegroundWhite
                    )
                )
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "비밀번호가 틀렸습니다",
                        color = CrisisRed,
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
                colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("로그인", color = BackgroundBlack)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = ForegroundMuted)
            }
        }
    )
}

@Composable
private fun RankRestoreDialog(
    previousDuration: Long,
    isElite: Boolean,
    onRestoreWithAd: () -> Unit,
    onStartFresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val previousRank = EliteRank.fromDuration(previousDuration)
    val formattedDuration = EliteRank.fromDurationFormatted(previousDuration)
    var showResetConfirmation by remember { mutableStateOf(false) }

    if (showResetConfirmation) {
        RankResetConfirmDialog(
            previousRank = previousRank,
            onConfirm = {
                showResetConfirmation = false
                onStartFresh()
            },
            onCancel = { showResetConfirmation = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,  // 바깥 터치로 닫힘 - 전선 투입 시 다시 표시됨
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
                    text = "계급 복구",
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
                Text(
                    text = "이전 세션에서 달성한 계급이 있어요",
                    fontSize = 14.sp,
                    color = ForegroundMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EliteGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 계급장
                        RankInsignia(rank = previousRank, size = 40.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        // 계급 이름
                        Text(
                            text = previousRank.koreanName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = EliteGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedDuration,
                            style = MonoTypography.hudMedium,
                            color = ForegroundMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 배터리 상태에 따른 안내 문구
                if (isElite) {
                    Text(
                        text = "광고를 시청하면 이전 계급으로\n바로 시작할 수 있어요",
                        fontSize = 13.sp,
                        color = ForegroundDim,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                } else {
                    Text(
                        text = "⚡ 100% 충전 후 복구할 수 있어요",
                        fontSize = 13.sp,
                        color = CrisisRed,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRestoreWithAd,
                enabled = isElite,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EliteGreen,
                    disabledContainerColor = MutedBlack,
                    disabledContentColor = ForegroundMuted
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isElite) "광고 보고 복구" else "충전 필요",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isElite) BackgroundBlack else ForegroundMuted
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "새로 시작", color = ForegroundMuted)
            }
        }
    )
}

@Composable
private fun RankResetConfirmDialog(
    previousRank: EliteRank,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = CardBlack,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "⚠️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "정말 새로 시작할까요?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CrisisRed
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "현재 보유한 계급",
                    fontSize = 12.sp,
                    color = ForegroundMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 이전 계급 → 훈련병 표시
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 이전 계급 (계급장 + 이름)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RankInsignia(rank = previousRank, size = 24.dp)
                        Text(
                            text = previousRank.koreanName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EliteGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "→",
                        fontSize = 18.sp,
                        color = ForegroundMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    // 훈련병 (계급장 + 이름)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RankInsignia(rank = EliteRank.TRAINEE, size = 24.dp)
                        Text(
                            text = "훈련병",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrisisRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "새로 시작하면 계급이 훈련병으로\n초기화됩니다. 되돌릴 수 없습니다.",
                    fontSize = 13.sp,
                    color = ForegroundDim,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(containerColor = EliteGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "돌아가기", fontWeight = FontWeight.SemiBold, color = BackgroundBlack)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "훈련병부터 다시 시작", color = CrisisRed.copy(alpha = 0.7f))
            }
        }
    )
}
