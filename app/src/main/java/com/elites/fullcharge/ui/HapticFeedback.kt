package com.elites.fullcharge.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * 햅틱 피드백 유틸리티
 * 타이핑 시 짧은 진동 제공
 */
object HapticFeedbackUtil {
    private var lastHapticTime = 0L
    private const val MIN_HAPTIC_INTERVAL_MS = 100L  // 최소 100ms 간격

    /**
     * 타이핑 햅틱 피드백 (가벼운 진동)
     */
    fun triggerTypingHaptic(context: Context) {
        try {
            val currentTime = System.currentTimeMillis()
            // 너무 자주 진동하지 않도록 제한
            if (currentTime - lastHapticTime < MIN_HAPTIC_INTERVAL_MS) {
                return
            }
            lastHapticTime = currentTime

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android Q 이상: EFFECT_TICK 사용
                    it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android O 이상: 짧은 진동 (10ms, 약한 강도)
                    it.vibrate(VibrationEffect.createOneShot(10, 50))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(10)
                }
            }
        } catch (e: Exception) {
            // 진동 실패 시 무시 (크래시 방지)
        }
    }
}

/**
 * Context 확장 함수로 간편하게 호출
 */
fun Context.hapticFeedback() {
    HapticFeedbackUtil.triggerTypingHaptic(this)
}
