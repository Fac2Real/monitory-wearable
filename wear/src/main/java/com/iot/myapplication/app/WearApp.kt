package com.iot.myapplication.app

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.iot.myapplication.presentation.BioMonitorViewModel
import com.iot.myapplication.presentation.theme.MyApplicationTheme

@Composable
fun WearApp(profileJson: String, bioMonitorViewModel: BioMonitorViewModel) {
    MyApplicationTheme {
        Scaffold(timeText = { TimeText() } ){
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
//                TimeText()
                    // 프로필 정보
                    ProfileScreen(profileJson)
                    BioDataScreen(bioViewModel = bioMonitorViewModel)
                }
            }

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

@Composable
fun BioDataScreen(bioViewModel: BioMonitorViewModel) {
    val filteredBioDataState by bioViewModel.filteredBioDataFlow.collectAsState()
    Log.d("BioDataScreen", "filteredBioDataState: $filteredBioDataState")
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bio Data Monitor",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.primary
        )

        filteredBioDataState?.let { filteredData ->
            Text(
                text = "HR: ${filteredData.originalData.heartRate?.toString() ?: "N/A"} BPM",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.primary
            )
            if (filteredData.status != BioDataStatus.NORMAL && filteredData.status != BioDataStatus.UNKNOWN && filteredData.reason != null) {
                Text(
                    text = "사유: ${filteredData.reason}"
                )
            }
        } ?: run {
            Text(
                text = "데이터 수신 중..."
            )
        }
    }
}
