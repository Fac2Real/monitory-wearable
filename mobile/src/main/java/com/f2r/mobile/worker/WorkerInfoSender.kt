package com.f2r.mobile.worker

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 모바일에 할당된 사용자 프로필을 워치에 1회 푸시해 매핑한다.
 * Hard-coded sample – replace with DB / API later.
 */

private const val REQUEST_WEARABLE_ID_PATH = "/request_wearable_id"

class WorkerInfoSender : LifecycleService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    override fun onCreate() {
        super.onCreate()
        scope.launch { pushWorkerInfo() }
    }
    fun requestWearableIdFromWear(context: Context) {
        messageClient.sendMessage(
            "wear_node_id", // Wear OS 기기의 노드 ID (연결된 노드에서 가져옴)
            REQUEST_WEARABLE_ID_PATH,
            null // 메시지에 추가 데이터가 필요하지 않으면 null
        ).addOnSuccessListener {
            Log.d("MobileApp", "Wearable ID request sent successfully.")
        }.addOnFailureListener { e ->
            Log.e("MobileApp", "Failed to send Wearable ID request.", e)
        }
    }

    private suspend fun pushWorkerInfo() {
        try {
            // ① 하드코딩 사용자 정보
            val userJson = WorkerInfo.toJson()
            Log.d("WorkerInfoSender", "pushWorkerInfo: User JSON to send: $userJson") // ② 전송할 JSON 데이터 로그
            // ② DataMap 생성 (≤ 100 kB 권장)
            val putReq = PutDataMapRequest.create("/worker_info").apply {
                dataMap.putString("payload", userJson.toString())
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