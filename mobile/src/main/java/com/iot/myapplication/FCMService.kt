package com.iot.myapplication

import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.f2r.mobile.worker.WorkerInfo
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.retrofit.FCMTokenRegistDto
import com.retrofit.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.forEach


private const val TAG = "FCMService"
private const val DEFAULT_CHANNEL_ID = "Monitory"
private const val CHAT_CHANNEL_ID = "chat_channel"

// Wear OS 앱과의 통신을 위한 경로 및 Capability 정의
private const val WEAR_APP_CAPABILITY = "wear_app_capability" // Wear OS 앱의 AndroidManifest.xml에 정의된 capability와 일치해야 함
private const val FCM_MESSAGE_PATH = "/fcm_message" // 모바일 -> 웨어러블 메시지 경로
class FCMService : FirebaseMessagingService() {
    private lateinit var messageClient: MessageClient
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var nodeClient: NodeClient

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        messageClient = Wearable.getMessageClient(this)
        nodeClient = Wearable.getNodeClient(this)
        capabilityClient = Wearable.getCapabilityClient(this)
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        try {
            val workerId = WorkerInfo.toJson().getString("workerId")
            CoroutineScope(Dispatchers.IO).launch {
                RetrofitClient.instance.sendFCM(FCMTokenRegistDto(workerId, token))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workerId for FCM token registration", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message Received: ${remoteMessage}")

        // 알림 데이터 처리
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "알림"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "새로운 메시지가 도착했습니다."

        // 1. 모바일 자체에 알림 표시 (기존 로직)
        showNotification(title, body)

        // 2. Wear OS 기기로 메시지 전송
        sendFcmDataToWearable(title, body)
    }
    private fun sendFcmDataToWearable(title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connectedNodesList = nodeClient.connectedNodes.await()
                if (connectedNodesList.isEmpty()) {
                    Log.d(TAG, "NodeClient: No connected nodes found at all.")
                } else {
                    Log.d(TAG, "NodeClient: Found ${connectedNodesList.size} connected node(s):")
                    connectedNodesList.forEach { node ->
                        Log.d(TAG, "Node: ${node.displayName}, ID: ${node.id}, isNearby: ${node.isNearby}")
                    }
                }
                if (connectedNodesList.isEmpty()) {
                    Log.d(TAG, "No wearable device with '$WEAR_APP_CAPABILITY' capability found or connected.")
                    return@launch
                }

                // 모든 연결된 노드에 메시지 전송 (보통은 하나의 웨어러블만 연결됨)
                connectedNodesList.forEach { node ->
                    // 메시지 페이로드 생성 (ByteArray로 변환)
                    // 간단한 문자열 결합 또는 JSON/ProtoBuf 사용 가능
                    val payloadString = "$title|$body" // 간단한 구분자 사용 예시
                    val payload: ByteArray = payloadString.toByteArray(Charsets.UTF_8)

                    messageClient.sendMessage(node.id, FCM_MESSAGE_PATH, payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM message sent to wearable (${node.displayName}): $payloadString")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send FCM message to wearable (${node.displayName})", e)
                        }
                }
                // ... 기존 capabilityClient 로직 ...
            } catch (e: Exception) {
                Log.e(TAG, "Error in NodeClient check or capability check", e)
            }
        }
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