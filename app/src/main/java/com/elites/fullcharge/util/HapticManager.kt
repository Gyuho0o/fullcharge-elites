package com.elites.fullcharge.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 앱 햅틱 피드백 관리
 * - 입장 햅틱
 * - 메시지 수신 햅틱
 * - 퇴장/경고 햅틱
 * - 리액션 햅틱
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator

    // 햅틱 활성화 여부
    var hapticEnabled: Boolean = true

    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * 입장 햅틱 (전기/번개 느낌 - 빠른 연속 진동)
     */
    fun playEntryHaptic() {
        if (!hapticEnabled) return
        vibrateElectric()
    }

    /**
     * 메시지 수신 햅틱 (짧은 틱)
     */
    fun playMessageHaptic() {
        if (!hapticEnabled) return
        vibrateTick()
    }

    /**
     * 퇴장/경고 햅틱 (강한 진동)
     */
    fun playExileHaptic() {
        if (!hapticEnabled) return
        vibrateHeavy()
    }

    /**
     * 위험 경고 햅틱 (연속 패턴)
     */
    fun playDangerHaptic() {
        if (!hapticEnabled) return
        vibrateWarning()
    }

    /**
     * 리액션 햅틱 (가벼운 틱)
     */
    fun playReactionHaptic() {
        if (!hapticEnabled) return
        vibrateLight()
    }

    /**
     * 투표 햅틱
     */
    fun playVoteHaptic() {
        if (!hapticEnabled) return
        vibrateMedium()
    }

    /**
     * 랭크업 햅틱 (축하 패턴)
     */
    fun playRankUpHaptic() {
        if (!hapticEnabled) return
        vibrateCelebration()
    }

    /**
     * 멘션 햅틱
     */
    fun playMentionHaptic() {
        if (!hapticEnabled) return
        vibrateDouble()
    }

    /**
     * 가벼운 틱 (10ms)
     */
    private fun vibrateLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10)
            }
        } catch (e: Exception) { }
    }

    /**
     * 일반 틱 (20ms)
     */
    private fun vibrateTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
        } catch (e: Exception) { }
    }

    /**
     * 중간 강도 (40ms)
     */
    private fun vibrateMedium() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Exception) { }
    }

    /**
     * 강한 진동 (100ms)
     */
    private fun vibrateHeavy() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) { }
    }

    /**
     * 더블 틱 (멘션용)
     */
    private fun vibrateDouble() {
        try {
            val pattern = longArrayOf(0, 30, 50, 30)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) { }
    }

    /**
     * 경고 패턴 (위험 상황)
     */
    private fun vibrateWarning() {
        try {
            val pattern = longArrayOf(0, 50, 50, 50, 50, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) { }
    }

    /**
     * 전기/번개 느낌의 진동 패턴 (입장)
     */
    private fun vibrateElectric() {
        try {
            val pattern = longArrayOf(0, 20, 30, 15, 40, 25, 20, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) { }
    }

    /**
     * 축하 패턴 (랭크업)
     */
    private fun vibrateCelebration() {
        try {
            val pattern = longArrayOf(0, 30, 50, 30, 50, 30, 100, 80)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) { }
    }

    fun release() {
        // Vibrator는 별도 release 불필요
    }
}
