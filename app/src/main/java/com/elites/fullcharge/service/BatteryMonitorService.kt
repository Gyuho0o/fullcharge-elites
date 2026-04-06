package com.elites.fullcharge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.elites.fullcharge.ElitesApplication
import com.elites.fullcharge.MainActivity
import com.elites.fullcharge.R
import com.elites.fullcharge.data.EliteRank
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class BatteryMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sessionStartTime: Long = 0L
    private var userId: String = ""
    private var nickname: String = ""
    private var isGracefulStop: Boolean = false  // 정상 종료 여부
    private var leaveMessageSent: Boolean = false  // 퇴장 메시지 전송 여부 (중복 방지)

    private val _shouldExile = MutableStateFlow(false)
    val shouldExile: StateFlow<Boolean> = _shouldExile

    // Firebase 참조
    private val database: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val usersRef by lazy { database.getReference("online_users") }
    private val messagesRef by lazy { database.getReference("messages") }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    checkBatteryStatus(intent)
                }
                // 충전기 분리는 이제 퇴장 조건이 아님
                // 99%가 되면 카운트다운, 충전하면 살아남음
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionStartTime = System.currentTimeMillis()

        // 배터리 상태 변화 리시버 등록
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)

        // 주기적인 배터리 체크 및 활동 업데이트
        startPeriodicCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 정상 종료 요청 처리
        if (intent?.action == ACTION_GRACEFUL_STOP) {
            isGracefulStop = true
            stopSelf()
            return START_NOT_STICKY
        }

        // Intent에서 사용자 정보 받기
        intent?.let {
            userId = it.getStringExtra(EXTRA_USER_ID) ?: ""
            nickname = it.getStringExtra(EXTRA_NICKNAME) ?: ""
            sessionStartTime = it.getLongExtra(EXTRA_SESSION_START_TIME, System.currentTimeMillis())
        }

        startForeground(ElitesApplication.NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val duration = System.currentTimeMillis() - sessionStartTime
        val formattedDuration = EliteRank.fromDurationFormatted(duration)
        val rank = EliteRank.fromDuration(duration)

        return NotificationCompat.Builder(this, ElitesApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("${rank.koreanName} | $formattedDuration")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(ElitesApplication.NOTIFICATION_ID, notification)
    }

    private fun checkBatteryStatus(intent: Intent) {
        // 배터리 상태 모니터링만 수행
        // 실제 퇴장 로직은 MainViewModel에서 처리
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            100
        }
        // 알림 업데이트 용도로만 사용
    }

    private fun triggerExile() {
        _shouldExile.value = true

        // 앱을 포그라운드로 가져오면서 추방 처리
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_EXILE, true)
        }
        startActivity(intent)

        // 서비스 종료
        stopSelf()
    }

    private var activityUpdateCounter = 0

    private fun startPeriodicCheck() {
        serviceScope.launch {
            while (isActive) {
                delay(1000) // 1초마다 체크
                updateNotification()

                // 30초마다 Firebase 활동 업데이트 (백그라운드에서도 온라인 유지)
                activityUpdateCounter++
                if (activityUpdateCounter >= 30 && userId.isNotBlank()) {
                    activityUpdateCounter = 0
                    updateUserActivity()
                }
            }
        }
    }

    /**
     * Firebase에 사용자 활동 시간 업데이트 (온라인 상태 유지)
     */
    private fun updateUserActivity() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                usersRef.child(userId).child("lastActiveTime")
                    .setValue(System.currentTimeMillis()).await()
            } catch (e: Exception) {
                // 에러 무시 (네트워크 오류 등)
            }
        }
    }

    /**
     * 서비스 종료 시 퇴장 메시지 전송 (동기 처리)
     */
    private fun sendLeaveMessage() {
        // 이미 전송했거나 정보가 없으면 무시
        if (leaveMessageSent || userId.isBlank() || nickname.isBlank()) return
        leaveMessageSent = true  // 중복 전송 방지

        try {
            // 동기 처리로 앱 종료 전에 완료되도록 함
            kotlinx.coroutines.runBlocking {
                withContext(Dispatchers.IO) {
                    // 퇴장 시스템 메시지 전송
                    val key = messagesRef.push().key ?: return@withContext
                    val message = mapOf(
                        "id" to key,
                        "userId" to "SYSTEM",
                        "nickname" to "시스템",
                        "message" to "${nickname}님이 전우회를 배신했습니다",
                        "timestamp" to System.currentTimeMillis(),
                        "rank" to "TRAINEE",
                        "isSystemMessage" to true
                    )
                    messagesRef.child(key).setValue(message).await()

                    // 온라인 상태 업데이트
                    usersRef.child(userId).child("isOnline").setValue(false).await()
                }
            }
        } catch (e: Exception) {
            // 에러 무시
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 정상 종료가 아닌 경우에만 퇴장 메시지 전송
        if (!isGracefulStop) {
            sendLeaveMessage()
        }

        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // 이미 해제됨
        }
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 앱이 최근 앱 목록에서 제거될 때 즉시 퇴장 메시지 전송
        // onDestroy가 호출되지 않을 수 있으므로 여기서 직접 전송
        if (!isGracefulStop) {
            sendLeaveMessage()
        }
    }

    companion object {
        const val EXTRA_EXILE = "extra_exile"
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_NICKNAME = "extra_nickname"
        const val EXTRA_SESSION_START_TIME = "extra_session_start_time"
        const val ACTION_GRACEFUL_STOP = "com.elites.fullcharge.GRACEFUL_STOP"
    }
}
