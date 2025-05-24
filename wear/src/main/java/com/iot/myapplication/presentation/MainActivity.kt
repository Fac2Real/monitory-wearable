/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.iot.myapplication.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable


import com.iot.myapplication.app.MainAppController
import java.util.UUID
import androidx.core.content.edit

private const val WEARABLE_DEVICE_ID_PATH = "/test_path_123" // 웨어러블에서 ID 전송 시 사용한 경로
private const val TAG = "MainActivityWear" // 로깅을 위한 태그
private const val PREFS_NAME = "WearAppPrefs" // SharedPreferences 파일 이름
private const val KEY_WEARABLE_DEVICE_ID = "wearableDeviceId" // SharedPreferences 키
class MainActivity : ComponentActivity() {
    private lateinit var controller: MainAppController
    private var wearableDeviceId: String? = null
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        wearableDeviceId = getOrGenerateWearableDeviceId(this)
        Log.d(TAG, "onCreate: $wearableDeviceId")

        wearableDeviceId?.let { sendDeviceIdToMobile(it) }

        controller = MainAppController(this, intent)
        controller.init()

    }
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    private fun getOrGenerateWearableDeviceId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_WEARABLE_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = Build.getSerial()
            prefs.edit() { putString(KEY_WEARABLE_DEVICE_ID, deviceId) }
            Log.i(TAG, "New Wearable Device ID generated and saved: $deviceId")
        } else {
            Log.i(TAG, "Loaded existing Wearable Device ID: $deviceId")
        }
        return deviceId
    }
    private fun sendDeviceIdToMobile(deviceId: String) {
        val dataClient = Wearable.getDataClient(this)
        val putDataMapReq = PutDataMapRequest.create(WEARABLE_DEVICE_ID_PATH) // 고유한 경로 사용
        putDataMapReq.dataMap.putString(KEY_WEARABLE_DEVICE_ID, deviceId)
        // putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis()) // 필요시 타임스탬프 추가
        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent() // 중요 데이터이므로 urgent 설정

        val task = dataClient.putDataItem(putDataReq)
        task.addOnSuccessListener {
            Log.d(TAG, "Device ID ($deviceId) sent to mobile successfully. $WEARABLE_DEVICE_ID_PATH")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to send Device ID to mobile.", e)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        controller.updateIntent(intent)
    }
}