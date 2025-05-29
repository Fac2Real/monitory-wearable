//package com.iot.myapplication
//
//import android.annotation.SuppressLint
//import android.content.Intent
//import android.os.Bundle
//import android.widget.TextView
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import com.f2r.mobile.worker.WorkerInfo
//import com.f2r.mobile.worker.WorkerInfoSender
//import com.mqtt.AwsIotClientManager
//import org.json.JSONObject
//
//class MainActivity : AppCompatActivity() {
//    private val viewModel: MainViewModel by viewModels()
//
//    private lateinit var workerPayloadTextView: TextView
//
//    @SuppressLint("SetTextI18n")
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_main)
//        workerPayloadTextView = findViewById<TextView>(R.id.worker_payload)
//        viewModel.workerPayloadText.observe(this) {
//            text -> workerPayloadTextView.text = text
//        }
//
//        // ② 서비스 기동 (extra 필요 X — Sender 내부에서 자체 생성)
////        startService(Intent(this, WorkerInfoSender::class.java))                          // ↓ 서비스가 전송
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//    }
//}