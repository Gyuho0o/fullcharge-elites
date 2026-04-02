package com.elites.fullcharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_POWER_DISCONNECTED -> {
                // 충전기 분리됨 - 추방 처리가 필요한 경우 브로드캐스트
                val exileIntent = Intent(ACTION_EXILE_REQUIRED).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(exileIntent)
            }
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level >= 0 && scale > 0) {
                    (level * 100) / scale
                } else {
                    100
                }

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                // 100% 미만이거나 충전 중이 아니면 추방
                if (batteryPct < 100 || !isCharging) {
                    val exileIntent = Intent(ACTION_EXILE_REQUIRED).apply {
                        setPackage(context.packageName)
                    }
                    context.sendBroadcast(exileIntent)
                }
            }
        }
    }

    companion object {
        const val ACTION_EXILE_REQUIRED = "com.elites.fullcharge.ACTION_EXILE_REQUIRED"
    }
}
