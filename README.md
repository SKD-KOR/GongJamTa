# 📱 StudyFocus (학습 집중 타이머 & 앱 잠금)

공부 중 스마트폰 몰입을 돕기 위해 **승인된 특정 앱만 허용**하고, **순수 집중 시간을 실시간으로 측정/기록**하는 안드로이드 애플리케이션입니다.

---

## 📌 주요 기능 (Key Features)

- **공부 시간 실시간 측정 (Stopwatch/Timer)**
  - 공부 시작/일시정지/종료 기능
  - 다른 앱을 사용하는 중에도 상단 상태바(Notification)에서 실시간으로 경과 시간 확인 가능 (Foreground Service)
- **화이트리스트 기반 앱 잠금 (App Blocker)**
  - 사용자가 사전에 허용한 앱(인터넷 강의, 사전, 학습용 앱 등)만 설정 시간 동안 실행 허용
  - 승인되지 않은 앱(SNS, 게임, 웹서핑 등) 실행 시 즉시 감지하여 차단 화면 오버레이 표시 및 홈 화면 복귀
- **앱 권한 및 목록 관리**
  - 기기에 설치된 앱 목록을 조회하여 화이트리스트 등록/해제
- **학습 기록 관리**
  - 일자별 총 학습 시간 및 허용 앱 사용 시간 로컬 저장

---

## 🛠 기술 스택 (Tech Stack)

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Architecture:** MVVM, Clean Architecture
- **Async & Reactive:** Coroutines, StateFlow
- **Key Android APIs:**
  - `Foreground Service` & `NotificationManager` (백그라운드 타이머 및 상태바 표시)
  - `UsageStatsManager` (포그라운드 앱 실행 감지)
  - `SYSTEM_ALERT_WINDOW` (차단 화면 오버레이 표시)
  - `PackageManager` (설치된 앱 정보 조회)

---

## ⚙️ 필수 권한 (Permissions)

앱의 정상 동작을 위해 아래 시스템 권한이 필요합니다.
1. **다른 앱 위에 그리기 권한 (`SYSTEM_ALERT_WINDOW`)**: 미승인 앱 실행 시 차단 화면 표출
2. **사용 정보 접근 권한 (`PACKAGE_USAGE_STATS`)**: 현재 사용자가 보고 있는 앱 감지
3. **알림 권한 (`POST_NOTIFICATIONS`)**: 백그라운드 타이머 상태 알림창 표시
