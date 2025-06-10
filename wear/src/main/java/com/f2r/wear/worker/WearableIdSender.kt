package com.f2r.wear.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.edit
import androidx.core.os.postDelayed
import androidx.lifecycle.LifecycleService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

private const val WEARABLE_DEVICE_ID_PATH = "/test_path_123" // 웨어러블에서 ID 전송 시 사용한 경로
private const val KEY_WEARABLE_DEVICE_ID = "wearableDeviceId" // SharedPreferences 키
private const val PREFS_NAME = "WearAppPrefs" // SharedPreferences 파일 이름

private const val TAG = "WearableIdSender"

class WearableIdSender: LifecycleService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val dataClient by lazy { Wearable.getDataClient(this) }

    private lateinit var wearableDeviceId: String
    override fun onCreate() {
        super.onCreate()
        wearableDeviceId = getOrGenerateWearableDeviceId(this)
        Log.d(TAG, "onCreate: $wearableDeviceId")
        scope.launch {
                sendDeviceIdToMobile()
            }
    }
    fun sendDeviceIdToMobile() {
        try{
//            val putDataMapReq = PutDataMapRequest.create(WEARABLE_DEVICE_ID_PATH) // 고유한 경로 사용
//            putDataMapReq.dataMap.putString(KEY_WEARABLE_DEVICE_ID, deviceId)
            // putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis()) // 필요시 타임스탬프 추가
//            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent() // 중요 데이터이므로 urgent 설정

            val putReq = PutDataMapRequest.create(WEARABLE_DEVICE_ID_PATH).apply {
                dataMap.putString(KEY_WEARABLE_DEVICE_ID, wearableDeviceId)
            }.asPutDataRequest()

            dataClient.putDataItem(putReq)
                .addOnSuccessListener {
                    Log.d(TAG, "Device ID ($deviceId) sent to mobile successfully. $WEARABLE_DEVICE_ID_PATH")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send Device ID to mobile.", e)
                    retrySendWearableId()
                }
        }catch(e: ApiException){
            Log.e("Sender","Wearable API unavailable: ${e.statusCode}")
        } finally {
            stopSelf()
        }
//        task.addOnSuccessListener {
//            Log.d(TAG, "Device ID ($deviceId) sent to mobile successfully. $WEARABLE_DEVICE_ID_PATH")
//        }.addOnFailureListener { e ->
//            Log.e(TAG, "Failed to send Device ID to mobile.", e)
//        }
    }
    private fun retrySendWearableId() {
        // 재시도 로직 (예: 일정 시간 후 재시도)
        Handler(Looper.getMainLooper()).postDelayed({
            sendDeviceIdToMobile()
        }, 5000) // 5초 후 재시도
    }
    private fun getOrGenerateWearableDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_WEARABLE_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString() // 고유 ID 생성
            prefs.edit { putString(KEY_WEARABLE_DEVICE_ID, deviceId) }
            Log.i(TAG, "New Wearable Device ID generated and saved: $deviceId")
        } else {
            Log.i(TAG, "Loaded existing Wearable Device ID: $deviceId")
        }
        return deviceId
    }
}