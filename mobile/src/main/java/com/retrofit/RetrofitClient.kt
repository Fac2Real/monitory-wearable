package com.retrofit

import okhttp3.ConnectionSpec
// import com.your_app_package.BuildConfig // 앱의 BuildConfig를 사용한다고 가정
import okhttp3.OkHttpClient
// import okhttp3.TlsVersion // MODERN_TLS 사용 시 직접 명시 필요 없을 수 있음
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import software.amazon.awssdk.crt.BuildConfig

object RetrofitClient {
    // 실제 운영 시에는 HTTPS URL 사용
//    private const val BASE_URL = "https://www.monitory.space/"
    // 개발 시 로컬 HTTP 서버 테스트용 URL (주석 처리하여 선택)
     private const val BASE_URL = "http://10.0.2.2:8080/" // 에뮬레이터에서 호스트 PC의 localhost

    // 앱의 BuildConfig.DEBUG를 사용한다고 가정 (import 문 확인 필요)
    // 만약 이 RetrofitClient가 라이브러리 모듈이라면, 초기화 시점에 디버그 여부를 전달받는 것이 좋음
    private val IS_DEBUG = BuildConfig.DEBUG // 실제 앱의 BuildConfig 경로로 수정

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (IS_DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder().apply {
        addInterceptor(loggingInterceptor)

        if (BASE_URL.startsWith("https://")) {
            // HTTPS 전용: MODERN_TLS는 안전한 최신 프로토콜을 사용하도록 권장
            val httpsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build()
            connectionSpecs(listOf(httpsSpec))
        } else {
            // HTTP 허용 (개발용):
            // 로컬 HTTP 테스트를 위해 CLEARTEXT를 허용합니다.
            // Android 9 (API 28) 이상에서는 network_security_config.xml에
            // 해당 도메인(10.0.2.2)에 대한 cleartextTrafficPermitted="true" 설정이 필요합니다.
            val httpSpec = ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT).build()
            // HTTPS도 시도할 수 있게 하려면 MODERN_TLS도 추가할 수 있으나,
            // 보통 개발용 로컬 서버는 HTTP만 사용하므로 CLEARTEXT만으로도 충분할 수 있습니다.
            // val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build()
            // connectionSpecs(listOf(modernTlsSpec, httpSpec))
            connectionSpecs(listOf(httpSpec))
        }
    }.build()

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}