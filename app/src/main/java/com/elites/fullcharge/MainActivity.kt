package com.elites.fullcharge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.WindowManager
import androidx.core.view.WindowCompat
import com.elites.fullcharge.ad.AdManager
import com.elites.fullcharge.ui.AppScreen
import com.elites.fullcharge.ui.MainViewModel
import com.elites.fullcharge.ui.screens.ChatScreen
import com.elites.fullcharge.ui.screens.ExileScreen
import com.elites.fullcharge.ui.screens.GatekeeperScreen
import com.elites.fullcharge.ui.theme.ElitesTheme
import com.elites.fullcharge.util.SoundManager
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adManager: AdManager
    private lateinit var soundManager: SoundManager

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { viewModel.updateBatteryFromIntent(it) }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 권한 결과 처리
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge 디스플레이 설정 (WindowInsets 직접 제어)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 알림 권한 요청 (Android 13+)
        requestNotificationPermission()

        // 배터리 최적화 무시 요청
        requestBatteryOptimizationExemption()

        // 사운드 매니저 초기화
        soundManager = SoundManager(this)

        // 배터리 상태 리시버 등록
        registerBatteryReceiver()

        // AdMob 초기화
        MobileAds.initialize(this) {}
        adManager = AdManager(this)

        setContent {
            ElitesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // 화면 전환 시 사운드 재생
                LaunchedEffect(uiState.currentScreen) {
                    when (uiState.currentScreen) {
                        AppScreen.CHAT -> soundManager.playEntrySound()
                        AppScreen.EXILE -> soundManager.playExileSound()
                        else -> {}
                    }
                }

                // 위험 모드 시 경고음
                LaunchedEffect(uiState.isInDanger, uiState.dangerCountdown) {
                    if (uiState.isInDanger && uiState.dangerCountdown <= 5) {
                        soundManager.playDangerSound()
                    }
                }

                // 채팅방 진입 시 풀파워 모드 + 광고 미리 로드
                LaunchedEffect(uiState.isInChat) {
                    if (uiState.isInChat) {
                        풀파워모드시작()
                        adManager.loadInterstitialAd() // 퇴장 대비 미리 로드
                    } else {
                        풀파워모드종료()
                    }
                }

                // 배경색 보장 (화면 전환 중 검은색 방지)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.elites.fullcharge.ui.theme.BackgroundWhite)
                ) {
                    MainContent(
                        uiState = uiState,
                        onEnterPortal = { viewModel.enterChat() },
                        onSendMessage = { viewModel.sendMessage(it) },
                        onNicknameChange = { viewModel.changeNickname(it) },
                        onReportMessage = { message, reason, onResult ->
                            viewModel.reportMessage(message, reason, onResult)
                        },
                        onClearFilterError = { viewModel.clearFilterError() },
                        onReply = { viewModel.setReplyingTo(it) },
                        onClearReply = { viewModel.clearReplyingTo() },
                        onToggleReaction = { messageId, emoji ->
                            viewModel.toggleReaction(messageId, emoji)
                        },
                        onCreatePoll = { question, options ->
                            viewModel.createPoll(question, options)
                        },
                        onVotePoll = { messageId, optionIndex ->
                            viewModel.votePoll(messageId, optionIndex)
                        },
                        onExileDismiss = {
                            // 광고 표시 후 게이트키퍼로 이동
                            adManager.showInterstitialAd(this@MainActivity) {
                                viewModel.returnToGatekeeper()
                            }
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        uiState: com.elites.fullcharge.ui.MainUiState,
        onEnterPortal: () -> Unit,
        onSendMessage: (String) -> Unit,
        onNicknameChange: (String) -> Unit,
        onReportMessage: (com.elites.fullcharge.data.ChatMessage, String, (Boolean) -> Unit) -> Unit,
        onClearFilterError: () -> Unit,
        onReply: (com.elites.fullcharge.data.ChatMessage) -> Unit,
        onClearReply: () -> Unit,
        onToggleReaction: (String, String) -> Unit,
        onCreatePoll: (String, List<String>) -> Unit,
        onVotePoll: (String, Int) -> Unit,
        onExileDismiss: () -> Unit
    ) {
        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
                when {
                    targetState == AppScreen.EXILE -> {
                        // 추방 시: 빠른 페이드 인
                        fadeIn(animationSpec = tween(150)) togetherWith
                                fadeOut(animationSpec = tween(150))
                    }
                    targetState == AppScreen.CHAT -> {
                        // 채팅 진입: 스케일 업
                        (fadeIn(animationSpec = tween(400)) +
                                scaleIn(initialScale = 0.9f, animationSpec = tween(400))) togetherWith
                                (fadeOut(animationSpec = tween(400)) +
                                        scaleOut(targetScale = 1.1f, animationSpec = tween(400)))
                    }
                    else -> {
                        // 기본: 페이드
                        fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.GATEKEEPER -> {
                    GatekeeperScreen(
                        batteryState = uiState.batteryState,
                        onlineUserCount = uiState.onlineUserCount,
                        onEnterPortal = onEnterPortal
                    )
                }
                AppScreen.CHAT -> {
                    ChatScreen(
                        batteryState = uiState.batteryState,
                        messages = uiState.messages,
                        onlineUsers = uiState.onlineUsers,
                        currentUserId = uiState.userId,
                        currentUserNickname = uiState.nickname,
                        sessionDuration = uiState.sessionDuration,
                        onSendMessage = onSendMessage,
                        onNicknameChange = onNicknameChange,
                        onReportMessage = onReportMessage,
                        filterErrorMessage = uiState.filterErrorMessage,
                        onClearFilterError = onClearFilterError,
                        replyingTo = uiState.replyingTo,
                        onReply = onReply,
                        onClearReply = onClearReply,
                        onToggleReaction = onToggleReaction,
                        onCreatePoll = onCreatePoll,
                        onVotePoll = onVotePoll,
                        isInDanger = uiState.isInDanger,
                        dangerCountdown = uiState.dangerCountdown
                    )
                }
                AppScreen.EXILE -> {
                    ExileScreen(
                        sessionDuration = uiState.sessionDuration,
                        onDismiss = onExileDismiss
                    )
                }
            }
        }
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(batteryReceiver, filter)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    /**
     * 서바이벌 모드 시작
     * - 화면 항상 켜짐 (메시지를 놓치지 않게)
     * - 밝기는 시스템 설정 유지 (배터리 절약은 사용자 선택)
     */
    private fun 풀파워모드시작() {
        // 화면 항상 켜짐
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 서바이벌 모드 종료
     */
    private fun 풀파워모드종료() {
        // 화면 꺼짐 허용
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateBatteryStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // 이미 해제됨
        }
        soundManager.release()
    }
}
