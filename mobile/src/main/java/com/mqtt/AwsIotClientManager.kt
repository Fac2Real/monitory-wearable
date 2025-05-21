package com.mqtt

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import software.amazon.awssdk.crt.mqtt5.Mqtt5Client
import software.amazon.awssdk.crt.mqtt5.QOS
import software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket
import software.amazon.awssdk.crt.mqtt5.packets.PublishPacket
import software.amazon.awssdk.iot.AwsIotMqtt5ClientBuilder
import java.util.UUID

private const val AWS_IOT_ENDPOINT = "a2q1cmw33m6k7u-ats.iot.ap-northeast-2.amazonaws.com"
private const val TAG = "AwsIotClient"

private const val CERTIFICATE_FILENAME = "c060311b74ab5d78e0c9918acc72ebeb07d4d48accfab78d7296dae0e3718872-certificate.pem.crt"
private const val PRIVATE_KEY_FILENAME = "c060311b74ab5d78e0c9918acc72ebeb07d4d48accfab78d7296dae0e3718872-private.pem.key"
private const val CA_FILENAME = "AmazonRootCA1.pem"


object AwsIotClientManager{
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var awsIotMqtt5Client: Mqtt5Client? = null
    @Volatile
    private var isConnected: Boolean = false
    private fun readAssetFile(context: Context, filename: String):String{
        return try{
            context.assets.open(filename).bufferedReader().use{it.readText()}
        }catch (e: Exception){
            Log.e(TAG, "Failed to read asset file: $filename", e)
            throw e // 파일 읽기 실패 시 예외 발생
        }
    }

    // 앱 설치 시 고유한 클라이언트 ID를 생성하거나 가져오는 함수 (SharedPreferences 사용 예시)
    private fun getUniqueClientId(context: Context): String {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        var clientId = prefs.getString("clientId", null)
        if (clientId == null) {
            // UUID 등으로 고유 ID 생성 (android-mobile- 접두사 붙이기 권장)
            clientId = "android-mobile-${UUID.randomUUID()}"
            prefs.edit().putString("clientId", clientId).apply()
            Log.d(TAG, "새로운 클라이언트 ID 생성: $clientId")
        } else {
            Log.d(TAG, "기존 클라이언트 ID 사용: $clientId")
        }
        return clientId
    }

    fun initializeAndConnect(context: Context) { // 초기화와 연결을 한 번에 (또는 분리해도 됨)
        if (awsIotMqtt5Client != null && isConnected) {
            Log.d(TAG, "AWS IoT MQTT Client 이미 초기화되고 연결됨")
            return
        }
        if (awsIotMqtt5Client != null && !isConnected) {
            Log.d(TAG, "AWS IoT MQTT Client 초기화되었으나 연결되지 않음. 연결 시도.")
            connectClient() // 이미 빌드된 클라이언트로 연결 시도
            return
        }

        try {
            Log.d(TAG, "AWS IoT MQTT Client 초기화 및 연결 시작...")
            val clientId = getUniqueClientId(context) // clientId는 builder에 명시적으로 전달할 수 있습니다.

            // Lifecycle Events 핸들러 정의
            val lifecycleEvents = MyLifecycleEvents(
                onConnected = {
                    isConnected = true
                    Log.i(TAG, "연결 성공 콜백 수신. isConnected = true")
                    // TODO: 연결 성공 후 필요한 작업 (예: 초기 구독 설정)
                    // subscribeToDefaultTopics()
                },
                onDisconnected = { cause ->
                    isConnected = false
                    Log.w(TAG, "연결 끊김/실패 콜백 수신. isConnected = false. 원인: ${cause?.message}")
                    // TODO: 연결 끊김/실패 시 필요한 작업 (예: UI 업데이트, 재시도 로직)
                }
            )
            val connectPacketBuilder = ConnectPacket.ConnectPacketBuilder()
                .withClientId(clientId)
            awsIotMqtt5Client = AwsIotMqtt5ClientBuilder
                .newDirectMqttBuilderWithMtlsFromMemory(
                    AWS_IOT_ENDPOINT,
                    readAssetFile(context, CERTIFICATE_FILENAME), // readAssetFile 사용
                    readAssetFile(context, PRIVATE_KEY_FILENAME)  // readAssetFile 사용
                )
                .withConnectProperties(connectPacketBuilder)
                .withLifeCycleEvents(lifecycleEvents)
                // 라이프사이클 이벤트 리스너 등록
                .build()

            Log.d(TAG, "AWS IoT MQTT Client 빌드 성공. 클라이언트 ID: $clientId. 연결 시도 중...")
            connectClient()

        } catch (e: Exception) {
            Log.e(TAG, "AWS IoT MQTT Client 초기화/빌드 실패", e)
            awsIotMqtt5Client = null
            isConnected = false
        }
    }

    // AWS IoT에 연결하는 함수 (초기화 후 호출)
    // AWS SDK for Kotlin은 클라이언트 사용 시 자동 연결 관리를 하는 경우가 많음.
    // 명시적인 connect 메소드가 없을 수 있으며, publish/subscribe 시 연결 시도.
    // 이 함수는 연결 상태 확인 및 관리를 위한 뼈대 역할.
    fun connect() {
        // Kotlin SDK는 명시적 connect 호출이 필요 없을 수 있음.
        // 클라이언트 사용 시 자동으로 연결됨.
        // 연결 상태 확인이나 연결 끊김 처리 등을 위해 ConnectionState Flow 등을 관찰할 수 있음 (SDK 문서 참고).
        Log.d(TAG, "connect() 호출 (Kotlin SDK는 자동 연결 관리)")
    }
    private fun connectClient() {
        val currentClient = awsIotMqtt5Client
        if (currentClient == null) {
            Log.e(TAG, "MQTT 클라이언트가 빌드되지 않았습니다. 연결 불가.")
            return
        }
        if (isConnected) {
            Log.d(TAG, "이미 연결되어 있습니다.")
            return
        }
        // 연결 시도는 비동기적으로 이루어짐. 결과는 lifecycleEvents에서 처리.
        currentClient.start() // 이전 버전의 connect()에 해당. 비동기적으로 연결 시도.
        Log.d(TAG, "Mqtt5Client.start() 호출됨. 연결 결과는 lifecycle events를 통해 전달됩니다.")
    }
    fun isClientConnected(): Boolean{
        return isConnected && awsIotMqtt5Client != null
    }

    // MQTT 메시지 발행 함수
    fun publishMessage(topic: String, message: String) {
        val currentClient = awsIotMqtt5Client
        if (currentClient == null) {
            Log.e(TAG, "AWS IoT Client가 초기화되지 않았습니다. 발행 실패.")
            // TODO: 초기화 안 된 경우 예외 처리 또는 재시도 로직
            return
        }

        clientScope.launch { // 코루틴 스코프 사용
            try {

                var packet = PublishPacket.PublishPacketBuilder()
                    .withTopic(topic)
                    .withQOS(QOS.AT_LEAST_ONCE)
                    .withPayload(message.toByteArray())
                    .build()
                Log.d(TAG, "MQTT 메시지 발행 시도: 토픽='$topic'")
                val publishResult = currentClient.publish(packet)// 발행 실행 (자동 연결 시도 포함)
                Log.d(TAG, "MQTT 메시지 발행 요청 성공 $publishResult")
            } catch (e: Exception) {
                Log.e(TAG, "MQTT 메시지 발행 실패", e)
                // TODO: 발행 실패 처리 (네트워크 오류, 권한 오류 등)
            }
        }
    }

    // 클라이언트 연결 해제 (앱 종료 시 등)
    fun disconnect() {
        val currentClient = awsIotMqtt5Client
        if (currentClient == null) {
            Log.d(TAG, "AWS IoT Client가 이미 해제되었거나 초기화되지 않았습니다.")
            return
        }

        clientScope.launch { // 코루틴 스코프 사용
            try {
                Log.d(TAG, "AWS IoT 연결 해제 시도...")
                currentClient.close() // 연결 해제 (SDK 버전에 따라 다를 수 있음)
                Log.d(TAG, "AWS IoT 연결 해제 완료")
            } catch (e: Exception) {
                Log.e(TAG, "AWS IoT 연결 해제 실패", e)
            } finally {
                if(awsIotMqtt5Client == currentClient){
                    awsIotMqtt5Client = null
                }
            }
        }
    }

    // 앱 종료 시 클라이언트 스코프 취소
    fun cancelClientScope() {
        Log.d(TAG, "AWS IoT Client 스코프 취소")
        clientScope.cancel("Application Shutdown", null)
    }

}