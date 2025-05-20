package com.iot.myapplication.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iot.myapplication.R
import com.iot.myapplication.presentation.MainActivity

class NotificationHandler(private val context: Context) {
    private val TAG = "NotificationHandler"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        private const val CHAT_CHANNEL_ID = "chat_channel"
        private var notificationId = 0
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 기본 채널
            val defaultChannel = NotificationChannel(
                DEFAULT_CHANNEL_ID,
                "기본 알림",
                NotificationManager.IMPORTANCE_HIGH // 중요도 설정
            ).apply {
                description = "일반적인 앱 알림"
                // 채널에 대한 진동, 소리 등 기본 설정 가능
            }

            // 채팅 메시지용 채널 (예시)
            val chatChannel = NotificationChannel(
                CHAT_CHANNEL_ID,
                "채팅 메시지",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "새로운 채팅 메시지 알림"
                // 채팅 알림은 다른 소리나 진동 패턴을 사용할 수 있습니다.
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200) // 진동 패턴
            }
            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(chatChannel)
            Log.d(TAG, "Notification channels created.")
        }
    }

    /**
     * 수신된 데이터를 바탕으로 알림을 생성하고 표시합니다.
     * @param title 알림 제목
     * @param body 알림 내용
     * @param messageType 메시지 유형 (채널 선택 등에 사용)
     * @param data 추가 데이터 (PendingIntent 생성 등에 사용)
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun handleNotificationData(
        title: String,
        body: String,
        messageType: String?,
        data: Map<String, String>
    ) {
        Log.d(
            TAG,
            "Handling notification: Title='$title', Body='$body', Type='$messageType', Data='$data'"
        )

        val channelId = when (messageType) {
            "chat" -> CHAT_CHANNEL_ID
            // 다른 메시지 유형에 따라 다른 채널 ID를 반환할 수 있습니다.
            // "announcement" -> ANNOUNCEMENT_CHANNEL_ID
            else -> DEFAULT_CHANNEL_ID
        }
        Log.d("channelId","channelId: $channelId")

        // 알림 클릭 시 실행될 Intent 생성
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 알림을 통해 전달받은 추가 데이터를 Activity로 전달할 수 있습니다.
            // 예를 들어, 채팅방 ID, 특정 게시글 ID 등을 전달하여 해당 화면으로 바로 이동
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            putExtra("notification_source", "fcm_message") // 알림 출처 구분
        }

        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0 /* Request code */,
            intent,
            pendingIntentFlag
        )

        // 알림 소리 설정
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.splash_icon) // 알림 아이콘 (필수) - 실제 리소스로 변경
            .setContentTitle(title) // 제목
            .setContentText(body) // 내용
            .setAutoCancel(true) // 사용자가 탭하면 자동으로 알림 삭제
            .setSound(defaultSoundUri) // 알림 소리
            .setContentIntent(pendingIntent) // 알림 탭 시 실행할 Intent
            .setPriority(NotificationCompat.PRIORITY_HIGH) // 중요도 (헤드업 알림 등에 영향)
        // .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 메시지 카테고리
        // .setColor(ContextCompat.getColor(context, R.color.colorPrimary)) // 아이콘 배경색 등 (선택)
        // .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.ic_large_icon)) // 큰 아이콘 (선택)
        // .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // 긴 텍스트 스타일 (선택)

        // Android 8.0 (API 26) 이상에서는 채널 ID가 필수입니다.
        // 채널 설정은 createNotificationChannels()에서 이미 처리했습니다.

        // 진동 설정 (채널에 설정된 기본값을 따르거나, 여기서 개별 알림에 대해 재정의 가능)
        // 만약 채널에서 진동을 활성화했다면, 기본적으로 해당 채널의 진동 패턴을 따릅니다.
        // 개별 알림에 대해 다른 진동을 주고 싶다면 여기서 설정할 수 있습니다.
        // 예를 들어, 중요한 메시지에 대해서만 다른 진동 패턴을 적용:
        if (messageType == "urgent_alert") {
            val urgentVibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            notificationBuilder.setVibrate(urgentVibrationPattern)
        } else {
            // 채널의 기본 진동 설정을 따르도록 하려면 별도 설정 불필요
            // 또는 여기서 기본 진동을 명시적으로 설정할 수도 있습니다.
            // triggerManualVibration() // 수동 진동 (아래 예시 함수)
        }


        // 알림 표시
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
//        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification sent with ID: $notificationId")

        // (선택 사항) 알림과 별개로 수동 진동을 추가로 트리거하고 싶다면
        // triggerManualVibration(messageType)
    }

    /**
     * 특정 조건에 따라 수동으로 진동을 울리는 예시 함수 (필요한 경우 사용)
     * 일반적으로 알림 채널에 진동을 설정하거나 NotificationCompat.Builder에서 .setVibrate()를 사용하는 것이 권장됩니다.
     */
    private fun triggerManualVibration(messageType: String?) {
        val vibrationPattern = when (messageType) {
            "chat" -> longArrayOf(0, 150, 80, 150) // 짧은 패턴
            "urgent_alert" -> longArrayOf(0, 500, 200, 500) // 긴급 패턴
            else -> longArrayOf(0, 300) // 기본 패턴
        }
    }
}