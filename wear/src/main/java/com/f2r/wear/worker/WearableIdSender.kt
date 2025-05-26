package com.f2r.wear.worker

import android.content.Context
import android.util.Log
import androidx.core.content.edit
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
                sendDeviceIdToMobile(wearableDeviceId)
            }
    }
    private suspend fun sendDeviceIdToMobile(deviceId: String) {
        try{
//            val putDataMapReq = PutDataMapRequest.create(WEARABLE_DEVICE_ID_PATH) // 고유한 경로 사용
//            putDataMapReq.dataMap.putString(KEY_WEARABLE_DEVICE_ID, deviceId)
            // putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis()) // 필요시 타임스탬프 추가
//            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent() // 중요 데이터이므로 urgent 설정

            val putReq = PutDataMapRequest.create(WEARABLE_DEVICE_ID_PATH).apply {
                dataMap.putString(KEY_WEARABLE_DEVICE_ID, deviceId)
            }.asPutDataRequest()

            dataClient.putDataItem(putReq)
                .addOnSuccessListener {
                    Log.d(TAG, "Device ID ($deviceId) sent to mobile successfully. $WEARABLE_DEVICE_ID_PATH")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send Device ID to mobile.", e)
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
    private fun getOrGenerateWearableDeviceId(context: Context): String { // 반환 타입을 String으로 변경
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_WEARABLE_DEVICE_ID, null)
        if (deviceId == null) {
            // Build.getSerial() 대신 UUID 사용
            deviceId = UUID.randomUUID().toString()
            prefs.edit { putString(KEY_WEARABLE_DEVICE_ID, deviceId) }
            Log.i(TAG, "New Wearable Device ID generated using UUID and saved: $deviceId")
        } else {
            Log.i(TAG, "Loaded existing Wearable Device ID: $deviceId")
        }
        return deviceId // deviceId는 이제 null이 될 수 없으므로 !! 사용 (또는 반환 타입만 String으로)
    }
}