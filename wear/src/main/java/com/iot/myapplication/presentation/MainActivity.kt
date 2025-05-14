/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.iot.myapplication.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.iot.myapplication.R
import com.iot.myapplication.presentation.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private fun currentProfile(intent: Intent? = this.intent): String =
        intent?.getStringExtra("profile")
            ?: getSharedPreferences("worker", MODE_PRIVATE)
                .getString("profile", getString(R.string.worker_info_default))
                ?: getString(R.string.worker_info_default)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        showScreen()
    }
    /** Listener 가 SINGLE_TOP 으로 다시 호출할 때 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showScreen(intent)
    }

    private fun showScreen(intent: Intent? = null) = setContent {
        WearApp(profileJson = currentProfile(intent))
    }
}

@Composable
fun WearApp(profileJson: String) {
    MyApplicationTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            ProfileScreen(profileJson)
        }
    }
}

@Composable
fun ProfileScreen(profileJson: String) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = profileJson
    )
}

/* 프리뷰용 기본 값 */
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp(profileJson = "No profile")
}