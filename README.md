# 모바일 배포 방법
- 수동으로 apk download 폴더로 이동후, 설치
# 웨어러블 배포 방법
1. 안드로이드 스튜디오 사용
- 개발자 모드에서 무선 디버깅 허용
- 디바이스 페어링 후 설치 진행
2. apk 사용
- ADB 사용하여 기기 페어링 후, (adb pair <deviceIP>:<paring Port>)
- adb -s <deviceIP>:<device Port> install <apk path>