package com.iot.myapplication.app

data class BioData(
    val heartRate: Float? = null,
    val spo2: Float? = null,
    val skinTemperature: Float? = null
)

enum class BioDataStatus {
    NORMAL,
    ABNORMAL_HEART_RATE_HIGH,
    ABNORMAL_HEART_RATE_LOW,
    UNKNOWN
}

data class FilteredBioData(
    val originalData: BioData,
    val status: BioDataStatus,
    val reason: String? = null // 비정상일 경우 이유
)

class RuleBasedFilter {

    // 예시: 심박수 정상 범위 (실제로는 더 정교한 규칙 필요)
    private val NORMAL_HEART_RATE_MIN = 60f
    private val NORMAL_HEART_RATE_MAX = 140f

    fun process(bioData: BioData): FilteredBioData {
        var status = BioDataStatus.UNKNOWN
        var reason: String? = null

        bioData.heartRate?.let { hr ->
            status = if (hr < NORMAL_HEART_RATE_MIN) {
                reason = "심박수가 정상 범위보다 낮습니다 (측정: ${hr}bpm, 정상 최저: ${NORMAL_HEART_RATE_MIN}bpm)."
                BioDataStatus.ABNORMAL_HEART_RATE_LOW
            } else if (hr > NORMAL_HEART_RATE_MAX) {
                reason = "심박수가 정상 범위보다 높습니다 (측정: ${hr}bpm, 정상 최고: ${NORMAL_HEART_RATE_MAX}bpm)."
                BioDataStatus.ABNORMAL_HEART_RATE_HIGH
            } else {
                BioDataStatus.NORMAL
            }
        }

        // 여기에 SpO2, 체온 등 다른 데이터에 대한 규칙 추가
        // 예:
        // bioData.spo2?.let { spo2 ->
        // if (spo2 < NORMAL_SPO2_MIN) {
        // // 비정상 처리
        // }
        // }

        return FilteredBioData(bioData, status, reason)
    }
}