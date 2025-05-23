package com.retrofit
import com.iot.myapplication.Location
import retrofit2.Response // Response 래퍼를 사용하여 HTTP 상태 코드 등을 함께 받을 수 있음
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.time.LocalDateTime

interface ApiService {
    @GET("/api/zones")
    suspend fun getAvailableLocations():
            Response<List<Location>>

    @POST("/api/worker-locations/update")
    suspend fun postWorkerLocation(
        @Body request: WorkerLocationRequest
    ): Response<Unit>
}