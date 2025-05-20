/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.iot.myapplication.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.iot.myapplication.app.MainAppController

class MainActivity : ComponentActivity() {
    private lateinit var controller: MainAppController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        controller = MainAppController(this, intent)
        controller.init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        controller.updateIntent(intent)
    }
}