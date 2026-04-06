package com.elites.fullcharge.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.elites.fullcharge.MainActivity
import com.elites.fullcharge.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EliteMessagingService : FirebaseMessagingService() {

    companion object {
        // 채널 ID 변경 (기존 캐시된 설정 무효화)
        const val CHANNEL_ID = "elite_chat_silent_v2"
        const val CHANNEL_NAME = "채팅 알림"
        const val CHANNEL_DESCRIPTION = "새 메시지 알림 (무음)"

        // 토큰 변경 콜백 (앱에서 설정)
        var onTokenRefresh: ((String) -> Unit)? = null
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 새 토큰 발급 시 콜백 호출
        onTokenRefresh?.invoke(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // 데이터 메시지 처리
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: "완충 전우회"
        val body = data["body"] ?: message.notification?.body ?: "새 메시지가 있습니다"
        val senderNickname = data["senderNickname"] ?: ""

        // 앱이 포그라운드인지 확인하고 알림 표시
        showNotification(title, body, senderNickname)
    }

    private fun showNotification(title: String, body: String, senderNickname: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 생성 (Android 8.0+)
        createNotificationChannel(notificationManager)

        // 알림 클릭 시 앱 열기
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 빌드 (효과음 없이 진동만)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(null)
            .setVibrate(longArrayOf(0, 200))

        // 알림 표시 (ID를 시간 기반으로 해서 여러 알림 표시 가능)
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                // 효과음 비활성화 (진동만 사용)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
