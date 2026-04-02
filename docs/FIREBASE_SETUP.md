# Firebase 프로젝트 생성 가이드

## 1단계: Firebase 프로젝트 생성

1. [Firebase Console](https://console.firebase.google.com/) 접속
2. **"프로젝트 추가"** 클릭
3. 프로젝트 이름 입력: `the-100-percent-elites` (또는 원하는 이름)
4. Google Analytics는 **비활성화** 선택 (선택사항)
5. **"프로젝트 만들기"** 클릭

## 2단계: Android 앱 등록

1. 프로젝트 대시보드에서 **Android 아이콘** 클릭
2. 다음 정보 입력:
   - **Android 패키지 이름**: `com.elites.fullcharge`
   - **앱 닉네임**: `완충 전우회` (선택사항)
   - **디버그 서명 인증서 SHA-1**: (선택사항, 나중에 추가 가능)
3. **"앱 등록"** 클릭

## 3단계: google-services.json 다운로드

1. **"google-services.json 다운로드"** 버튼 클릭
2. 다운로드된 파일을 프로젝트의 `app/` 폴더로 이동:
   ```
   the-100%-elites/
   └── app/
       └── google-services.json  <-- 여기에 배치
   ```

## 4단계: Realtime Database 생성

1. 왼쪽 메뉴에서 **"빌드"** → **"Realtime Database"** 클릭
2. **"데이터베이스 만들기"** 클릭
3. 위치 선택:
   - **asia-southeast1 (싱가포르)** 권장 (한국에서 가장 가까움)
4. 보안 규칙:
   - **"테스트 모드에서 시작"** 선택
5. **"사용 설정"** 클릭

## 5단계: 데이터베이스 보안 규칙 설정

1. Realtime Database 페이지에서 **"규칙"** 탭 클릭
2. 다음 규칙으로 교체:

```json
{
  "rules": {
    "messages": {
      ".read": true,
      ".write": true,
      ".indexOn": ["timestamp"],
      "$messageId": {
        ".validate": "newData.hasChildren(['userId', 'nickname', 'message', 'timestamp'])"
      }
    },
    "online_users": {
      ".read": true,
      "$userId": {
        ".write": true,
        ".validate": "newData.hasChildren(['odhnickname', 'sessionStartTime', 'isOnline'])"
      }
    }
  }
}
```

3. **"게시"** 클릭

## 6단계: 확인

### Database URL 확인
Realtime Database 페이지 상단에서 URL 확인:
```
https://YOUR-PROJECT-ID-default-rtdb.asia-southeast1.firebasedatabase.app/
```

### google-services.json 확인
파일 내용에 다음이 포함되어 있어야 함:
```json
{
  "project_info": {
    "project_id": "your-project-id",
    "firebase_url": "https://your-project-id-default-rtdb...."
  },
  ...
}
```

## 완료!

이제 Android Studio에서 프로젝트를 빌드할 수 있습니다:
```bash
./gradlew assembleDebug
```

---

## 문제 해결

### "Firebase Database URL not found" 에러
- Realtime Database가 생성되었는지 확인
- google-services.json을 다시 다운로드

### "Permission denied" 에러
- 보안 규칙이 올바르게 설정되었는지 확인
- 규칙을 "게시"했는지 확인

### 빌드 실패
- google-services.json 파일이 `app/` 폴더에 있는지 확인
- 파일 이름이 정확히 `google-services.json`인지 확인
