package com.iot.myapplication.app

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BioDataRepository {
    private const val TAG = "BioDataRepository"

    // ViewModel과 UI가 관찰할 필터링된 생체 데이터의 StateFlow
    // 초기값은 null
    private val _latestFilteredBioData = MutableStateFlow<FilteredBioData?>(null)
    val latestFilteredBioData: StateFlow<FilteredBioData?> = _latestFilteredBioData.asStateFlow()

    // 서비스에서 호출하여 최신 필터링된 데이터를 업데이트하는 함수
    fun updateLatestFilteredBioData(data: FilteredBioData) {
        Log.d(TAG, "Updating latest filtered bio data: ${data.status}")
        _latestFilteredBioData.value = data
    }

    // 필요하다면 raw 데이터나 다른 상태를 위한 Flow도 추가할 수 있음
}