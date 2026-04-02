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
import com.elites.fullcharge.data.BatteryState
import com.elites.fullcharge.data.ChargingType
import com.elites.fullcharge.data.EliteRank
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BatteryMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sessionStartTime: Long = 0L

    private val _shouldExile = MutableStateFlow(false)
    val shouldExile: StateFlow<Boolean> = _shouldExile

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

        // 주기적인 배터리 체크
        startPeriodicCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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

    private fun startPeriodicCheck() {
        serviceScope.launch {
            while (isActive) {
                delay(1000) // 1초마다 체크
                updateNotification()
                // 실제 퇴장 로직은 MainViewModel에서 처리
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // 이미 해제됨
        }
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_EXILE = "extra_exile"
    }
}
