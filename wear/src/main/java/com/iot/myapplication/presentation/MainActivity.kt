/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.iot.myapplication.presentation

import android.Manifest // 권한 관련 클래스
import android.os.Bundle // 액티비티 생명주기 관련 bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts// 액티비티 결과 계약 (권한 요청 등에 사용)
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope// Lifecycle-aware 코루틴 스코프 (액티비티 생명주기에 따라 코루틴 관리)
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.iot.myapplication.app.BioData
import com.iot.myapplication.app.BioMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch // 코루틴을 시작하기 위한 함수

class MainActivity : ComponentActivity() {

    // BioMonitor 클래스의 인스턴스를 선언
    private lateinit var bioMonitor: BioMonitor

    override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bioMonitor = BioMonitor(this)

        // 권한 요청
        requestPermission()

        setContent {
            WearApp(bioMonitor.bioFlow)
        }
        lifecycleScope.launch {
            bioMonitor.bioFlow.collect { data ->
                Log.d("MainActivity", "📡 BioData: $data")
//                sendToServer(data)
            }
        }
    }

    // 생체 센서 권한 (BODY_SENSORS)을 요청하는 함수입니다.
    private fun requestPermission() {
        val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Log.w("Permission", "BODY_SENSORS not granted")
                    // TODO: 사용자에게 권한이 필요한 이유를 설명하거나 다른 조치를 취할 수 있습니다.
                }
            }
        permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
    }

//    private fun sendToServer(data: BioData) {
//        lifecycleScope.launch {
//            try {
//                val response = ApiClient.apiService.sendBioData(data)
//                Log.d("Network", "✅ Server response: ${response.code()}")
//            } catch (e: Exception) {
//                Log.e("Network", "❌ Failed to send data: ${e.message}", e)
//            }
//        }
//    }
    @Composable
    fun WearApp(bioDataFlow: Flow<BioData>) {
        // MaterialTheme을 사용하여 Wear OS 디자인 시스템 테마를 적용합니다.
        MaterialTheme {
            // bioDataFlow를 Compose State로 변환하고 값을 관찰합니다.
            // collectAsStateWithLifecycle을 사용하는 것이 액티비티 생명주기에 더 안전합니다.
            // (lifecycle-runtime-compose 라이브러리 필요)
            // 예: val latestBioData by bioDataFlow.collectAsStateWithLifecycle()
            val latestBioData by bioDataFlow.collectAsState(initial = BioData()) // 초기값은 BioData()

            // 세로 방향으로 컴포넌트를 배치하는 Column 레이아웃입니다.
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp), // 전체 화면 채우고 패딩 추가
                verticalArrangement = Arrangement.Center, // 세로 중앙 정렬
                horizontalAlignment = Alignment.CenterHorizontally // 가로 중앙 정렬
            ) {
                // 앱 제목 텍스트
                Text(
                    text = "Bio Data Monitor",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3 // Wear OS 스타일의 제목 3
                )

                // 심박수 표시 텍스트
                Text(
                    text = "HR: ${latestBioData.heartRate ?: "N/A"} BPM", // heartRate 값이 null이면 "N/A" 표시
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body1 // Wear OS 스타일의 본문 텍스트
                )

                // SpO2 표시 텍스트 (BioData에 spo2 필드가 있다면)
                // BioData 클래스 정의에 spo2, stressLevel 등이 포함되어 있다고 가정합니다.
                latestBioData.spo2?.let { spo2 ->
                    Text(
                        text = "SpO2: $spo2 %",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1
                    )
                }

    //            // 스트레스 레벨 표시 텍스트 (BioData에 stressLevel 필드가 있다면)
    //            latestBioData
    //            latestBioData.stressLevel?.let { stressLevel ->
    //                Text(
    //                    text = "Stress Level: $stressLevel",
    //                    textAlign = TextAlign.Center,
    //                    style = MaterialTheme.typography.body1
    //                )
    //            }
    //
    //            // 활동 상태 표시 텍스트 (BioData에 activityState 필드가 있다면)
    //            latestBioData.activityState?.let { activityState ->
    //                Text(
    //                    text = "Activity: ${activityState.state} (${activityState.activity?.id ?: "Unknown"})",
    //                    textAlign = TextAlign.Center,
    //                    style = MaterialTheme.typography.body2 // Wear OS 스타일의 작은 텍스트
    //                )
    //            }
    //
    //            // 활동 이벤트 표시 (BioData에 activityEvents 필드가 있다면)
    //            if (latestBioData.activityEvents.isNotEmpty()) {
    //                Text(
    //                    text = "Events: ${latestBioData.activityEvents.joinToString { it.type.toString() }}",
    //                    textAlign = TextAlign.Center,
    //                    style = MaterialTheme.typography.body2
    //                )
    //            }

                // TODO: 다른 BioData 필드들도 유사하게 표시할 수 있습니다.
            }
        }
    }
}

// Wear OS 앱의 Compose UI를 정의하는 Composable 함수입니다.
