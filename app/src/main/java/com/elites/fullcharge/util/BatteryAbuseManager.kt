package com.elites.fullcharge.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

/**
 * 풀파워 매니저
 * "넌 충전 중이잖아. 배터리 아낄 필요 없어."
 */
class 풀파워매니저(private val activity: Activity) {

    private var 원래밝기: Int = -1
    private var 원래자동밝기: Int = -1

    /**
     * 풀파워 모드 활성화
     * - 화면 밝기 최대
     * - 화면 항상 켜짐
     */
    fun 풀파워모드시작() {
        화면항상켜기()
        밝기최대로()
    }

    /**
     * 풀파워 모드 비활성화
     * - 원래 설정으로 복구
     */
    fun 풀파워모드종료() {
        화면끄기허용()
        밝기복구()
    }

    private fun 화면항상켜기() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun 화면끄기허용() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun 밝기최대로() {
        try {
            // 현재 밝기 저장
            원래밝기 = Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )

            원래자동밝기 = Settings.System.getInt(
                activity.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )

            // 윈도우 밝기를 최대로 설정 (앱 내에서만 적용)
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = 1.0f // 최대 밝기
            activity.window.attributes = layoutParams
        } catch (e: Exception) {
            // 권한 없음 - 무시
        }
    }

    private fun 밝기복구() {
        try {
            // 윈도우 밝기를 시스템 설정으로 복구
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            activity.window.attributes = layoutParams
        } catch (e: Exception) {
            // 무시
        }
    }

    companion object {
        private var instance: 풀파워매니저? = null

        fun getInstance(activity: Activity): 풀파워매니저 {
            return instance ?: 풀파워매니저(activity).also { instance = it }
        }
    }
}
