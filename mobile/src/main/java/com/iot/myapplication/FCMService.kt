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
private const val DEFAULT_CHANNEL_ID = "Monitory"
private const val CHAT_CHANNEL_ID = "chat_channel"
class FCMService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }
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
        Log.d(TAG, "FCM Message Received: ${remoteMessage}")

        // 알림 데이터 처리
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "알림"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "새로운 메시지가 도착했습니다."
//
//        val title = remoteMessage.data.get("title") ?: "알림"
//        val body = remoteMessage.data.get("body") ?: "새로운 메시지가 도착했습니다."
        showNotification(title, body)
    }
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "Monitory",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "일반적인 앱 알림"
            }

            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "채팅 메시지",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "새로운 채팅 메시지 알림"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
            }

            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(chatChannel)
            Log.d(TAG, "Notification channels created in FCMService.")
        }
    }
    private fun showNotification(title: String, message: String) {
        Log.d(TAG, "Showing notification: $title, $message")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "Monitory",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 생성
        val notification = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // 알림 표시
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}