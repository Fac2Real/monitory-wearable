package com.f2r.mobile.worker

import android.util.Log
import androidx.lifecycle.LifecycleService          // 서비스 예시
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.*          // Data Layer
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 모바일에 할당된 사용자 프로필을 워치에 1회 푸시해 매핑한다.
 * Hard-coded sample – replace with DB / API later.
 */
class WorkerInfoSender : LifecycleService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataClient by lazy { Wearable.getDataClient(this) }

    override fun onCreate() {
        super.onCreate()
        scope.launch { pushWorkerInfo() }
    }

    private suspend fun pushWorkerInfo() {
        try {
            // ① 하드코딩 사용자 정보
            val userJson = WorkerInfo.toJson()

            // ② DataMap 생성 (≤ 100 kB 권장)
            val putReq = PutDataMapRequest.create("/worker_info").apply {
                dataMap.putString("payload", userJson)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest()      // 자동 change-ID 삽입

            // ③ 보내기 (auto-sync & offline queue 지원)
            dataClient.putDataItem(putReq).await()
        }catch(e: ApiException){
            Log.e("Sender","Wearable API unavailable: ${e.statusCode}")
        } finally {
            stopSelf()
        }
    }
}