package com.f2r.wear.worker

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.iot.myapplication.app.FilteredBioData

object DataType {
    const val PERIODIC_HEALTH = "PERIODIC_HEALTH"
    const val IMMEDIATE_EVENT = "IMMEDIATE_EVENT"
    // 필요에 따라 더 많은 데이터 유형 추가
}
class BioDataSenderToMobile(private val context:Context) {
    private val dataClient by lazy { Wearable.getDataClient(context)}

    fun sendBioData(filteredBioData: FilteredBioData, workerId: String) {
        // filteredBioData.originalData.heartRate가 null이 아니고 0f가 아닐 때만 전송
        filteredBioData.originalData.heartRate?.let { heartRateValue ->
            if (heartRateValue != 0f) { // 또는 heartRateValue > 0f 와 같이 필요에 따라 조건 변경
                val request = PutDataMapRequest.create("/bio_data").apply {
                    dataMap.apply {
                        putString("workerId", workerId)
                        putString("sensorType", "heartRate")
                        putFloat("val", heartRateValue) // null이 아님이 보장되므로 'it' 대신 'heartRateValue' 사용
                        // filteredBioData.status가 enum 또는 특정 클래스일 경우 .name 또는 .toString()이 필요할 수 있습니다.
                        // 여기서는 status가 "NORMAL" 문자열과 직접 비교 가능한 것으로 가정합니다.
                        putLong("dangerLevel", if (filteredBioData.status.toString() == "NORMAL") 0L else 1L)
                    }
                }.asPutDataRequest().setUrgent() // 중요 데이터일 수 있으므로 setUrgent() 추가 고려

                dataClient.putDataItem(request)
                    .addOnSuccessListener { Log.d("BioDataSender", "Data sent successfully: $request, Heart Rate: $heartRateValue") }
                    .addOnFailureListener { Log.e("BioDataSender", "Failed to send data", it) }
            } else {
                Log.d("BioDataSender", "Heart rate is 0. Data not sent. Worker ID: $workerId")
            }
        } ?: run {
            // heartRate가 null인 경우 (선택적 로깅)
            Log.d("BioDataSender", "Heart rate is null. Data not sent. Worker ID: $workerId")
        }
    }
}