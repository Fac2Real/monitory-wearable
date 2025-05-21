package com.f2r.mobile.worker

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.mqtt.AwsIotClientManager
import org.json.JSONObject

// SharedPreferences 및 경로/키 상수 정의
private const val TAG = "MobileWearListener"
private const val WEARABLE_DEVICE_ID_PATH = "/test_path_123" // 웨어러블에서 ID 전송 시 사용한 경로
private const val KEY_WEARABLE_DEVICE_ID = "wearableDeviceId"    // 웨어러블에서 ID 전송 시 사용한 키

private const val BIO_DATA_PATH = "/bio_data" // 생체 데이터 경로

private const val MOBILE_PREFS_NAME = "MobileAppPrefs"
private const val KEY_STORED_WEARABLE_ID = "storedWearableDeviceId" // 모바일 앱에 저장할 웨어러블 ID 키

class WearableListener : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged received ${dataEvents.count} events")
        dataEvents.forEach { event ->
            val eventPath = event.dataItem.uri.path
            Log.d(TAG, "Event type: ${event.type}, path: $eventPath")

            if (event.type == DataEvent.TYPE_CHANGED) {
                when (eventPath) {
                    WEARABLE_DEVICE_ID_PATH -> {
                        // 웨어러블 기기 ID 수신 처리
                        try {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val receivedDeviceId = dataMap.getString(KEY_WEARABLE_DEVICE_ID)

                            if (receivedDeviceId != null) {
                                Log.i(TAG, "Received Wearable Device ID from Wearable: $receivedDeviceId")
                                // 모바일 앱의 SharedPreferences에 저장
                                saveWearableDeviceIdToMobilePrefs(applicationContext, receivedDeviceId)
                            } else {
                                Log.w(TAG, "Received null for Wearable Device ID from path $eventPath")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing Wearable Device ID data item", e)
                        }
                    }
                    BIO_DATA_PATH -> {
                        // 생체 데이터 수신 및 MQTT 전송 처리
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap

                        val workerId = dataMap.getString("workerId")
                        val status = dataMap.getLong("status")
                        val heartRate = dataMap.getFloat("heartRate")
                        val timestamp = dataMap.getLong("timestamp")

                        Log.d(TAG, "Received BioData: workerId=$workerId, HR=$heartRate, status=$status, timestamp=$timestamp")

                        // 저장된 wearable_id 가져오기
                        val wearableIdForTopic = getStoredWearableDeviceId(applicationContext)

                        if (wearableIdForTopic == null) {
                            Log.e(TAG, "Wearable ID not found in SharedPreferences. Cannot create MQTT topic.")
                            // ID가 없으면 MQTT 전송을 시도하지 않거나, 기본 토픽 등을 사용할 수 있음
                            return@forEach // 또는 다른 오류 처리
                        }

                        try {
                            val payloadJson = JSONObject()
                            payloadJson.put("wearableDeviceId", wearableIdForTopic) // MQTT 페이로드에도 추가 (선택 사항)
                            payloadJson.put("workerId", workerId)
                            payloadJson.put("status", status)
                            payloadJson.put("heartRate", heartRate)
                            payloadJson.put("timestamp", timestamp)
                            val messagePayload = payloadJson.toString()

                            // 저장된 wearableIdForTopic을 사용하여 토픽 구성
                            val topic = "wearable/$wearableIdForTopic" // 수정된 토픽
                            AwsIotClientManager.publishMessage(topic, messagePayload)
                            Log.d(TAG, "Forwarded BioData to MQTT topic: $topic")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to create JSON or publish MQTT message for BioData", e)
                        }
                    }
                    else -> {
                        Log.d(TAG, "Received data for unhandled path: $eventPath")
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "Data deleted for path: $eventPath")
                // 필요시 데이터 삭제 이벤트 처리
            }
        }
        dataEvents.release() // 루프 바깥에서 한 번만 호출
    }

    private fun saveWearableDeviceIdToMobilePrefs(context: Context, deviceId: String) {
        val prefs = context.getSharedPreferences(MOBILE_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STORED_WEARABLE_ID, deviceId).apply()
        Log.i(TAG, "Saved Wearable Device ID to mobile preferences: $deviceId")
    }

    private fun getStoredWearableDeviceId(context: Context): String? {
        val prefs = context.getSharedPreferences(MOBILE_PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_STORED_WEARABLE_ID, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WearableListenerService destroyed")
    }
}