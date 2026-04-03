package com.elites.fullcharge.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 앱 효과음 관리
 * - 입장 효과음
 * - 메시지 수신 효과음
 * - 퇴장/경고 효과음
 * - 리액션 효과음
 */
class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator

    // 효과음 활성화 여부
    var soundEnabled: Boolean = true
    var vibrationEnabled: Boolean = true

    init {
        // SoundPool 초기화
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        // ToneGenerator 초기화 (시스템 톤용)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
        } catch (e: Exception) {
            // 초기화 실패 시 무시
        }

        // Vibrator 초기화
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * 입장 효과음 (상승 톤)
     */
    fun playEntrySound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 150)
        } catch (e: Exception) { }
        vibrateShort()
    }

    /**
     * 메시지 수신 효과음 (짧은 틱)
     */
    fun playMessageSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
        } catch (e: Exception) { }
    }

    /**
     * 퇴장/경고 효과음 (하강 톤)
     */
    fun playExileSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200)
        } catch (e: Exception) { }
        vibrateLong()
    }

    /**
     * 위험 경고 효과음 (긴급 톤)
     */
    fun playDangerSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
        } catch (e: Exception) { }
        vibratePattern()
    }

    /**
     * 리액션 효과음 (가벼운 팝)
     */
    fun playReactionSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 30)
        } catch (e: Exception) { }
    }

    /**
     * 투표 효과음
     */
    fun playVoteSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
        } catch (e: Exception) { }
        vibrateShort()
    }

    /**
     * 랭크업 효과음 (축하 톤)
     */
    fun playRankUpSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 300)
        } catch (e: Exception) { }
        vibrateLong()
    }

    /**
     * 멘션 효과음
     */
    fun playMentionSound() {
        if (!soundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
        } catch (e: Exception) { }
        vibrateShort()
    }

    private fun vibrateShort() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) { }
    }

    private fun vibrateLong() {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) { }
    }

    private fun vibratePattern() {
        if (!vibrationEnabled) return
        try {
            val pattern = longArrayOf(0, 50, 50, 50)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) { }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
