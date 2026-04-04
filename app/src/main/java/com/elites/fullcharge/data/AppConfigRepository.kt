package com.elites.fullcharge.data

import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * 앱 설정 (강제 업데이트 등) 관리
 */
class AppConfigRepository {

    private val database = FirebaseDatabase.getInstance()
    private val configRef = database.getReference("app_config")

    /**
     * 최소 필요 버전 코드 조회
     * @return 최소 버전 코드 (조회 실패 시 0 반환 = 업데이트 강제 안 함)
     */
    suspend fun getMinVersionCode(): Int {
        return try {
            val snapshot = configRef.child("min_version_code").get().await()
            snapshot.getValue(Int::class.java) ?: 0
        } catch (e: Exception) {
            // 네트워크 오류 등의 경우 업데이트 강제 안 함
            0
        }
    }
}
