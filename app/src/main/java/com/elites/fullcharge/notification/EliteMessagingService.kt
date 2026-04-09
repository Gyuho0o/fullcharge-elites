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

        // 고정 알림 ID (알림이 쌓이지 않고 교체됨)
        private const val NOTIFICATION_ID = 1001

        // 토큰 변경 콜백 (앱에서 설정)
        var onTokenRefresh: ((String) -> Unit)? = null

        // 읽지 않은 메시지 수 (앱이 포그라운드로 오면 초기화)
        private var unreadMessageCount = 0
        private val recentSenders = mutableListOf<String>()

        /**
         * 앱이 포그라운드로 돌아올 때 호출하여 알림 초기화
         */
        fun clearUnreadCount(context: Context) {
            unreadMessageCount = 0
            recentSenders.clear()
            // 기존 알림도 제거
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
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

        // 메시지 수 증가 및 발신자 기록
        unreadMessageCount++
        if (senderNickname.isNotEmpty() && !recentSenders.contains(senderNickname)) {
            recentSenders.add(senderNickname)
            // 최대 5명까지만 기록
            if (recentSenders.size > 5) {
                recentSenders.removeAt(0)
            }
        }

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

        // 요약 알림 텍스트 생성
        val summaryTitle = "완충 전우회"
        val summaryText = when {
            unreadMessageCount == 1 -> body
            recentSenders.isNotEmpty() -> {
                val sendersText = if (recentSenders.size > 2) {
                    "${recentSenders.takeLast(2).joinToString(", ")} 외 ${recentSenders.size - 2}명"
                } else {
                    recentSenders.joinToString(", ")
                }
                "${unreadMessageCount}개의 새 메시지 ($sendersText)"
            }
            else -> "${unreadMessageCount}개의 새 메시지"
        }

        // 알림 빌드 (효과음 없이 진동만)
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(summaryTitle)
            .setContentText(summaryText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(null)
            .setVibrate(longArrayOf(0, 200))
            .setNumber(unreadMessageCount)  // 배지 카운트

        // 고정 ID로 알림 표시 (기존 알림 교체)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
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
