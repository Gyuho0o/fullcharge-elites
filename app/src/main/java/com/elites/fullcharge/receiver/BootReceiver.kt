package com.elites.fullcharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // 부팅 완료 시 필요한 초기화 작업
            // 현재는 특별한 작업 없음
        }
    }
}
