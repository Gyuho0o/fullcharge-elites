package com.elites.fullcharge.ui

import android.content.Context
import android.media.AudioManager

/**
 * 타이핑 효과음 유틸리티
 * 시스템 키보드 클릭음 사용
 */
object TypingSoundUtil {
    private var lastSoundTime = 0L
    private const val MIN_SOUND_INTERVAL_MS = 50L  // 최소 50ms 간격

    /**
     * 타이핑 효과음 재생
     */
    fun playTypingSound(context: Context) {
        try {
            val currentTime = System.currentTimeMillis()
            // 너무 자주 재생하지 않도록 제한
            if (currentTime - lastSoundTime < MIN_SOUND_INTERVAL_MS) {
                return
            }
            lastSoundTime = currentTime

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // 시스템 볼륨에 연동 (별도 볼륨 지정 없음)
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        } catch (e: Exception) {
            // 효과음 재생 실패 시 무시 (크래시 방지)
        }
    }
}

/**
 * Context 확장 함수로 간편하게 호출
 */
fun Context.typingSound() {
    TypingSoundUtil.playTypingSound(this)
}
