package com.elites.fullcharge.update

import android.app.Activity
import android.content.IntentSender
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * Google Play In-App Updates 관리자
 *
 * 사용법:
 * 1. Activity에서 InAppUpdateManager 생성
 * 2. checkForUpdate() 호출하여 업데이트 확인
 * 3. 업데이트 가능 시 startUpdate() 호출
 */
class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var updateInfo: AppUpdateInfo? = null

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> {
                // 다운로드 완료 - 앱 재시작 필요
                onUpdateDownloaded?.invoke()
            }
            InstallStatus.FAILED -> {
                onUpdateFailed?.invoke()
            }
            else -> {}
        }
    }

    // 콜백
    var onUpdateAvailable: ((AppUpdateInfo) -> Unit)? = null
    var onUpdateDownloaded: (() -> Unit)? = null
    var onUpdateFailed: (() -> Unit)? = null
    var onNoUpdateAvailable: (() -> Unit)? = null

    init {
        appUpdateManager.registerListener(installStateUpdatedListener)
    }

    /**
     * 업데이트 확인
     */
    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            updateInfo = info

            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    // 업데이트 가능
                    onUpdateAvailable?.invoke(info)
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // 이전에 시작한 업데이트가 진행 중
                    onUpdateAvailable?.invoke(info)
                }
                else -> {
                    // 업데이트 없음
                    onNoUpdateAvailable?.invoke()
                }
            }
        }.addOnFailureListener {
            // 업데이트 확인 실패 - 앱 사용에는 지장 없음
            onNoUpdateAvailable?.invoke()
        }
    }

    /**
     * 즉시 업데이트 시작 (전체 화면, 앱 사용 불가)
     */
    fun startImmediateUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = updateInfo ?: return

        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                )
            } catch (e: IntentSender.SendIntentException) {
                onUpdateFailed?.invoke()
            }
        }
    }

    /**
     * 유연한 업데이트 시작 (백그라운드 다운로드, 앱 사용 가능)
     */
    fun startFlexibleUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = updateInfo ?: return

        if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            try {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    launcher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            } catch (e: IntentSender.SendIntentException) {
                onUpdateFailed?.invoke()
            }
        }
    }

    /**
     * 다운로드 완료된 업데이트 설치 (앱 재시작)
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    /**
     * 리소스 정리
     */
    fun unregister() {
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
}
