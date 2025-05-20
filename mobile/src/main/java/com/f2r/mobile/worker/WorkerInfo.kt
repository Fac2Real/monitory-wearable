package com.f2r.mobile.worker

import org.json.JSONObject

object WorkerInfo {
    /** 하드코딩 정보 → JSON 문자열 반환 */
    fun toJson(): String = JSONObject().apply {
        put("workerId", "W-2025-0002")
        put("name",     "정민석")
        put("phone",    "+821012345678")
        put("role",     "용접공")
        put("bloodType","O")
    }.toString()
}