package com.elites.fullcharge.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryState(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val chargingType: ChargingType = ChargingType.NONE
) {
    // 입장 자격: 100%만 되면 됨 (충전 여부 무관)
    val isElite: Boolean
        get() = level == 100

    // 안전 상태: 100% 또는 충전 중
    val isSafe: Boolean
        get() = level == 100 || isCharging

    // 위험 상태: 99% 이하 + 충전 안 함
    val isInDanger: Boolean
        get() = level < 100 && !isCharging
}

enum class ChargingType {
    NONE, AC, USB, WIRELESS
}

class BatteryStatusManager(private val context: Context) {

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    fun updateBatteryStatus() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }

        batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level >= 0 && scale > 0) {
                (level * 100) / scale
            } else {
                0
            }

            // 충전기 연결 여부는 EXTRA_PLUGGED로 확인 (BATTERY_STATUS_FULL은 충전기 없이도 될 수 있음)
            val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val isPluggedIn = chargePlug != 0  // 0이면 충전기 미연결

            val chargingType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
                else -> ChargingType.NONE
            }

            _batteryState.value = BatteryState(
                level = batteryPct,
                isCharging = isPluggedIn,
                chargingType = chargingType
            )
        }
    }

    fun updateFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) {
            (level * 100) / scale
        } else {
            _batteryState.value.level
        }

        // 충전기 연결 여부는 EXTRA_PLUGGED로 확인
        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isPluggedIn = chargePlug != 0

        val chargingType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
            else -> ChargingType.NONE
        }

        _batteryState.value = BatteryState(
            level = batteryPct,
            isCharging = isPluggedIn,
            chargingType = chargingType
        )
    }
}
