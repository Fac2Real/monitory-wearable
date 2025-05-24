package com.iot.myapplication

import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.f2r.mobile.worker.WorkerInfo
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.retrofit.FCMTokenRegistDto
import com.retrofit.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val TAG = "FCMService"
private const val CHANNEL_ID = "fcm_notifications"
class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        val workerId = WorkerInfo.toJson().getString("workerId")
        CoroutineScope(Dispatchers.IO).launch {
            RetrofitClient.instance.sendFCM(FCMTokenRegistDto(workerId, token))
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received: ${remoteMessage.data}")

        // 알림 데이터 처리
        val title = remoteMessage.notification?.title ?: "알림"
        val body = remoteMessage.notification?.body ?: "새로운 메시지가 도착했습니다."
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "default_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 알림 표시
        notificationManager.notify(1, notification)
    }
}