package com.iot.myapplication

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel // ViewModel 인스턴스 변수 선언

    // UI 요소 변수 선언
    private lateinit var workerInfoTextView: TextView
    private lateinit var mqttStatusTextView: TextView
    private lateinit var locationButtonsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2) // 위에서 정의한 레이아웃 사용

        // UI 요소 초기화 (레이아웃 ID에 맞게 수정!)
        workerInfoTextView = findViewById(R.id.workerInfoTextView)
        mqttStatusTextView = findViewById(R.id.mqttStatusTextView)
        locationButtonsContainer = findViewById(R.id.locationButtonsContainer)

        // ViewModelProvider를 사용해서 ViewModel 인스턴스 가져오기! ⭐ 여기가 중요 ⭐
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        // ViewModel의 LiveData 관찰 (Observe) 시작!

        // 1. 사용 가능한 위치 목록 (availableLocations) 관찰
        viewModel.availableLocations.observe(this, Observer { locations ->
            // 위치 목록이 업데이트 될 때마다 이 블록이 실행돼!
            // 받아온 locations 리스트를 가지고 버튼을 동적으로 만들어서 레이아웃에 추가해 줄 거야.
            locationButtonsContainer.removeAllViews() // 기존 버튼 제거 (새로 갱신하기 위함)
            locations.forEach { location ->
                val button = Button(this).apply {
                    text = location.zoneName // 버튼 텍스트는 위치 이름
                    layoutParams = LinearLayout.LayoutParams( // 버튼 레이아웃 설정
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        // 버튼 간의 간격 설정 (dp 값을 px로 변환해야 함)
                        // 예: resources.getDimensionPixelSize(R.dimen.button_margin_right) 사용
                        marginEnd = resources.getDimensionPixelSize(R.dimen.button_spacing) // dimens.xml에 button_spacing 정의 필요
                    }
                    // 버튼 클릭 리스너 설정!
                    setOnClickListener {
                        // 버튼이 눌리면 ViewModel의 publishWorkerLocation 함수를 호출하고,
                        // 이 버튼과 연결된 Location 객체를 인자로 넘겨줘!
                        viewModel.postWorkerLocation(location)
                    }
                }
                locationButtonsContainer.addView(button) // 컨테이너에 버튼 추가!
            }
        })

        // 2. 작업자 정보 및 보고 위치 텍스트 (workerPayloadText) 관찰
        viewModel.workerPayloadText.observe(this, Observer { text ->
            // ViewModel에서 작업자 정보/위치 텍스트가 업데이트될 때마다 이 TextView를 갱신해 줘.
            workerInfoTextView.text = text
        })

        // 3. MQTT 발행 상태 텍스트 (mqttPublishStatus) 관찰
        viewModel.mqttPublishStatus.observe(this, Observer { status ->
            // ViewModel에서 MQTT 상태 메시지가 업데이트될 때마다 이 TextView를 갱신해 줘.
            mqttStatusTextView.text = status
        })

        // ViewModel의 서비스 초기화 및 시작 함수 호출 (필요하다면)
        // viewModel.initializeAndStartService() // 만약 Activity 시작 시 서비스도 시작해야 한다면 호출

        // 다른 초기화나 UI 설정...
    }

    // Activity가 파괴될 때 ViewModel의 onCleared()가 자동으로 호출되어 정리됨.
    // 따로 onDestroy에서 ViewModel 정리 코드를 호출할 필요는 없어!
}