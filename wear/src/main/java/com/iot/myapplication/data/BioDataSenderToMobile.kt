package com.iot.myapplication.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.iot.myapplication.app.BioData

class BioDataSenderToMobile(private val context:Context) {
    fun sendBioData(bioData: BioData, profileJson: String) {
        val dataMap = DataMap().apply {
            bioData.heartRate?.let { putFloat("heartRate", it) }
            putString("profile", profileJson)
            putLong("timestamp", System.currentTimeMillis())
        }

        val request = PutDataMapRequest.create("/bio_data").apply {
            dataMap.putAll(dataMap)
        }.asPutDataRequest()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnSuccessListener { Log.d("BioDataSender", "Data sent successfully")}
            .addOnFailureListener { Log.e("BioDataSender", "Failed to send data", it)}
    }
}