package com.iot.myapplication.app

import android.content.Context
import android.content.Intent
import com.iot.myapplication.R

class ProfileManager(private val context: Context) {
    fun getProfile(intent: Intent?): String {
        return intent?.getStringExtra("profile")
            ?: context.getSharedPreferences("worker", Context.MODE_PRIVATE)
                .getString("profile", context.getString(R.string.worker_info_default))
            ?: context.getString(R.string.worker_info_default)
    }
    fun getWorkerId(intent: Intent?): String {
        return intent?.getStringExtra("workerId")
            ?: context.getSharedPreferences("worker", Context.MODE_PRIVATE)
                .getString("workerId", context.getString(R.string.worker_info_default))
            ?: context.getString(R.string.worker_info_default)
    }
    fun getWorkerName(intent: Intent?): String {
        return intent?.getStringExtra("workerId")
            ?: context.getSharedPreferences("worker", Context.MODE_PRIVATE)
                .getString("workerName", context.getString(R.string.worker_info_default))
            ?: context.getString(R.string.worker_info_default)
    }
}