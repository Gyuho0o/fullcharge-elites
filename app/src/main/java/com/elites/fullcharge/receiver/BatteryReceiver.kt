package com.elites.fullcharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * 배터리 상태 변화 감지용 리시버
 *
 * 참고: 퇴장 로직은 MainViewModel에서 10초 카운트다운으로 처리함
 * 이 리시버는 배터리 상태 변화를 시스템에서 감지하기 위한 용도로만 사용
 */
class BatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        // 배터리 상태 변경은 BatteryStatusManager에서 처리
        // 이 리시버는 앱이 포그라운드에 없을 때 시스템 이벤트를 받기 위한 용도
        // 실제 퇴장 로직은 MainViewModel의 10초 카운트다운에서 처리
    }
}
