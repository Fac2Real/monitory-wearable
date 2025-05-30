package com.f2r.wear.worker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject
import kotlin.or
import kotlin.text.compareTo

// 모바일 앱 FCMService에서 정의한 경로와 일치해야 함
private const val FCM_MESSAGE_PATH = "/fcm_message"
private const val RESPONSE_WEARABLE_ID_PATH = "/response_wearable_id"

// Wear OS 알림을 위한 채널 ID
private const val WEAR_NOTIFICATION_CHANNEL_ID = "wear_fcm_notifications"

class WorkerInfoListener : WearableListenerService() {
    var workerId: String? = null
    var workerName: String? = null

    override fun onCreate() {
        super.onCreate()
        createWearNotificationChannel()
    }

    @SuppressLint("WearRecents")
    override fun onDataChanged(buffer: DataEventBuffer) {
        buffer.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path == "/worker_info") {

                val jsonString = DataMapItem.fromDataItem(event.dataItem) // 변수명 변경 (json -> jsonString)
                    .dataMap.getString("payload") ?: return@forEach // forEach 람다에서 return

                val jsonObject = JSONObject(jsonString)
                workerId = jsonObject.getString("workerId")
                workerName = jsonObject.getString("name")

                Log.d("WorkerInfo", "Data Changed: $jsonString")
                // ① 로컬 저장
                getSharedPreferences("worker", MODE_PRIVATE)
                    .edit {
                        putString("profile", jsonString)
                        putString("workerId", workerId)
                        putString("workerName", workerName)
                    }

                // ② 워치 화면 자동 표시 (MainActivity 경로 확인 필요)
                // 현재 파일의 패키지명은 com.f2r.wear.worker 이고,
                // 호출하려는 MainActivity는 com.iot.myapplication.presentation.MainActivity 입니다.
                // 이는 일반적으로 다른 앱의 액티비티를 직접 호출하는 방식이므로,
                // 실제 웨어러블 앱의 MainActivity 경로로 수정해야 합니다.
                // 예시: val i = Intent(this, com.f2r.wear.presentation.MainActivity::class.java).apply {
                val i = Intent().apply {
                    // 명시적으로 컴포넌트를 지정하거나, 웨어러블 앱 내의 MainActivity 클래스로 변경
                    // 예: setClassName(this@WorkerInfoListener, "com.your.wearable.app.package.MainActivity")
                    // 또는 Intent(this@WorkerInfoListener, YourWearableMainActivity::class.java)
                    setClassName(applicationContext, "com.iot.myapplication.presentation.MainActivity") // 실제 웨어러블 앱의 MainActivity 클래스로 경로 수정!
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("profile", jsonString) // 모바일에서 전달받은 프로필 정보를 전달할 수 있음
                    // FCM 메시지를 통해 MainActivity를 띄울 경우, 여기서 전달받은 title, body도 넘겨줄 수 있습니다.
                }
                // 액티비티가 없을 경우를 대비한 예외 처리 추가 가능
                try {
                    startActivity(i)
                } catch (e: Exception) {
                    Log.e("WorkerInfoListener", "Failed to start activity for /worker_info", e)
                }
                // buffer.release()는 forEach 루프 밖에서 한 번만 호출하는 것이 좋습니다.
            }
        }
        buffer.release() // 루프 종료 후 한 번만 호출
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WorkerInfoListener", "Message received with path: ${messageEvent.path}")
        when (messageEvent.path) {
            RESPONSE_WEARABLE_ID_PATH -> {
                val wearableId = String(messageEvent.data, Charsets.UTF_8)
                Log.d("WorkerInfoListener", "Received Wearable ID from Mobile: $wearableId")
                // MQTT 전송 또는 다른 작업 수행 -> 이 부분은 WearableIdSender의 역할과 중복될 수 있으므로 확인 필요
                // 만약 이 메시지가 모바일로부터 "네 ID 잘 받았어"라는 응답이라면,
                // WearableIdSender를 또 호출할 필요는 없을 수 있습니다.
                // val wearableIdSender = WearableIdSender()
                // wearableIdSender.sendDeviceIdToMobile() // 이 호출의 목적을 다시 확인해야 합니다.
            }
            FCM_MESSAGE_PATH -> {
                val payloadString = String(messageEvent.data, Charsets.UTF_8)
                Log.d("WorkerInfoListener", "Received FCM message from Mobile: $payloadString")

                // 페이로드 파싱 (모바일에서 "title|body" 형식으로 보냈다고 가정)
                val parts = payloadString.split('|', limit = 2)
                val title = if (parts.isNotEmpty()) parts[0] else "알림"
                val body = if (parts.size > 1) parts[1] else "새로운 메시지가 도착했습니다."

                // Wear OS에 알림 표시
                showWearNotification(title, body)

                // 필요하다면 MainActivity를 띄우거나 특정 UI를 업데이트 할 수도 있습니다.
                // 예: MainActivity에 title, body 전달
                // val intent = Intent(this, com.iot.myapplication.presentation.MainActivity::class.java).apply { // 실제 웨어러블 앱의 MainActivity
                //     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                //     putExtra("fcm_title", title)
                //     putExtra("fcm_body", body)
                // }
                // startActivity(intent)
            }
            else -> {
                Log.w("WorkerInfoListener", "Received unknown message path: ${messageEvent.path}")
            }
        }
    }

    private fun createWearNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FCM 알림" // 사용자에게 표시될 채널 이름
            val descriptionText = "모바일 앱으로부터 수신된 FCM 알림"
            // 중요도(Importance)가 높을수록 알림이 더 눈에 띄게 표시됩니다.
            // IMPORTANCE_HIGH 또는 IMPORTANCE_DEFAULT 는 일반적으로 소리와 진동을 동반합니다 (사용자 설정에 따라 다름).
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(WEAR_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText

                // 진동 활성화 (선택 사항, 채널의 중요도에 따라 기본적으로 활성화될 수 있음)
                enableVibration(true)
                // 커스텀 진동 패턴 설정 (선택 사항)
                // 패턴: 대기(ms), 진동(ms), 대기(ms), 진동(ms)...
                // 예: 0.5초 대기 후 1초 진동
                vibrationPattern = longArrayOf(0, 1000)

                // 소리 설정 (선택 사항, 채널의 중요도에 따라 기본 소리가 재생될 수 있음)
                // 기본 알림 소리를 사용하려면 아래 줄은 필요 없습니다.
                // 특정 소리를 지정하려면 Uri를 사용합니다. 예:
                 val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                 setSound(soundUri, AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build())

                // 알림 배지 (점) 표시 여부 (일반적으로 true)
                setShowBadge(true)

                // 잠금 화면에서의 알림 공개 수준 설정 (예시)
                // VISIBILITY_PUBLIC: 모든 내용 표시
                // VISIBILITY_PRIVATE: 민감한 내용 숨김 (예: "내용 숨김")
                // VISIBILITY_SECRET: 전혀 표시 안 함
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC // 또는 다른 값

                // 일부 기기에서 알림 LED 색상 설정 (웨어러블에서는 덜 일반적)
                // enableLights(true)
                // lightColor = Color.RED
            }
            // 시스템에 채널 등록
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("WorkerInfoListener", "Wear notification channel created with vibration/sound settings.")
        }
    }

    private fun showWearNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(this, com.iot.myapplication.presentation.MainActivity::class.java).apply {
            // MainActivity가 이미 실행 중일 때 새 태스크를 만들지 않고 기존 것을 사용하거나,
            // 특정 데이터를 전달할 수 있습니다.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 필요하다면 title, message 등을 Intent에 추가하여 MainActivity에서 활용
            // putExtra("notification_title", title)
            // putExtra("notification_message", message)
        }
        // PendingIntent 플래그는 SDK 버전에 따라 FLAG_IMMUTABLE 또는 FLAG_MUTABLE 중 적절한 것을 사용해야 합니다.
        // Android 12 (API 31) 이상을 타겟팅한다면 FLAG_IMMUTABLE 또는 FLAG_MUTABLE을 명시해야 합니다.
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // 또는 FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        // 알림 클릭 시 실행될 Intent (옵션: 앱의 MainActivity 실행)
        // val intent = Intent(this, com.iot.myapplication.presentation.MainActivity::class.java).apply { // 실제 웨어러블 앱의 MainActivity
        //     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // }
        // val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, WEAR_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 중요: 웨어러블에 적합한 자체 앱 아이콘으로 변경하세요!
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // 알림 클릭 시 실행할 PendingIntent 설정
            .setAutoCancel(true) // 사용자가 알림을 탭하면 자동으로 알림을 제거

        // 알림 표시: 고유한 ID와 함께 notify() 호출
        // 여러 알림을 구분하기 위해 고유한 ID를 사용합니다.
        // 간단하게 현재 시간을 사용하거나, 알림 내용에 따라 ID를 생성할 수 있습니다.
        val notificationId = SystemClock.uptimeMillis().toInt() // 간단한 고유 ID 생성 예시
        notificationManager.notify(notificationId, notificationBuilder.build())

        Log.d("WorkerInfoListener", "Notification shown with ID: $notificationId, Title: $title")
    }
}