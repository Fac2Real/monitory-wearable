# Monitory Wearable
공장 통합 모니터링 시스템 **Monitory**의 작업자 안전 모니터링을 위한 웨어러블 기반 생체 데이터 수집 시스템입니다.

---

## 🏭 레포지토리 소개
Monitory Wearable은 공장 작업자의 실시간 생체 데이터(심박수 등)를 수집하여 안전 상태를 모니터링하는 시스템입니다.<br>
Wear OS 기반 스마트워치에서 수집된 생체 데이터를 모바일 앱을 통해 AWS IoT Core로 전송하며, 이상 상황 발생 시 실시간 알림을 제공합니다.

- **실시간 생체 모니터링**: Wear OS Health Services를 통한 심박수 실시간 수집
- **모바일-웨어러블 연동**: Google Wearable Data Layer API를 통한 데이터 동기화
- **작업자 위치 추적**: 모바일 앱을 통한 작업 구역 체크인/체크아웃
- **이상 상황 감지**: 규칙 기반 생체 데이터 필터링 및 위험 상태 판단
- **실시간 알림**: FCM 기반 위험 상황 알림 및 웨어러블 진동 알림
- **MQTT 데이터 전송**: AWS IoT Core를 통한 실시간 생체 데이터 전송

---

## 📁 폴더 구조

```
monitory-wearable/
├── mobile/                           # 안드로이드 모바일 앱
│   ├── src/main/
│   │   ├── java/com/
│   │   │   ├── iot/myapplication/    # 메인 앱 로직
│   │   │   │   ├── FCMService.kt     # FCM 알림 서비스
│   │   │   │   ├── MainActivity2.kt  # 메인 액티비티
│   │   │   │   ├── MainViewModel.kt  # ViewModel 로직
│   │   │   │   └── Location.kt       # 위치 데이터 모델
│   │   │   ├── f2r/mobile/worker/    # 작업자 관련 서비스
│   │   │   │   ├── WearableListener.kt    # 웨어러블 데이터 수신
│   │   │   │   ├── WorkerInfo.kt          # 작업자 정보 관리
│   │   │   │   └── WorkerInfoSender.kt    # 작업자 정보 전송
│   │   │   ├── mqtt/                 # MQTT 통신 관리
│   │   │   │   ├── AwsIotClientManager.kt # AWS IoT Core 연결
│   │   │   │   └── MyLifeCycleEvent.kt    # MQTT 생명주기 관리
│   │   │   └── retrofit/             # REST API 통신
│   │   │       ├── RetrofitClient.kt      # HTTP 클라이언트
│   │   │       ├── ApiService.kt          # API 인터페이스
│   │   │       └── WorkerLocationRequest.kt # 위치 요청 모델
│   │   ├── assets/                   # AWS IoT 인증서
│   │   │   ├── certificate.pem.crt   # AWS IoT 디바이스 인증서
│   │   │   ├── private.pem.key       # AWS IoT 개인키
│   │   │   └── AmazonRootCA1.pem     # AWS 루트 CA
│   │   └── res/                      # 리소스 파일
│   └── build.gradle.kts              # 모바일 앱 빌드 설정
├── wear/                             # Wear OS 웨어러블 앱
│   ├── src/main/
│   │   ├── java/com/
│   │   │   ├── iot/myapplication/    # 웨어러블 메인 로직
│   │   │   │   ├── MainActivity.kt   # 웨어러블 메인 액티비티
│   │   │   │   └── theme/            # UI 테마 설정
│   │   │   └── f2r/wearable/         # 웨어러블 전용 서비스
│   │   │       ├── HeartRateService.kt    # 심박수 측정 서비스
│   │   │       ├── DataSender.kt          # 모바일로 데이터 전송
│   │   │       └── WearableUtils.kt       # 웨어러블 유틸리티
│   │   └── res/                      # 웨어러블 리소스 파일
│   └── build.gradle.kts              # 웨어러블 앱 빌드 설정
├── gradle/                           # Gradle 설정
│   └── wrapper/
├── build.gradle.kts                  # 프로젝트 빌드 설정
└── settings.gradle.kts               # 프로젝트 설정
```

---

## 🚀 주요 기능

### 1. 웨어러블 생체 데이터 수집
- **심박수 모니터링**: Wear OS Health Services API를 통한 실시간 심박수 측정
- **연속 모니터링**: 백그라운드에서 지속적인 생체 데이터 수집
- **데이터 필터링**: 노이즈 제거 및 유효 데이터 검증
- **배터리 최적화**: 효율적인 센서 사용으로 배터리 수명 최대화

### 2. 모바일-웨어러블 연동
- **Data Layer API**: Google Wearable Data Layer를 통한 실시간 데이터 동기화
- **자동 재연결**: 연결 끊김 시 자동 재연결 메커니즘
- **데이터 큐잉**: 연결 불안정 시 데이터 임시 저장 및 배치 전송
- **상태 동기화**: 웨어러블과 모바일 간 앱 상태 실시간 동기화

### 3. 작업자 위치 관리
- **체크인/체크아웃**: 작업 구역별 출입 관리
- **위치 기반 모니터링**: GPS 기반 작업 구역 자동 감지
- **이동 경로 추적**: 작업자 동선 및 체류 시간 기록
- **구역별 안전 설정**: 구역별 위험도에 따른 모니터링 강도 조절

### 4. 이상 상황 감지 및 알림
- **규칙 기반 필터링**: 심박수 임계값 기반 위험 상황 판단
- **FCM 푸시 알림**: 관리자 및 작업자에게 즉시 알림 전송
- **웨어러블 진동 알림**: 스마트워치 진동을 통한 즉각적 경고
- **단계별 알림**: 주의/경고/위험 단계별 차등 알림

---

## 🛠️ 기술 스택

### 모바일 앱 (Android)
- **Kotlin**: 메인 개발 언어
- **Retrofit**: REST API 통신
- **AWS IoT SDK**: MQTT 프로토콜 기반 AWS IoT Core 연결
- **Google Play Services**: Wearable Data Layer API
- **Firebase FCM**: 푸시 알림 서비스

### 웨어러블 앱 (Wear OS)
- **Kotlin**: 메인 개발 언어
- **Wear OS**: Google 웨어러블 플랫폼
- **Health Services API**: 생체 데이터 수집
- **Compose for Wear OS**: UI 프레임워크
- **Wearable Data Layer**: 모바일과 데이터 동기화

---

## 📡 MQTT 토픽 구조

### 작업자 생체 데이터
```
worker/biometric/{workerId}/heartrate
```

### 작업자 위치 데이터
```
worker/location/{workerId}/checkin
worker/location/{workerId}/checkout
```

### 위험 상황 알림
```
alert/worker/{workerId}/emergency
alert/worker/{workerId}/warning
```

---

## 📝 데이터 포맷

### 심박수 데이터
```json
{
    "workerId": "WORKER-001",
    "timestamp": "2025-06-16T10:30:00Z",
    "heartRate": 85,
    "status": "normal",
    "zoneId": "ZONE-001",
    "deviceId": "WEAR-001"
}
```

### 위치 체크인 데이터
```json
{
    "workerId": "WORKER-001",
    "zoneId": "ZONE-001",
    "action": "checkin",
    "timestamp": "2025-06-16T09:00:00Z",
    "location": {
        "latitude": 37.5665,
        "longitude": 126.9780
    }
}
```

### 위험 상황 알림 데이터
```json
{
    "workerId": "WORKER-001",
    "alertType": "high_heartrate",
    "severity": "warning",
    "heartRate": 150,
    "normalRange": "60-100",
    "timestamp": "2025-06-16T14:15:00Z",
    "zoneId": "ZONE-001"
}
```

---

## 🔄 시스템 작동 흐름

1. **웨어러블 데이터 수집**: 스마트워치에서 실시간 심박수 측정
2. **데이터 전송**: Wearable Data Layer를 통해 모바일 앱으로 전송
3. **데이터 처리**: 모바일 앱에서 데이터 검증 및 필터링
4. **위험 상황 판단**: 규칙 기반으로 이상 상황 감지
5. **MQTT 전송**: AWS IoT Core로 생체 데이터 및 알림 전송
6. **알림 발송**: FCM을 통한 관리자 알림 및 웨어러블 진동 알림

---

## ⚙️ 설정 및 실행

### 사전 요구사항
- Android Studio Arctic Fox 이상
- Android SDK 30 이상
- Wear OS SDK
- AWS IoT Core 계정 및 디바이스 인증서
- Firebase 프로젝트 설정

### AWS IoT 인증서 설정
1. AWS IoT Core에서 디바이스 인증서 생성
2. `mobile/src/main/assets/` 디렉토리에 인증서 파일 배치:
   - `certificate.pem.crt`: 디바이스 인증서
   - `private.pem.key`: 개인키
   - `AmazonRootCA1.pem`: AWS 루트 CA

### Firebase 설정
1. Firebase 콘솔에서 프로젝트 생성
2. FCM 활성화 및 서버 키 생성
3. `google-services.json` 파일을 앱 모듈에 추가

### 빌드 및 실행
```bash
# 프로젝트 클론
git clone [repository-url]

# Android Studio에서 프로젝트 열기
# 모바일 앱과 웨어러블 앱을 각각 빌드 및 설치
```

---

## 🚨 주의사항

### 보안
- AWS IoT 인증서는 안전하게 관리하고 버전 관리에 포함하지 않음
- Firebase 서버 키 및 구성 파일 보안 관리
- 생체 데이터의 개인정보 보호 정책 준수

### 성능
- 웨어러블 배터리 수명을 위한 센서 사용 최적화
- 대량 데이터 전송 시 배터리 소모 및 네트워크 비용 고려
- 메모리 누수 방지를 위한 리소스 관리

### 호환성
- Wear OS 2.0 이상 호환 스마트워치 필요
- Android 7.0 (API 24) 이상 모바일 디바이스 필요
- 블루투스 연결 안정성 확인

---

## 📊 현재 구현된 모니터링 규칙

| 생체 지표 | 정상 범위 | 비정상 (낮음) | 비정상 (높음) |
|-----------|-----------|---------------|---------------|
| 심박수 (BPM) | 60-140 | 60 미만 | 140 초과 |

### 알림 규칙
- **정상**: 60-140 BPM 범위 내
- **비정상 (낮음)**: 60 BPM 미만 시 "심박수가 정상 범위보다 낮습니다" 알림
- **비정상 (높음)**: 140 BPM 초과 시 "심박수가 정상 범위보다 높습니다" 알림
---

## 🔗 관련 시스템 연동
- **Monitory 웹 대시보드**: 실시간 작업자 상태 모니터링
- **Monitory 센서 시스템**: 환경 센서와 작업자 생체 데이터 통합 분석
- **응급 대응 시스템**: 위험 상황 발생 시 자동 응급 프로토콜 실행

---

# 모바일 배포 방법
- 수동으로 apk download 폴더로 이동후, 설치
# 웨어러블 배포 방법
1. 안드로이드 스튜디오 사용
- 개발자 모드에서 무선 디버깅 허용
- 디바이스 페어링 후 설치 진행
2. apk 사용
- ADB 사용하여 기기 페어링 후, (adb pair <deviceIP>:<paring Port>)
- adb -s <deviceIP>:<device Port> install <apk path>
