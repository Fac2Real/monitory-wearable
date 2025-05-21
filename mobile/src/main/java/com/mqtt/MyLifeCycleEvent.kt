package com.mqtt

import android.util.Log
import software.amazon.awssdk.crt.mqtt5.Mqtt5Client
import software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions.LifecycleEvents
import software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn
import software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn
import software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn
import software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn
import software.amazon.awssdk.crt.mqtt5.OnStoppedReturn
private const val TAG = "MyLifeCycleEvents"
// AwsIotClientManager.kt 내에 또는 별도의 파일로 정의
class MyLifecycleEvents(private val onConnected: () -> Unit, private val onDisconnected: (cause: Throwable?) -> Unit) : LifecycleEvents {
    override fun onAttemptingConnect(client: Mqtt5Client, eventData: OnAttemptingConnectReturn) {
        Log.i(TAG, "Mqtt5Client: Attempting connection...")
    }

    override fun onConnectionSuccess(client: Mqtt5Client, eventData: OnConnectionSuccessReturn) {
        Log.i(TAG, "Mqtt5Client: Connection successful! Endpoint: ${eventData.connAckPacket?.assignedClientIdentifier}")
        // 연결 성공 시 필요한 작업 수행 (예: 구독 시작, 연결 상태 플래그 업데이트)
        onConnected()
    }

    override fun onConnectionFailure(client: Mqtt5Client, eventData: OnConnectionFailureReturn) {

        Log.e(TAG, "Mqtt5Client: Connection failed! Error: ${eventData.errorCode}")
        // 연결 실패 시 필요한 작업 수행 (예: 재시도 로직, 사용자 알림)
        onDisconnected(Throwable("Connection failed"))
    }

    override fun onDisconnection(client: Mqtt5Client, eventData: OnDisconnectionReturn) {
        Log.i(TAG, "Mqtt5Client: Disconnected. Reason: ${eventData.disconnectPacket?.reasonCode}, Error: ${eventData.errorCode}")
        // 연결 끊김 시 필요한 작업 수행
        onDisconnected(Throwable("Connection failed"))
    }

    override fun onStopped(client: Mqtt5Client, eventData: OnStoppedReturn) {
        Log.i(TAG, "Mqtt5Client: Stopped.")
        // 클라이언트가 완전히 중지되었을 때 호출됨
        onDisconnected(null) // 혹은 다른 상태로 처리
    }
}