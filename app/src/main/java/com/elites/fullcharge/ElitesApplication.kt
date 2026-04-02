package com.elites.fullcharge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.elites.fullcharge.data.ElitePreferences
import com.google.firebase.FirebaseApp

class ElitesApplication : Application() {

    lateinit var preferences: ElitePreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Firebase 초기화
        FirebaseApp.initializeApp(this)

        // Preferences 초기화
        preferences = ElitePreferences(this)

        // 알림 채널 생성
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "elite_monitor_channel"
        const val NOTIFICATION_ID = 100

        lateinit var instance: ElitesApplication
            private set
    }
}
