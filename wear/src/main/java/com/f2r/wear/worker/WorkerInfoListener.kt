package com.f2r.wear.worker

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class WorkerInfoListener : WearableListenerService() {
    var workerId: String? = null
    var workerName: String? = null
    @SuppressLint("WearRecents")
    override fun onDataChanged(buffer: DataEventBuffer) {
        buffer.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/worker_info") {

                val json = DataMapItem.fromDataItem(event.dataItem)
                    .dataMap.getString("payload") ?: return

                val jsonObject = JSONObject(json)
                workerId = jsonObject.getString("workerId")
                workerName = jsonObject.getString("name")

                Log.d("WorkerInfo", "received: $json")
                // ① 로컬 저장
                getSharedPreferences("worker", MODE_PRIVATE)
                    .edit {
                        putString("profile",json)
                        putString("workerId", workerId)
                        putString("workerName", workerName)
                    }

                // ② 워치 화면 자동 표시
                val i = Intent(this, com.iot.myapplication.presentation.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("profile", json)
                }

                startActivity(i)

                Log.d("WorkerInfo", "received: $json")
                buffer.release()
            }
        }
    }
}
