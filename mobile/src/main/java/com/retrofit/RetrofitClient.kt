package com.retrofit

import okhttp3.ConnectionSpec
import software.amazon.awssdk.crt.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Arrays

object RetrofitClient {
//    private const val BASE_URL = "https://www.monitory.space/"
    private const val BASE_URL = "http://10.0.2.2:8080"
    private val loggingInterceptor = HttpLoggingInterceptor().apply{
        level = if(BuildConfig.DEBUG){
            HttpLoggingInterceptor.Level.BODY
        }else{
            HttpLoggingInterceptor.Level.NONE
        }
    }
    val spec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_2)
        .tlsVersions(TlsVersion.TLS_1_1)
        .tlsVersions(TlsVersion.TLS_1_0)
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectionSpecs(Arrays.asList(spec,ConnectionSpec.CLEARTEXT))
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: ApiService by lazy{
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}