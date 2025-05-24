package com.retrofit
import com.google.gson.annotations.SerializedName
import com.iot.myapplication.Location
import retrofit2.Response // Response 래퍼를 사용하여 HTTP 상태 코드 등을 함께 받을 수 있음
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.LocalDateTime
// API 응답 전체를 감싸는 데이터 클래스 (위에 정의한 것과 동일)
data class ApiResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("status")
    val status: Int,
    @SerializedName("data")
    val data: List<Location>, // Location 객체 리스트
    @SerializedName("timestamp")
    val timestamp: String
)
interface ApiService {
    @GET("/api/zones")
    suspend fun getAvailableLocations():
            Response<ApiResponse>

    @POST("/api/zone-history/update")
    suspend fun postWorkerLocation(
        @Body request: WorkerLocationRequest
    ): Response<Unit>

    @POST("/api/fcm")
    suspend fun sendFCM(
        @Body request: FCMTokenRegistDto
    ): Response<Unit>
}