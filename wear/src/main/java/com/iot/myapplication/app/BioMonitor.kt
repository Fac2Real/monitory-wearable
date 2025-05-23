package com.iot.myapplication.app


import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.PassiveMonitoringClient
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await

class BioMonitor(context: Context) {

    private val passiveClient: PassiveMonitoringClient=
        HealthServices.getClient(context).passiveMonitoringClient

    val bioFlow: Flow<BioData> = callbackFlow {
        val callback = object : PassiveListenerCallback {
            override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
                val heartRate = dataPoints.getData(DataType.HEART_RATE_BPM)
                    .firstOrNull()?.value?.toFloat()


                val bioData = BioData(
                    heartRate = heartRate,
                )

                Log.d("BioMonitor", "🧬 Received: $bioData")
                trySend(bioData)
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.e("BioMonitor", "❌ Registration failed: ${throwable.message}")
                close(throwable)
            }
        }

        val config = PassiveListenerConfig.Builder()
            .setDataTypes(
                setOf(
                    DataType.HEART_RATE_BPM,
//                    DataType.OXYGEN_SATURATION,
//                    DataType.SKIN_TEMPERATURE_CELSIUS
                )
            )
            .build()

        try {
            passiveClient.setPassiveListenerCallback(config, callback)
        } catch (e: Exception) {
            Log.e("BioMonitor", "Error setting up listener: ${e.message}", e)
            close(e)
        }

        awaitClose {
            val future = passiveClient.clearPassiveListenerCallbackAsync()
            Futures.addCallback(future, object : FutureCallback<Void> {
                override fun onSuccess(result: Void?) {
                    Log.d("BioMonitor", "🛑 Listener cleared")
                }

                override fun onFailure(t: Throwable) {
                    Log.e("BioMonitor", "Failed to clear listener: ${t.message}", t)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    suspend fun stop() {
        passiveClient.clearPassiveListenerCallbackAsync().await()
    }
}