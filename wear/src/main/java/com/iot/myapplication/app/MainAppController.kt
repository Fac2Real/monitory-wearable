package com.iot.myapplication.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.iot.myapplication.presentation.BioMonitorViewModel
import com.iot.myapplication.service.BioMonitoringService

class MainAppController(private val activity: ComponentActivity, private var intent: Intent?) {

    // ViewModel을 직접 생성하지 않음. Compose UI에서 viewModel() 헬퍼로 얻을 것임.
    // private val viewModel = BioMonitorViewModel(activity.application)

    // BioDataSenderToMobile과 ProfileManager는 UI와 서비스 둘 다에서 사용될 수 있으나,
    // 현재 구조에서는 서비스가 데이터 전송을 담당하므로 여기서는 제거해도 됨.
    // private val bioSender = BioDataSenderToMobile(activity)
    // private val profileManager = ProfileManager(activity)


    fun init() {
        Log.d("MainAppController", "init() 호출됨")
        requestPermission()
        // UI 데이터 관찰 로직은 ViewModel이 Repository를 통해 직접 할 것이므로 여기서는 제거
        // observeFilteredDataForUI()
        // 서비스를 시작해야 함!
        startBioMonitoringService()
        // UI는 서비스 시작 요청 후에 보여주는 게 자연스러울 수 있어.
        showUI()
    }

    fun updateIntent(newIntent: Intent) {
        Log.d("MainAppController", "updateIntent() 호출됨")
        this.intent = newIntent
        // TODO: 서비스에 새로운 인텐트 정보를 전달해야 할 수도 있음
        showUI()
    }

    // UI 데이터 관찰 로직 (ViewModel로 이동했거나 Repostiory가 대체함)
    // private fun observeFilteredDataForUI() { ... }

    private fun showUI() {
        // ProfileManager는 UI에 프로필 정보를 전달하기 위해 필요할 수 있음.
        // 서비스에서 프로필 정보를 Intent에 담아 보내거나 Repository에 저장해서
        // UI 쪽에서 ProfileManager 없이 접근하는 방식도 고려 가능.
        // 일단은 MainAppController가 ProfileManager를 관리하고 UI에 전달하는 구조 유지.
        val profileManager = ProfileManager(activity) // 여기서 새로 생성하거나 MainAppController 속성으로 유지
        val profile = profileManager.getProfile(intent)
        val profileId = profileManager.getWorkerId(intent)
        val profileName = profileManager.getWorkerName(intent)
        Log.d("MainAppController","profileId: $profileId, profileName: $profileName")
        Log.d("MainAppController", "showUI() 호출됨.")

        activity.setContent {
            // ViewModel을 직접 생성하지 않고, Compose의 viewModel() 헬퍼 함수 사용
            // 이 헬퍼가 Activity 생명주기에 맞춰 ViewModel 인스턴스를 관리해줌.
            val bioMonitorViewModel: BioMonitorViewModel = androidx.lifecycle.viewmodel.compose.viewModel() // 이 ViewModel이 BioDataRepository를 구독함

            WearApp(
                profileId = profileId, // 프로필 정보 전달
                profileName = profileName,
                bioMonitorViewModel = bioMonitorViewModel // 시스템이 관리하는 ViewModel 인스턴스 전달
            )
        }
    }

    private fun requestPermission() {
        Log.d("MainAppController", "requestPermission() 호출됨")
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("MainAppController", "BODY_SENSORS 권한 승인됨")
            } else {
                Log.d("MainAppController", "BODY_SENSORS 권한 거부됨")
            }
        }
        launcher.launch(Manifest.permission.BODY_SENSORS)
    }

    private fun startBioMonitoringService(){
        Log.d("MainAppController", "startBioMonitoringService() 호출됨")
        val serviceIntent = Intent(activity, BioMonitoringService::class.java)

        // TODO: 서비스에 필요한 데이터 (예: 프로필 ID 등)를 Intent에 담아 전달할 수 있음
        // val profileManager = ProfileManager(activity) // 프로필 정보를 가져오기 위해 필요시 여기서 생성
        // val profile = profileManager.getProfile(intent)
        // serviceIntent.putExtra("user_profile_id", profile?.id)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            activity.startForegroundService(serviceIntent)
            Log.d("MainAppController", "startForegroundService 호출됨")
        }else{
            activity.startService(serviceIntent)
            Log.d("MainAppController", "startService 호출됨 (API < 26)")
        }
    }
}