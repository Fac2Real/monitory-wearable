/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.iot.myapplication.presentation


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.f2r.wear.worker.WearableIdSender
import com.iot.myapplication.app.MainAppController

private const val TAG = "MainActivityWear" // 로깅을 위한 태그
class MainActivity : ComponentActivity() {
    private lateinit var controller: MainAppController
    @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)


        controller = MainAppController(this, intent)
        controller.init()
        startWearableIdSenderService()
    }

    private fun startWearableIdSenderService() {
        val intent = Intent(this, WearableIdSender::class.java)
        startService(intent)
    }
    // @RequiresPermission("android.permission.READ_PRIVILEGED_PHONE_STATE") // 더 이상 이 어노테이션 필요 없음


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        controller.updateIntent(intent)
    }
}