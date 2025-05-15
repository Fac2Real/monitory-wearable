//// HeartRateMonitor.kt
//package com.example.wearbiosample
//
//import android.content.Context
//import android.util.Log
//import androidx.core.content.ContextCompat
//import com.google.common.util.concurrent.FutureCallback
//import com.google.common.util.concurrent.Futures
//import kotlinx.coroutines.channels.awaitClose
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.callbackFlow
//import kotlinx.coroutines.guava.await
//
//data class BioData(
//    val heartRate: Float? = null,
//    val spo2: Float? = null,
//    val stressLevel: Int? = null,
//    val activityState: ActivityState? = null,
//    val activityEvents: List<ActivityEvent> = emptyList()
//)
//
//class HeartRateMonitor(context: Context) {
//    private val passiveClient: PassiveMonitoringClient = HealthServices.getClient(context).passiveMonitoringClient
//    val latestHeartRate: Flow<Float?> = callbackFlow {
//        val callback = object : PassiveListenerCallback {
//            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
//                val heartRate = dataPoints.getData(DataType.HEART_RATE_BPM)
//                    .firstOrNull()
//                    ?.value
//                    ?.toFloat()
//
//                heartRate?.let {
//                    Log.d("HeartRate", "💓 Current BPM: $it")
//                    trySend(it) // 수신된 심박수 데이터를 Flow로 전송
//                } ?: run {
//                    Log.d("HeartRate", "❌ No heart rate data in this update")
//                    trySend(null) // 데이터가 없음을 알림 (선택 사항)
//                }
//            }
//
//            override fun onRegistrationFailed(throwable: Throwable) {
//                Log.e("HeartRateMonitor", "❌ Registration failed: ${throwable.message}")
//                close(throwable) // 등록 실패 시 Flow 닫기
//            }
//        }
//
//        val config = PassiveListenerConfig.Builder()
//            .setDataTypes(setOf(DataType.HEART_RATE_BPM))
//            .build()
//
//        try {
//            passiveClient.setPassiveListenerCallback(config, callback)
//            Log.d("HeartRateMonitor", "🔄 Set passive listener callback")
//
//            // 구독 시작 (이전 인터페이스 정의에 따라 setPassiveListenerCallback에서 구독 시작)
//            // 만약 subscribeToPassiveUpdates 함수가 있다면 여기서 호출합니다.
//            // passiveClient.subscribeToPassiveUpdates(config).await()
//            Log.d("HeartRateMonitor", "🔄 Subscribed to heart rate updates")
//
//        } catch (e: Exception) {
//            Log.e("HeartRateMonitor", "Error setting up passive listener: ${e.message}", e)
//            close(e) // 오류 발생 시 Flow 닫기
//        }
//
//        // Flow가 취소될 때 실행되는 cleanup 블록
//        awaitClose {
//            val future = passiveClient.clearPassiveListenerCallbackAsync()
//            Futures.addCallback(future, object : FutureCallback<Void> {
//                override fun onSuccess(result: Void?) {
//                    Log.d("HeartRateMonitor", "🛑 Cleared passive listener callback (Callback)")
//                }
//
//                override fun onFailure(t: Throwable) {
//                    Log.e("HeartRateMonitor", "Error clearing passive listener (Callback): ${t.message}", t)
//                }
//            }, ContextCompat.getMainExecutor(context)) // 콜백이 실행될 Executor 지정
//        }
//    }
//
//    suspend fun stopMonitoring() {
//        try {
//            passiveClient.clearPassiveListenerCallbackAsync().await()
//            Log.d("HeartRateMonitor", "🛑 Stopped monitoring")
//        } catch (e: Exception) {
//            Log.e("HeartRateMonitor", "Error stopping monitoring: ${e.message}", e)
//        }
//    }
//}