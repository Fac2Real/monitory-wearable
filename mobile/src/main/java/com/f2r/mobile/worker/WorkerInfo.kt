package com.f2r.mobile.worker

import org.json.JSONObject

object WorkerInfo {
    /** 하드코딩 정보 → JSON 문자열 반환 */
    fun toJson(): JSONObject = JSONObject().apply {
        put("workerId", "20220001")
        put("name",     "정민석")
        put("phone",    "+821012345678")
        put("role",     "용접공")
        put("bloodType","O")
    }
}