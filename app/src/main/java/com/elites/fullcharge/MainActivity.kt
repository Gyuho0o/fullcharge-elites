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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
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
import com.elites.fullcharge.ui.screens.OnboardingScreen
import com.elites.fullcharge.ui.theme.ElitesTheme
import com.elites.fullcharge.update.InAppUpdateManager
import com.elites.fullcharge.util.HapticManager
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.install.model.ActivityResult

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var adManager: AdManager
    private lateinit var hapticManager: HapticManager
    private lateinit var inAppUpdateManager: InAppUpdateManager

    // In-App Update 결과 처리
    private val updateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    Log.d("InAppUpdate", "Update accepted")
                }
                RESULT_CANCELED -> {
                    // 사용자가 업데이트를 취소함 - 다시 시도하도록 유도
                    Log.d("InAppUpdate", "Update cancelled by user")
                    checkForAppUpdate()
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    Log.e("InAppUpdate", "Update failed")
                }
            }
        }

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

        // 햅틱 매니저 초기화
        hapticManager = HapticManager(this)

        // 배터리 상태 리시버 등록
        registerBatteryReceiver()

        // AdMob 초기화
        MobileAds.initialize(this) {}
        adManager = AdManager(this)

        // In-App Update 초기화 및 체크
        setupInAppUpdate()

        setContent {
            ElitesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                // 화면 전환 시 햅틱 재생
                LaunchedEffect(uiState.currentScreen) {
                    when (uiState.currentScreen) {
                        AppScreen.CHAT -> hapticManager.playEntryHaptic()
                        AppScreen.EXILE -> hapticManager.playExileHaptic()
                        else -> {}
                    }
                }

                // 위험 모드 시 경고 햅틱
                LaunchedEffect(uiState.isInDanger, uiState.dangerCountdown) {
                    if (uiState.isInDanger && uiState.dangerCountdown <= 5) {
                        hapticManager.playDangerHaptic()
                    }
                }

                // 채팅방 진입 시 풀파워 모드 + 광고 미리 로드
                LaunchedEffect(uiState.isInChat) {
                    if (uiState.isInChat) {
                        풀파워모드시작()
                        adManager.loadInterstitialAd() // 퇴장 대비 미리 로드
                    } else {
                        풀파워모드종료()
                        // 복구 가능한 세션이 있으면 보상형 광고 미리 로드
                        if (uiState.restorableSessionDuration != null) {
                            adManager.loadRewardedAd()
                        }
                    }
                }

                // 배경색 보장 (화면 전환 중 검은색 방지)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(com.elites.fullcharge.ui.theme.BackgroundWhite)
                ) {
                    // 온보딩 완료 여부에 따라 화면 분기
                    if (!uiState.onboardingCompleted) {
                        OnboardingScreen(
                            onComplete = { viewModel.completeOnboarding() },
                            isCharging = uiState.batteryState.isCharging,
                            isElite = uiState.batteryState.isElite
                        )
                    } else {
                        MainContent(
                        uiState = uiState,
                        onEnterPortal = { viewModel.enterChat() },
                        onSendMessage = { viewModel.sendMessage(it) },
                        onLeaveChat = {
                            // 광고 표시 후 퇴장 처리
                            adManager.showInterstitialAd(this@MainActivity) {
                                viewModel.leaveChat()
                            }
                        },
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
                        onCreatePoll = { question, options, durationMinutes ->
                            viewModel.createPoll(question, options, durationMinutes)
                        },
                        onVotePoll = { messageId, optionIndex ->
                            viewModel.votePoll(messageId, optionIndex)
                        },
                        onExileDismiss = {
                            // 광고 표시 후 게이트키퍼로 이동
                            adManager.showInterstitialAd(this@MainActivity) {
                                viewModel.returnToGatekeeper()
                            }
                        },
                        onRestoreWithAd = { previousDuration ->
                            // 보상형 광고 표시 후 계급 복구하며 입장
                            adManager.showRewardedAd(
                                activity = this@MainActivity,
                                onRewarded = {
                                    viewModel.enterChatWithRestore(previousDuration)
                                },
                                onAdDismissed = {
                                    // 광고가 닫혔을 때 (보상을 받았든 안 받았든)
                                }
                            )
                        },
                        onDismissRestore = { viewModel.dismissRestorableSession() },
                        onDismissAchievement = { viewModel.dismissAchievementPopup() },
                        onDismissCrisisEscape = { viewModel.dismissCrisisEscapeCelebration() },
                        onDismissChatEvent = { viewModel.dismissChatEvent() },
                        onDismissTimeEvent = { viewModel.dismissTimeEvent() },
                        bannerAdContent = { adManager.BannerAd() },
                        // 관리자 모드
                        onAdminTapDetected = { viewModel.showAdminLoginDialog() },
                        onAdminLogin = { password -> viewModel.tryAdminLogin(password) },
                        onDismissAdminDialog = { viewModel.dismissAdminLoginDialog() },
                        onEnterAsAdmin = { viewModel.enterChatAsAdmin() },
                        onAdminLogout = { viewModel.logoutAdmin() },
                        onKickUser = { userId, nickname -> viewModel.kickUser(userId, nickname) },
                        onChangeUserRank = { userId, nickname, rank -> viewModel.changeUserRank(userId, nickname, rank) },
                        onSendAdminNotice = { message -> viewModel.sendAdminNotice(message) },
                        onDeleteMessage = { messageId -> viewModel.deleteMessage(messageId) },
                        onHandleReport = { reportId, messageId, deleteMsg -> viewModel.handleReport(reportId, messageId, deleteMsg) },
                        onDismissReport = { reportId -> viewModel.dismissReport(reportId) },
                        onShowOnboarding = { viewModel.showOnboarding() }
                    )
                    }
                }
            }
        }
    }

    @Composable
    private fun MainContent(
        uiState: com.elites.fullcharge.ui.MainUiState,
        onEnterPortal: () -> Unit,
        onSendMessage: (String) -> Unit,
        onLeaveChat: () -> Unit,
        onNicknameChange: (String) -> Unit,
        onReportMessage: (com.elites.fullcharge.data.ChatMessage, String, (Boolean) -> Unit) -> Unit,
        onClearFilterError: () -> Unit,
        onReply: (com.elites.fullcharge.data.ChatMessage) -> Unit,
        onClearReply: () -> Unit,
        onToggleReaction: (String, String) -> Unit,
        onCreatePoll: (String, List<String>, Int) -> Unit,
        onVotePoll: (String, Int) -> Unit,
        onExileDismiss: () -> Unit,
        onRestoreWithAd: (Long) -> Unit,
        onDismissRestore: () -> Unit,
        onDismissAchievement: () -> Unit,
        onDismissCrisisEscape: () -> Unit,
        onDismissChatEvent: () -> Unit,
        onDismissTimeEvent: () -> Unit,
        bannerAdContent: @Composable () -> Unit,
        // 관리자 모드
        onAdminTapDetected: () -> Unit,
        onAdminLogin: (String) -> Boolean,
        onDismissAdminDialog: () -> Unit,
        onEnterAsAdmin: () -> Unit,
        onAdminLogout: () -> Unit,
        onKickUser: (String, String) -> Unit,
        onChangeUserRank: (String, String, com.elites.fullcharge.data.EliteRank) -> Unit,
        onSendAdminNotice: (String) -> Unit,
        onDeleteMessage: (String) -> Unit,
        onHandleReport: (String, String, Boolean) -> Unit,
        onDismissReport: (String) -> Unit,
        onShowOnboarding: () -> Unit
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
                        onEnterPortal = onEnterPortal,
                        restorableSessionDuration = uiState.restorableSessionDuration,
                        onRestoreWithAd = onRestoreWithAd,
                        onDismissRestore = onDismissRestore,
                        // 관리자 모드
                        isAdminMode = uiState.isAdminMode,
                        showAdminLoginDialog = uiState.showAdminLoginDialog,
                        onAdminTapDetected = onAdminTapDetected,
                        onAdminLogin = onAdminLogin,
                        onDismissAdminDialog = onDismissAdminDialog,
                        onEnterAsAdmin = onEnterAsAdmin,
                        onAdminLogout = onAdminLogout,
                        // 온보딩 다시보기
                        onShowOnboarding = onShowOnboarding
                    )
                }
                AppScreen.CHAT -> {
                    ChatScreen(
                        batteryState = uiState.batteryState,
                        messages = uiState.messages,
                        onlineUsers = uiState.onlineUsers,
                        allTimeRecords = uiState.allTimeRecords,
                        currentUserId = uiState.userId,
                        currentUserNickname = uiState.nickname,
                        sessionDuration = uiState.sessionDuration,
                        onSendMessage = onSendMessage,
                        onLeaveChat = onLeaveChat,
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
                        dangerCountdown = uiState.dangerCountdown,
                        bannerAdContent = bannerAdContent,
                        newlyUnlockedAchievement = uiState.newlyUnlockedAchievement,
                        onDismissAchievement = onDismissAchievement,
                        unlockedAchievements = uiState.unlockedAchievements,
                        showCrisisEscapeCelebration = uiState.showCrisisEscapeCelebration,
                        onDismissCrisisEscape = onDismissCrisisEscape,
                        latestChatEvent = uiState.latestChatEvent,
                        onDismissChatEvent = onDismissChatEvent,
                        comboState = uiState.comboState,
                        personalHourMilestone = uiState.personalHourMilestone,
                        isHourlyChime = uiState.isHourlyChime,
                        isMidnightSpecial = uiState.isMidnightSpecial,
                        onDismissTimeEvent = onDismissTimeEvent,
                        // 관리자 모드
                        isAdminMode = uiState.isAdminMode,
                        onKickUser = onKickUser,
                        onChangeUserRank = onChangeUserRank,
                        onSendAdminNotice = onSendAdminNotice,
                        onDeleteMessage = onDeleteMessage,
                        // 신고 관리
                        reports = uiState.reports,
                        onHandleReport = onHandleReport,
                        onDismissReport = onDismissReport
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
        viewModel.onAppForeground()  // 포그라운드 복귀 시 모니터링 재개

        // 업데이트 상태 재확인 (백그라운드에서 복귀 시)
        if (::inAppUpdateManager.isInitialized) {
            checkForAppUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackground()  // 백그라운드 전환 시 모니터링 일시 중지
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // 이미 해제됨
        }
        hapticManager.release()

        // In-App Update 리스너 해제
        if (::inAppUpdateManager.isInitialized) {
            inAppUpdateManager.unregister()
        }
    }

    /**
     * In-App Update 초기화
     */
    private fun setupInAppUpdate() {
        inAppUpdateManager = InAppUpdateManager(this)

        inAppUpdateManager.onUpdateAvailable = { info ->
            // 업데이트 가능 - 즉시 업데이트 시작
            Log.d("InAppUpdate", "Update available: ${info.availableVersionCode()}")
            inAppUpdateManager.startImmediateUpdate(updateLauncher)
        }

        inAppUpdateManager.onUpdateDownloaded = {
            // 다운로드 완료 - 설치
            inAppUpdateManager.completeUpdate()
        }

        inAppUpdateManager.onNoUpdateAvailable = {
            Log.d("InAppUpdate", "No update available")
        }

        inAppUpdateManager.onUpdateFailed = {
            Log.e("InAppUpdate", "Update failed")
        }

        // 업데이트 체크 시작
        checkForAppUpdate()
    }

    /**
     * 앱 업데이트 확인
     */
    private fun checkForAppUpdate() {
        inAppUpdateManager.checkForUpdate()
    }
}
