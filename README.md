# 📱 GongJamTa (공잠타 - 학습 집중 타이머 & 앱 잠금)

스마트폰의 유혹을 잠그고 오롯이 공부에 몰입할 수 있도록 돕는 안드로이드 집중 보조 애플리케이션입니다.  
**화이트리스트 기반 차단 오버레이**를 통해 필요한 앱만 허용하고, **순수 집중 시간을 측정하여 로컬 DB에 자동 기록**합니다.

---

## 📌 주요 기능 (Key Features)

- **공부 시간 실시간 측정 (Timer & Stopwatch)**
  - 공부 시작 / 일시정지 / 종료 기능
  - 다른 허용 앱 사용 중에도 백그라운드에서 끊김 없이 측정 및 알림바(Notification) 표시 (`Foreground Service`)
- **화이트리스트 기반 앱 차단 (App Blocker & Overlay)**
  - 사용자가 사전에 등록한 앱(인강, 전자사전, 메모 등)만 집중 시간 동안 실행 허용
  - 미승인 앱(SNS, 유튜브, 게임 등) 실행 즉시 감지하여 차단 화면 오버레이 표시 및 홈 화면 전환 유도
- **설치 앱 목록 및 허용 앱 관리**
  - 기기에 설치된 앱 목록을 조회하여 원터치로 허용 앱 등록 및 해제
- **로컬 학습 통계 및 기록 저장 (Room Database)**
  - 타이머 종료 시 공부 세션(시작/종료 시간, 순수 집중 시간, 날짜) 자동 저장
  - 기록 탭을 통해 일자별 집중 내역 확인

---

## 🛠 기술 스택 (Tech Stack)

- **Language:** Kotlin 2.x
- **UI:** Jetpack Compose, Material 3
- **Local Database:** Room, KSP (Kotlin Symbol Processing)
- **Dependency Management:** Gradle Version Catalog (`libs.versions.toml`)
- **Async & Reactive:** Coroutines, StateFlow
- **Key Android Components:**
  - `Foreground Service` & `NotificationManager` (백그라운드 타이머 및 상태 유지)
  - `UsageStatsManager` (실행 중인 포그라운드 앱 감지)
  - `WindowManager` & `SYSTEM_ALERT_WINDOW` (비허용 앱 진입 시 커스텀 차단 오버레이 뷰 표출)
  - `PackageManager` (기기 설치 앱 목록 조회)

---

## ⚙️ 필수 권한 (Permissions)

앱의 정상 동작을 위해 최초 실행 시 아래 시스템 권한 허용이 필요합니다.
1. **다른 앱 위에 표시 (`SYSTEM_ALERT_WINDOW`)**: 미승인 앱 실행 시 차단 화면 표출
2. **사용 정보 접근 허용 (`PACKAGE_USAGE_STATS`)**: 현재 실행 중인 앱 실시간 감지
3. **알림 권한 (`POST_NOTIFICATIONS`)**: 백그라운드 타이머 작동 상태를 상단 상태바에 유지

---

## 📥 다운로드 및 설치 (Installation)

1. [Releases 페이지](../../releases)로 이동합니다.
2. 최신 버전의 `GongJamTa-v1.0.0.apk` 파일을 다운로드합니다.
3. 파일 실행 후 기기 안내에 따라 **'출처를 알 수 없는 앱 설치'**를 허용해 설치를 완료합니다.
4. 앱 실행 후 안내되는 2가지 핵심 권한(다른 앱 위에 표시, 사용 정보 접근)을 반드시 켜주세요.
