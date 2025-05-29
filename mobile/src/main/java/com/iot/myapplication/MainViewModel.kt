package com.iot.myapplication

import android.util.Log
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.f2r.mobile.worker.WorkerInfo
import com.f2r.mobile.worker.WorkerInfoSender
import com.google.firebase.messaging.FirebaseMessaging
import com.mqtt.AwsIotClientManager
import com.retrofit.WorkerLocationRequest
import com.retrofit.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _availableLocations = MutableLiveData<List<Location>>()
    val availableLocations: LiveData<List<Location>> = _availableLocations

    private val _workerPayloadText = MutableLiveData<String>()
    val workerPayloadText: LiveData<String> = _workerPayloadText

    private val appContext: Context = application.applicationContext

    private val _mqttPublishStatus = MutableLiveData<String>()
    val mqttPublishStatus: LiveData<String> = _mqttPublishStatus

    init {
        loadWorkerInfoDisplay()
        initializeMqttClient()
        fetchLocationFromServer()
        fetchFcmToken()
    }

    private fun initializeMqttClient(){
        viewModelScope.launch {
            AwsIotClientManager.initializeAndConnect(appContext)
        }
    }

    private fun fetchLocationFromServer(){
        viewModelScope.launch {
            _mqttPublishStatus.postValue("서버에서 위치 목록 가져오는 중...")
            Log.d("MainViewModel","위치 목록 가져오는 중...")
            delay(2000)
            try{
                val response = RetrofitClient.instance.getAvailableLocations()
                if(response.isSuccessful){
                    val apiResponse = response.body()

                    if(apiResponse != null && apiResponse.success){
                        val locations: List<Location> = apiResponse.data
                        locations.let { _availableLocations.postValue(it) }
                        _mqttPublishStatus.postValue("위치 목록 가져오기 완료! 현재 위치를 선택하세요.")
                        Log.d("MainViewModel","위치 목록 가져오기 완료: ${locations}")
                    } else {
                        _mqttPublishStatus.postValue("서버에서 위치 목록을 가져왔으나, 데이터가 없습니다.")
                        Log.w("MainViewModel", "서버로부터 빈 위치 목록 또는 null 응답 받음")
                        _availableLocations.postValue(emptyList()) // 빈 목록으로 처리
                    }
                }else{
                    Log.d("MainViewModel","response:${response}")
                    // 이곳에 실제 서버 API 호출로직을 구현.
                    val dummyLocations = listOf(
                        Location("20250507165750-827", "생산 라인 A"),
                        Location("20250507171046-862", "포장 구역 B"),
                        Location("20250507191546-243", "품질 검사 C")
                    )

                    _availableLocations.postValue(dummyLocations)
                    _mqttPublishStatus.postValue("위치 목록 가져오기 실패! mock 데이터를 가져왔습니다.")
                    Log.d("MainViewModel","위치 목록 가져오기 실패! mock 데이터를 가져왔습니다.")

                }
            }catch (e: Exception) {
                // 네트워크 오류 또는 기타 예외 처리
                _mqttPublishStatus.postValue("위치 목록 가져오기 중 오류 발생: ${e.message}")
                Log.e("MainViewModel", "API 호출 중 예외 발생", e)
                _availableLocations.postValue(emptyList()) // 오류 시 빈 목록 처리
            }
        }
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // FCM 토큰 가져오기 성공
                val token = task.result
                Log.d("FCMService", "FCM Token: $token")
            } else {
                // FCM 토큰 가져오기 실패
                Log.e("FCMService", "Failed to fetch FCM token", task.exception)
            }
        }
    }
    fun initializeAndStartService(){
        viewModelScope.launch {
            val serviceIntent = Intent(appContext, WorkerInfoSender::class.java)
            appContext.startService(serviceIntent)
        }
    }

    fun postWorkerLocation(location: Location){
        viewModelScope.launch {
            _mqttPublishStatus.postValue("${location.zoneName} 위치의 mqtt 메시지 발행")
            val topic = "wearable/worker/location"
            val workerId = WorkerInfo.toJson().getString("workerId") // 기존 방식대로 workerId 가져오기
            val zoneId = location.zoneId
            // LocalDateTime을 서버가 이해할 수 있는 문자열 형태로 변환 (ISO 8601 권장)
            val timestamp = LocalDateTime.now()
            val requestBody = WorkerLocationRequest(
                workerId = workerId,
                zoneId = zoneId,
                timestamp = timestamp.toString()
            )
            val response = RetrofitClient.instance.postWorkerLocation(requestBody)
            if (response.isSuccessful) {
                // POST 요청 성공 (HTTP 2xx 상태 코드)
                _mqttPublishStatus.postValue("${location.zoneName} 위치 정보 전송 성공!")
                Log.d("MainViewModel", "${location.zoneName} 위치 정보 POST 성공. 요청: $requestBody")

                // UI에 현재 선택된 위치를 표시하는 로직 (기존과 유사하게 유지 가능)
                _workerPayloadText.postValue("현재 작업자 ID:\n${WorkerInfo.toJson().get("workerId")}\n" +
                        "현재 작업자명:\n${WorkerInfo.toJson().get("name")}\n"+

                        "마지막 보고 위치: ${location.zoneName} (POST)")
            } else {
                // POST 요청 실패 (HTTP 오류 코드)
                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                _mqttPublishStatus.postValue("${location.zoneName} 위치 정보 전송 실패: ${response.code()} - $errorBody")
                Log.e("MainViewModel", "${location.zoneName} 위치 정보 POST 실패: ${response.code()} - $errorBody. 요청: $requestBody")
            }
                // Mqtt publish 방식
//            try {
//                val success = AwsIotClientManager.publishMessage(topic, message.toString())
//
//                _mqttPublishStatus.postValue("${location.zoneName} 위치 MQTT 메시지 발행 성공!")
//                Log.d("MainViewModel", "${location.zoneName} 위치 MQTT 메시지 발행 성공! 토픽: $topic, 메시지: $message")
//                // UI에 현재 선택된 위치를 표시하고 싶다면 _workerPayloadText를 업데이트해도 좋아!
//                _workerPayloadText.postValue("현재 작업자 정보:\n${WorkerInfo.toJson()}\n마지막 보고 위치: ${location.zoneName}")
//            } catch (e: Exception) {
//                _mqttPublishStatus.postValue("${location.zoneName} 위치 MQTT 메시지 발행 중 오류: ${e.message}")
//                Log.e("MainViewModel", "${location.zoneName} 위치 MQTT 메시지 발행 중 오류", e)
//            }
        }
    }


    private fun loadWorkerInfoDisplay() {
        _workerPayloadText.value = "현재 작업자 ID: ${WorkerInfo.toJson().get("workerId")}\n" +
                "현재 작업자명: ${WorkerInfo.toJson().get("name")}\n"
    }

    override fun onCleared() {
        super.onCleared()
        AwsIotClientManager.disconnect()
        AwsIotClientManager.cancelClientScope()
    }

}