package com.f2r.mobile.worker

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearableListener: WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        for (event in dataEvents){
            if(event.type == DataEvent.TYPE_CHANGED){
                val path = event.dataItem.uri.path
                if ( path == "/bio_data"){
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val heartRate = dataMap.getFloat("heartRate")
                    val profile = dataMap.getString("profile")
                    val timestamp = dataMap.getLong("timestamp")

                    Log.d("BioDataReceiver", "Received BioData: HR=$heartRate, profile=$profile, timestamp=$timestamp")

                }
            }
        }
    }
}