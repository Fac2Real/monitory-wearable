package com.iot.myapplication.service // 서비스는 보통 service 패키지에 넣는 게 일반적이야

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.iot.myapplication.app.BioDataRepository
import com.iot.myapplication.app.ProfileManager // ProfileManager는 MainAppController나 서비스 모두에서 사용할 수 있으니 가져와야겠지
import com.f2r.wear.worker.BioDataSenderToMobile // 데이터 전송 로직
import com.iot.myapplication.app.BioDataStatus // BioDataStatus enum 필요
import com.iot.myapplication.app.BioMonitor
import com.iot.myapplication.app.RuleBasedFilter
import com.iot.myapplication.presentation.MainActivity // 알림 클릭 시 열 MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.content.edit

// TODO: Manifest에 서비스 등록 및 FOREGROUND_SERVICE, BODY_SENSORS 권한 추가 필수!
private const val PREFS_BIO_MONITORING = "BioMonitoringPrefs"
private const val KEY_LAST_NORMAL_SEND_TIME = "lastNormalBioSendTime"
private const val NORMAL_SEND_INTERVAL_MS = 1 * 60 * 1000L // 예: 5분 (정상 상태 데이터 전송 간격)

class BioMonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 서비스가 직접 BioMonitor와 RuleBasedFilter 인스턴스를 가짐
    private lateinit var bioMonitor: BioMonitor
    private lateinit var ruleBasedFilter: RuleBasedFilter // 서비스에서 필터링 로직 수행

    // ViewModel은 여기서 만들지 않음! ViewModel은 UI (Activity/Composable)에서 관리함.
    // private lateinit var bioMonitorViewModel: BioMonitorViewModel

    // 데이터 전송 및 프로필 관리는 서비스에서 계속 수행 가능
    private lateinit var bioDataSender: BioDataSenderToMobile
    private lateinit var profileManager: ProfileManager

    private val sharedPreferences by lazy {
        applicationContext.getSharedPreferences(PREFS_BIO_MONITORING, Context.MODE_PRIVATE)
    }
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "BioMonitoringService - onStartCommand 시작")

        // 1. 포그라운드 서비스 시작
        val notification = createNotification()
        Log.d(TAG, "Notification 생성 완료")
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "startForeground 호출 완료")

        // 2. 필요한 객체들 초기화 (BioMonitor, RuleBasedFilter, Sender, ProfileManager)
        try {
            // 서비스에서 직접 BioMonitor와 RuleBasedFilter 초기화
            bioMonitor = BioMonitor(this) // 서비스의 context 전달
            Log.d(TAG, "BioMonitor 초기화 완료 (서비스 내부)")
            ruleBasedFilter = RuleBasedFilter() // 서비스에서 필터링 로직 관리
            Log.d(TAG, "RuleBasedFilter 초기화 완료 (서비스 내부)")

            // Sender와 ProfileManager도 서비스에서 초기화 유지 (UI와 독립적으로 동작)
            bioDataSender = BioDataSenderToMobile(this)
            Log.d(TAG, "BioDataSenderToMobile 초기화 완료")
            profileManager = ProfileManager(this)
            Log.d(TAG, "ProfileManager 초기화 완료")

        } catch (e: Exception) {
            Log.e(TAG, "객체 초기화 중 오류 발생", e)
            stopSelf()
            return START_NOT_STICKY
        }

        // 3. BioMonitor의 bioFlow에서 Raw 데이터 수신, 필터링, 그리고 Repository에 발행
        serviceScope.launch {
            bioMonitor.bioFlow
                .collectLatest { rawBioData -> // BioMonitor에서 raw BioData 수신
                    Log.d(TAG, "BioMonitor에서 raw BioData 수신 (서비스 내부): $rawBioData")

                    // 서비스 내부에서 raw 데이터를 FilteredBioData로 가공
                    val filteredData = ruleBasedFilter.process(rawBioData) // RuleBasedFilter 사용
                    Log.d(TAG, "서비스 내부에서 FilteredBioData 가공 완료: ${filteredData.status}")

                    // 가공된 데이터를 BioDataRepository에 업데이트
                    BioDataRepository.updateLatestFilteredBioData(filteredData)
                    Log.d(TAG, "FilteredBioData를 BioDataRepository에 발행 완료: ${filteredData.status}")
                    val workerId = profileManager.getWorkerId(null) // 서비스에서는 Activity Intent 접근 어렵.
                    val currentTime = System.currentTimeMillis()
                    val lastNormalSendTime = sharedPreferences.getLong(KEY_LAST_NORMAL_SEND_TIME, 0L)

                    val heartRate = filteredData.originalData.heartRate
                    val statusString = filteredData.status.toString()

                    // 비정상 상태 감지 및 데이터 전송 로직 (서비스에서 수행 유지)
                    if (filteredData.status != BioDataStatus.NORMAL && filteredData.status != BioDataStatus.UNKNOWN) {
                        Log.w(TAG, "서비스 감지: 비정상 생체 상태. 데이터 전송 시작.")
                        // 프로필 정보는 서비스에서 직접 가져오거나 필요한 정보를 미리 서비스로 전달해야 함.
                        // 여기서는 Repository나 SharedPreferences 등 서비스가 접근 가능한 곳에서 가져오는 방식 고려.

                        bioDataSender.sendBioData(filteredData, workerId)
                        // TODO: 위험 알림 발생 로직 (서비스에서 알림 생성 및 표시)
                    } else if (filteredData.status == BioDataStatus.NORMAL ){
                        if( currentTime - lastNormalSendTime >= NORMAL_SEND_INTERVAL_MS){
                            bioDataSender.sendBioData(filteredData, workerId)
                            sharedPreferences.edit() {
                                putLong(
                                    KEY_LAST_NORMAL_SEND_TIME,
                                    currentTime
                                )
                            }
                        }
                    }
                }
        }
        Log.d(TAG, "BioMonitor bioFlow 관찰 시작됨 (서비스 내부)")

        // ViewModel의 filteredBioDataFlow를 여기서 구독할 필요 없음!
        // ViewModel은 Repository를 구독할 것임.

        return START_STICKY
    }

    // ... createNotification() 함수 (이전과 동일) ...
    private fun createNotification(): Notification {
        // ... Notification 생성 코드 ...
        // (이전 코드 그대로 사용)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Factory Safety Monitoring", // 사용자에게 보이는 채널 이름
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Worker bio-data and safety monitoring service"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스마트 팩토리 안전 모니터링 중")
            .setContentText("작업자 상태 및 안전 데이터를 확인하고 있습니다.")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // 적절한 아이콘으로 변경
            .setContentIntent(pendingIntent)
            .setTicker("모니터링 서비스 시작됨")
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d(TAG, "BioMonitoringService - onDestroy 실행됨. 정리 작업 시작.")
        // BioMonitor 리스너 해제
        serviceScope.launch {
            try {
                bioMonitor.stop() // BioMonitor의 stop() 함수 호출
                Log.d(TAG, "BioMonitor 리스너 해제 완료 (서비스 내부)")
            } catch (e: Exception) {
                Log.e(TAG, "BioMonitor 리스너 해제 실패 (서비스 내부)", e)
            }
        }
        // 서비스 스코프 취소
        serviceScope.cancel("서비스 종료")
        Log.d(TAG, "서비스 코루틴 스코프 취소됨")

        super.onDestroy()
        Log.d(TAG, "BioMonitoringService - onDestroy 완료됨.")
    }

    companion object {
        private const val TAG = "BioMonitoringService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "factory_monitor_channel_id"
    }
}