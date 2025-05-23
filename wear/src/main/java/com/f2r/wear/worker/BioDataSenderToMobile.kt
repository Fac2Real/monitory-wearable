package com.f2r.wear.worker

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.iot.myapplication.app.BioData
import com.iot.myapplication.app.FilteredBioData

object DataType {
    const val PERIODIC_HEALTH = "PERIODIC_HEALTH"
    const val IMMEDIATE_EVENT = "IMMEDIATE_EVENT"
    // 필요에 따라 더 많은 데이터 유형 추가
}
class BioDataSenderToMobile(private val context:Context) {
    private val dataClient by lazy { Wearable.getDataClient(context)}
    fun sendBioData(filteredBioData: FilteredBioData, workerId: String) {
        val request = PutDataMapRequest.create("/bio_data").apply {
            dataMap.apply {
                filteredBioData.originalData.heartRate?.let {
                    putString("workerId", workerId)
                    putString("sensorType", "heartRate")
                    putFloat("val", it)
                    putLong("dangerLevel", if (filteredBioData.status.toString() == "NORMAL") 0L else 1L)

                }
            }
        }.asPutDataRequest()

        dataClient.putDataItem(request)
            .addOnSuccessListener { Log.d("BioDataSender", "Data sent successfully $request")}
            .addOnFailureListener { Log.e("BioDataSender", "Failed to send data", it)}
    }
}