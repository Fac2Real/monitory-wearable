package com.iot.myapplication.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iot.myapplication.app.BioDataRepository
import com.iot.myapplication.app.FilteredBioData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class BioMonitorViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = "BioMonitorViewModel"

    // BioMonitor와 RuleBasedFilter는 여기서 만들지 않음! 서비스에서 관리함.
    // private val bioMonitor = BioMonitor(application.applicationContext)
    // private val ruleBasedFilter = RuleBasedFilter()

    // ViewModel은 BioDataRepository의 latestFilteredBioData Flow를 구독해서
    // 자신의 filteredBioDataFlow를 업데이트할 것임.
    val filteredBioDataFlow: StateFlow<FilteredBioData?> =
        BioDataRepository.latestFilteredBioData
            .stateIn( // Repository의 Flow를 ViewModel 생명주기에 맞춰 StateFlow로 변환
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // UI가 활성화되어 있을 때 구독 유지
                initialValue = BioDataRepository.latestFilteredBioData.value // 현재 Repository의 초기값 사용
            )

    // raw 데이터를 처리하는 메소드는 더 이상 필요 없음.
    // 데이터는 서비스에서 처리되어 Repository를 통해 들어옴.
    // fun processHeartRate(rawHeartRate: Float) { ... }


    init {
        Log.d(TAG, "BioMonitorViewModel initialized. Observing BioDataRepository.")
        // Repository Flow를 stateIn으로 변환했기 때문에 여기서 별도의 launch 코루틴으로 collect할 필요는 없음.
        // stateIn이 내부적으로 collect를 관리해줌.

        // ViewModel이 초기화될 때 Repository의 현재 값을 로그로 찍어볼 수 있음
        Log.d(TAG, "Current value in Repository on ViewModel init: ${BioDataRepository.latestFilteredBioData.value}")
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "BioMonitorViewModel cleared.")
        // ViewModel은 데이터를 직접 수집하지 않으므로 여기서 BioMonitor를 멈출 필요 없음.
        // BioMonitor 정지는 서비스의 onDestroy에서 처리됨.
    }
}