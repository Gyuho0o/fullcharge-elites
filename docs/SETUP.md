# 완충 전우회 (The 100% Elites) - 설정 가이드

## 프로젝트 개요

배터리가 100%이고 충전 중인 사용자만 입장할 수 있는 엘리트 채팅 앱입니다.

## 요구 사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Firebase 프로젝트

## 설정 방법

### 1. Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/)에서 새 프로젝트 생성
2. Android 앱 추가 (패키지명: `com.elites.fullcharge`)
3. `google-services.json` 파일 다운로드
4. `app/` 폴더에 `google-services.json` 파일 배치

### 2. Firebase Realtime Database 설정

Firebase Console에서 Realtime Database를 생성하고 다음 보안 규칙을 적용:

```json
{
  "rules": {
    "messages": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp"]
    },
    "online_users": {
      ".read": true,
      ".write": true,
      "$userId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```

> ⚠️ **주의**: 위 규칙은 개발용입니다. 프로덕션에서는 인증을 추가하세요.

### 3. 빌드 및 실행

```bash
# 프로젝트 디렉토리로 이동
cd the-100%-elites

# Gradle 빌드
./gradlew assembleDebug

# 또는 Android Studio에서 직접 실행
```

### 4. 사운드 파일 (선택사항)

`app/src/main/res/raw/` 폴더에 다음 파일 추가:
- `entry_fanfare.mp3` - 입장 시 재생되는 팡파르
- `exile_buzz.mp3` - 추방 시 재생되는 효과음

## 프로젝트 구조

```
app/src/main/java/com/elites/fullcharge/
├── ElitesApplication.kt      # Application 클래스
├── MainActivity.kt           # 메인 액티비티
├── data/
│   ├── BatteryStatus.kt      # 배터리 상태 관리
│   ├── ChatMessage.kt        # 채팅 메시지 모델
│   ├── ChatRepository.kt     # Firebase 채팅 저장소
│   ├── ElitePreferences.kt   # DataStore 환경설정
│   └── EliteRank.kt          # 계급 체계
├── receiver/
│   ├── BatteryReceiver.kt    # 배터리 상태 리시버
│   └── BootReceiver.kt       # 부팅 완료 리시버
├── service/
│   └── BatteryMonitorService.kt  # 백그라운드 모니터링 서비스
└── ui/
    ├── MainViewModel.kt      # 메인 ViewModel
    ├── components/
    │   ├── BatteryIndicator.kt   # 배터리 표시 컴포넌트
    │   └── GoldParticles.kt      # 금가루 애니메이션
    ├── screens/
    │   ├── GatekeeperScreen.kt   # 검문소 화면
    │   ├── ChatScreen.kt         # 채팅 화면
    │   └── ExileScreen.kt        # 추방 화면
    └── theme/
        ├── Color.kt          # 색상 정의
        ├── Theme.kt          # 테마 설정
        └── Type.kt           # 타이포그래피
```

## 핵심 기능

### 엘리트 자격 검문소
- 배터리 100% + 충전 중인 경우만 입장 가능
- 실시간 배터리 상태 모니터링

### 실시간 채팅
- Firebase Realtime Database 기반
- 익명 채팅 (랜덤 닉네임 생성)
- 휘발성 메시지 (최근 50개만 유지)

### 추방 시스템
- 충전기 분리 즉시 추방
- 배터리 99% 이하로 떨어지면 추방
- Foreground Service로 백그라운드에서도 감지

### 계급 체계
- 신병 (0~10분)
- 일등병 (10~60분)
- 말년병장 (1~24시간)
- 완충의 신 (24시간 이상)

## 문제 해결

### Firebase 연결 실패
- `google-services.json` 파일 확인
- Firebase 프로젝트 설정 확인
- 인터넷 연결 확인

### 배터리 감지 안됨
- 배터리 최적화 예외 설정 확인
- 앱 권한 확인

### 알림이 표시되지 않음
- Android 13 이상: 알림 권한 허용 필요
- 알림 채널 설정 확인
