# AutoLog Frontend 설계 스펙

| 항목 | 내용 |
|---|---|
| 문서 목적 | AutoLog의 **모든 화면**(Pleos 차량 앱, 운영 웹, 폰 카메라 페이지)을 이 리포에서 어떻게 만들지 확정한다. 이 문서만 읽고도 다른 사람이 같은 결과물을 만들 수 있어야 한다 |
| 상태 | 확정 v1.0 (2026-11-09) |
| 함께 읽을 문서 | [`docs/design-language.md`](../../design-language.md)(디자인 철학·토큰·모션·컴포넌트) · [`CONVENTIONS.md`](../../../CONVENTIONS.md)(브랜치·커밋·코드 규칙) |
| 규범 용어 | **반드시** = 어기면 리뷰에서 반려 · **하지 않는다** = 금지 · **권장** = 이유가 있으면 벗어나도 됨(PR에 이유를 적음) · **[검증 필요]** = 근거가 아직 없음, 13장 목록에서 추적 |

---

## 0. 이 문서를 읽는 순서

1. 1~3장으로 **무엇을, 왜** 만드는지 파악한다. 특히 **1.1 사용 모델(주행·정차·주차)과 1.2 조작 최소화**는 모든 화면 설계의 전제다.
2. 4장 결정 표에서 **이미 정해진 것과 버린 대안**을 확인한다. 여기 적힌 결정을 바꾸려면 이 문서를 먼저 고치는 PR을 올린다.
3. 작업할 영역의 장(5 빌드 · 6 앱 구조 · 7 화면 · 8 사내 서버 · 9 웹)을 읽는다.
4. 화면을 그리기 전에 반드시 `docs/design-language.md`를 읽는다.
5. PR 전에 10장 품질 게이트를 통과시킨다.

---

## 1. 목적과 범위

AutoLog는 "운전만 하세요. 기록은 차가 합니다."를 약속하는 법인차량 운행·지출 장부 앱이다(2026 현대자동차그룹 차량용 앱 개발 공모전, Pleos App Market). 운행 기록의 원본은 차량에 있고, 차가 계기판(Odometer)을 직접 읽어 국세청 양식 운행기록부를 만든다.

**이 리포(AutoLog-Frontend)가 만드는 것**

| 산출물 | 경로 | 실행 위치 |
|---|---|---|
| Pleos 차량 앱 (화면 + ViewModel + Repository) | `vehicle-app/` | Pleos Connect 에뮬레이터 · 헤드유닛 |
| 운영 웹 (관리자) | `web/admin/` | nginx 정적 (`/admin/`) |
| 폰 영수증 카메라 페이지 | `web/camera/index.html` | nginx 정적 (`/r/{code}`) |

**이 리포가 만들지 않는 것** — 차량 로직(SDK 게이트웨이, 운행 세션, Room, 엑셀, 메일)과 중계 서버(relay)는 [AutoLog-Backend](https://github.com/hmg-app-challenge-2026/AutoLog-Backend)가 만든다. 이 리포는 Backend를 git submodule(`backend/`)로 가져와 `vehicle-core` 모듈의 공개 계약(`core/Contracts.kt`)만 쓴다.

### 1.1 사용 모델 — 주행·정차·주차 (가장 중요한 전제)

AutoLog는 **운전하는 동안 쓰는 앱이 아니다.** 기록은 차가 백그라운드에서 하고, 사람은 차가 멈췄을 때만 본다. 차량 상태에 따라 앱이 허용하는 범위를 세 단계(`UsageTier`)로 고정한다.

| 차량 상태 | 판정 | `UsageTier` | 앱이 하는 일 | 사람이 할 수 있는 일 |
|---|---|---|---|---|
| **주행** (움직이는 중) | 거리 증가 중, 또는 판정 불가 | `BACKGROUND` | 화면을 띄우지 않는다. 앱 창이 보이고 있었다면 스스로 뒤로 물러난다(`moveTaskToBack`). 기록은 앱 A의 Foreground Service가 계속하고, 상태는 지속 알림으로만 보인다 | 없음 |
| **정차** (기어 P가 아니고 멈춤 — 신호 대기 등) | 기어 ≠ P, 거리 불변 | `PEEK` | 앱을 열면 **Peek 패널**만 보여 준다: 지금 어떻게 기록되고 있는지(목적지, 이번 운행 거리, 운전자, 거리 출처, 마지막 저장 시각) | **열어서 보는 것까지.** 버튼·입력 없음. 다시 움직이면 자동으로 뒤로 물러난다 |
| **주차** (기어 P) | 기어 = P (기어 신호가 없으면 1.1.1 최후 규칙) | `FULL` | 전체 앱: 운전자 선택, 목적 확인, 영수증, 운행기록부, 이력, 설정 | 전부 |

- 판정이 애매하면 **더 제한적인 단계**로 간다(판정 불가 → `BACKGROUND`, P 여부 불명 → `PEEK`).

#### 1.1.1 기어·시동 판정 — 신호 우선순위 (사용자 결정 2026-11-09)

판정은 차량 데이터라서 **앱 A(DrivingJudge)** 가 하고, 화면은 결과(`motion`)만 받는다. 앱 A는 아래 순서로 쓸 수 있는 첫 신호를 쓴다.

| 순위 | 신호 | 얻는 것 | 근거 |
|---|---|---|---|
| 1 | Pleos Vehicle SDK **Gear** (`getCurrentGearSelectionState`·`registerGearSelection`, 권한 `pleos.car.permission.CAR_DRIVE`) | 기어 P/D (`VehicleGear.GEAR_PARK`·`GEAR_DRIVE` …) | Backend 기술명세 2.4, sdk-findings "Vehicle 2.3.2 AAR 정적 분석". Pleos `DrivingMode`는 주행 모드(NORMAL/ECO/SPORT)라 기어가 아니다(2026-11-09 정정) |
| 2 | AAOS 표준 속성 `GEAR_SELECTION` / `CURRENT_GEAR` (`CarPropertyManager`) | 기어 P/D | 권한 `android.car.permission.CAR_POWERTRAIN` = **normal**(선언만, AOSP android14-release 소스 확인). Pleos FAQ도 속도를 `CarPropertyManager`로 읽도록 안내 |
| 3 | `PARKING_BRAKE_ON` | 주차 브레이크 체결 = 주차 | 같은 권한 |
| 4 | `IGNITION_STATE` (OFF / ACC / ON / START) | OFF·ACC가 되는 **그 순간** 운행 종료(5초 안내 없이 바로 확정) | 같은 권한. 그 순간 프로세스가 살아 있어야 함 |
| 최후 | 위 신호가 하나도 없을 때 | 아래 규칙 | — |

**최후 규칙 (기어·주차 브레이크·시동 신호가 모두 없을 때)**

- **운행 종료**: 시동을 꺼도 운행은 RECORDING으로 남는다. 다음 시동(앱 프로세스 재개, 부팅 후 서비스 재시작, 또는 사용자가 앱을 열 때) 때 계기판을 읽어서
  - 마지막 저장값과 **같으면 묻지 않고 자동 확정**한다. 거리는 정확하고, 종료 시각은 마지막으로 거리가 변한 시각이다(0탭).
  - **늘어 있으면**(앱 없이 달림) 그때만 묻는다(Backend F-10 복구 다이얼로그).
- **화면 단계**: 아래 둘 중 하나면 `PARKED`로 간주한다.
  - 시동 직후 아직 한 번도 움직이지 않은 동안
  - **거리 불변 60초 이상**(`T_PARKED_FALLBACK_SEC = 60`, 사용자 결정)
  - 대가: 60초가 넘는 신호 대기에서 전체 앱이 열릴 수 있다(사각지대 B-2). Pleos 정책은 움직이는 주행만 막으므로 규정 위반은 아니다.
- 기어 P 5초 자동 종료(7.2.1)는 P 신호(1~3순위)가 있을 때만 동작한다. 최후 규칙에서는 `NO_MOVEMENT` 180초, 내비 도착, 시동 재개 시 자동 확정, [기록 종료] 1탭으로 끝난다.
- 단계가 내려가면(FULL → PEEK → BACKGROUND) 열려 있던 화면·시트·다이얼로그를 모두 닫는다. 단계가 다시 올라가도 닫힌 화면을 되살리지 않고 Home에서 시작한다.
- 이 표는 Backend 명세의 "주행 중 입력 차단(F-13)"과 "주행 중 상태 표시(F-03)"를 대체·구체화한다. F-03의 주행 중 표시는 **알림**(앱 A)이 맡고, 앱 화면은 주행 중에 보이지 않는다.

### 1.2 조작 최소화 — 팔을 드는 비용

차량 디스플레이는 팔을 뻗어 들어 올려야 닿는다. 노트북 터치스크린처럼 **터치 한 번의 신체 부담이 스마트폰·태블릿보다 훨씬 크다.** 그래서 같은 의사 결정을 하더라도 조작 횟수를 최소로 설계한다.

1. **기본값은 조작 없이 진행되고, 사람은 예외일 때만 누른다(opt-out).** 대표 예: 기어 P → "기록을 마칠까요?" + 5초 카운트다운 → 아무것도 누르지 않으면 자동 종료. [계속 기록]을 눌러야만 이어진다. [기록 종료]를 누르면 기다리지 않고 바로 끝난다.
2. **고르면 바로 실행된다.** 목록에서 고른 뒤 [확인]을 또 누르게 하지 않는다. 대표 예: 넷플릭스식 운전자 선택은 타일 1탭이 곧 선택이자 진입이다.
3. **확인 다이얼로그로 같은 결정을 두 번 묻지 않는다.** 되돌리기 어려운 동작(삭제)에만 확인을 묻는다.
4. **텍스트 입력은 최후의 수단.** 프리셋 → 최근값 → 음성 → 키보드 순.
5. **자동 진행에는 항상 남은 시간과 1탭 취소가 보인다.** 사람이 모르게 결정되지 않게 한다.

작업별 **조작 예산**(이 횟수를 넘기면 설계를 다시 본다):

| 작업 | 예산 | 방법 |
|---|---|---|
| 운전자 선택 | 1탭 | 넷플릭스식 타일 (배차 운전자가 첫 자리) |
| 운행 시작 | 0탭 (직접 시작은 1탭) | 운전자 선택 후 출발하면 자동 시작 + 10초 취소 알림(Backend F-02, 앱 A) |
| 운행 종료 | 0탭 (바로 끝내기는 1탭) | 기어 P → 카운트다운 → 자동 종료. [기록 종료] 1탭이면 즉시 |
| 목적 확정 | 1탭 (확신 낮으면 2탭) | 큰 추천 버튼 1개 / 선택지에서 1개 |
| 영수증 확정 | 1탭 | 전부 초록 + 검산 통과 + 후보 1개면 [확정]만 |
| 운행기록부 발송 | 2탭 | [만들기] → [보내기] (수신자는 기본 전체 선택) |
| 정차 중 상태 확인 | 0탭 (앱 열기 제외) | Peek 패널, 읽기 전용 |

---

## 2. 문서 서열과 근거

같은 주제를 문서마다 다르게 말할 때 아래 순서로 따른다. **위가 이긴다.**

| 순위 | 문서 | 성격 |
|---|---|---|
| 0 | **Pleos 공식 가이드** (UI Design Guide, 주행 규제 정책, 앱 심사 가이드) | 외부 제약. 어기면 앱 심사에서 거부된다. 다른 모든 문서보다 우선한다 |
| 1 | **기능명세** (팀 문서 "앱 사용 시나리오 및 주요 기능") | 무엇을 할지의 최상위 기준 |
| 2 | **계획서** (팀 문서 "AutoLog 기술 명세서", Java·FastAPI판) | 기능명세가 말하지 않은 부분의 기준 |
| 3 | **AutoLog-Backend 리포의 `docs/`·코드** (기능명세 v0.3, 기술명세, CONVENTIONS, `Contracts.kt`) | 현재 구현 현황. 계약 타입·API 형태는 이 코드가 사실이다 |
| 4 | 이 문서 | 위 문서들을 프론트 구현으로 옮긴 결과 |

**서열을 적용한 결과 (요약)**

- **취합 방식은 두 가지 다** — 기능명세가 정의한 ① 이메일로 엑셀 전송 → 관리자가 취합, ② 사내 서버로 전송 → 자동 취합(소프트웨어는 우리가 제공하고 운영은 회사가 자체 호스팅하는 "GitLab 방식"). 리포 v0.3이 ②를 P2 로드맵으로 뺀 것은 기능명세에 밀린다. 계획서의 중앙 SaaS형 백엔드(`api.autolog.example`)도 기능명세의 "자체 호스팅"에 밀린다. → 8장
- **기록할 정보** — 기능명세의 해당 절은 아직 비어 있다. 계획서 2.8 `TripEntity` 필드(운전자·시각·누적거리·출발·도착·목적·출처·상태)를 기준으로 한다. 기능명세가 채워지면 그쪽을 따른다.
- **언어·UI** — 기능명세는 언어를 정하지 않는다. 계획서는 Java 17 + XML/View를 말하지만, 이미 구현된 `vehicle-core`의 계약이 Kotlin `suspend`·`Flow`·sealed 타입이라 Java로는 직접 호출할 수 없다. **Kotlin + Jetpack Compose + Material 3**로 확정했다(사용자 결정, 2026-11-09). → D-02

---

## 3. 현황 (2026-11-09 기준)

| 대상 | 상태 |
|---|---|
| AutoLog-Frontend | 빈 리포(커밋 0). 이 문서가 첫 커밋 |
| Backend `app/` | placeholder `MainActivity` 1개뿐. 이 리포의 `vehicle-app/`이 대체한다 |
| Backend `web/` | `web/admin` 빈 골격(React 18.3.1 · TS 5.6 · Vite 6), `web/camera` 빈 파일. 이 리포의 `web/`이 대체한다 |
| 앱 B 계약 (구현·Fake 있음) | `ContextApi` · `PurposeEngine` · `VoiceApi` · `ReceiptApi` · `MailApi` · `SettingsApi` — `backend/vehicle-core/src/main/kotlin/kr/autolog/vehicle/core/Contracts.kt` |
| 앱 A 계약 (없음) | `TripSessionApi` · `LocalStore` · `ReportApi` · `DeviceApi` — `Contracts.kt`에 `TODO(A)`만 있다. → 6.6 interim |
| relay API | 차량 `/v1/devices`·`/v1/inbox`·`/v1/inbox/{id}/ack`·`/v1/qr-tokens`, 카메라 `POST /r/{code}/receipt?t=`(multipart `image`), 관리자 `/admin/vehicles|users|places|schedules|recipients`(CRUD + CSV import), 인증 `X-Admin-Token` 헤더 |
| 일정 | 11.14 3인 통합 → 11.15 1차 제출 → 12.03–04 시연 |

---

## 4. 핵심 결정 (Decision Log)

결정을 바꾸려면 이 표를 먼저 고친다. "기각한 대안"은 같은 논의를 반복하지 않기 위해 남긴다.

| ID | 결정 | 이유 | 기각한 대안 (이유) |
|---|---|---|---|
| D-01 | 화면은 **AutoLog-Frontend 리포**에 두고, Backend를 **git submodule + Gradle composite build**로 가져온다 | 프론트 소유 영역을 리포 단위로 분리. `vehicle-core` 변경이 바로 반영됨 | Backend 모노레포 `app/`(사용자가 별도 리포를 결정) · AAR 배포(로직이 바뀔 때마다 배포해야 해서 통합 직전에 느림) |
| D-02 | **Kotlin 2.0.21 + Jetpack Compose(BOM 2024.12.01) + Material 3** | `vehicle-core` 계약을 그대로 호출. Backend와 버전 카탈로그 공유 | Java + XML(계약 호출에 Kotlin 브리지가 필요) · Kotlin + XML(Backend 골격과 불일치, 가변 레이아웃 비용 큼) |
| D-03 | **MVVM**: Compose → ViewModel(`StateFlow<UiState>`) → **Repository** → `vehicle-core` Contracts | 화면은 SDK·계약을 모른다. ViewModel은 가짜 Repository로 JVM 테스트 | 화면이 Contracts를 직접 호출(교체·테스트 어려움) · MVI 라이브러리 도입(규모 대비 과함) |
| D-04 | Gradle은 **Kotlin DSL**, 버전은 **Backend의 `gradle/libs.versions.toml`을 그대로 읽는다**. 프론트 전용 라이브러리만 `gradle/fe.versions.toml` | composite build는 AGP·Kotlin 버전이 어긋나면 깨진다. 같은 파일을 읽으면 구조적으로 어긋날 수 없다 | 버전 복사(드리프트) · Groovy DSL(계획서 원안이나 Backend와 불일치) |
| D-05 | **수동 DI**(`AppGraph`) + product flavor **`demo` / `vehicle`** | 화면 10개 규모에 Hilt는 과함. `demo`는 전부 Fake라 일반 에뮬레이터·Preview·스크린샷 테스트에서 돈다 | Hilt(빌드 시간·학습 비용) · Koin(런타임 오류) |
| D-06 | 자체 **`Navigator`**(sealed `Route` 백스택) | 주행 시작 시 모든 보조 화면을 닫는 규칙(F-13)을 한 곳에서 처리하고 JVM 테스트 가능. 의존성 0 | navigation-compose(주행 규칙을 화면마다 흩어 처리하게 됨) |
| D-07 | 사용 단계(`UsageTier`) = **앱 A 차량 상태(기어·움직임)와 AAOS `CarUxRestrictions` 중 더 제한적인 쪽**, 판정 불가면 `BACKGROUND` | Pleos 가이드는 DO 앱도 `CarUxRestrictions`를 따르라고 요구. 보수적 판정은 Backend 명세와 동일 | 한쪽만 사용(규제 누락 또는 Odometer 지연) |
| D-08 | **가변 레이아웃 3단계**(NARROW·WIDE·TALL), `screenOrientation` 미지정, `resizeableActivity=true`, 창 크기 변경을 `configChanges`로 받아 Activity 재생성 없이 리컴포즈 | Pleos는 창 1~2개·가로/세로 전환을 사용자가 언제든 한다. 재생성이 없으면 상태가 유지되고 전환이 부드럽다 | 가로 고정(Pleos 가이드 위반) · 재생성 허용(전환 시 깜빡임·상태 손실) |
| D-09 | 서체 **Asta Sans**(42dot, OFL, 가변 300–800) 번들 | Pleos 시스템 UI 서체이자 현대차그룹 계열(42dot) 서체. OFL이라 앱에 넣을 수 있다 | Hyundai Sans(기업 전용, 배포 라이선스 없음) · Noto Sans KR(브랜드 연결 없음) |
| D-10 | 디자인: **테슬라식 미니멀 + 넷플릭스식 사용자 선택 + 애플식 부드러움**. 단 로고·색·에셋은 가져오지 않는다 | 사용자 결정. 상세는 `design-language.md` | — |
| D-11 | **정차(Peek) 화면은 정적**: 애니메이션 없음, 숫자는 즉시 교체. 주행 중에는 화면 자체가 없다 | Pleos: "주행 중 애니메이션·자동 스크롤로 시선을 유도하면 안 됨". 신호 대기 중 힐끗 보는 화면도 같은 기준 | 모션 유지(심사 거부 위험) |
| D-12 | 사내 서버 취합은 **11.15 이후** 화면부터 만든다. 11.14 통합은 메일 경로 완성 | 업로드 계약·API를 A·B가 새로 만들어야 함 | 11.15 전 동시 진행(통합 위험) |
| D-13 | 스크린샷 테스트 **Roborazzi + Robolectric**, 기준 플랫폼은 Linux(WSL·CI) | WSL에서는 Pleos 에뮬레이터를 돌릴 수 없다. JVM 렌더링으로 디자인을 검토하고 회귀를 잡는다 | Paparazzi(layoutlib 기반이라 Robolectric 위의 Compose UI 테스트와 인프라를 공유하지 못하고, 탭 같은 상호작용 후 상태를 찍기 어렵다) · 에뮬레이터 스크린샷만(맥에서만 가능) |
| D-14 | 운영 웹: React 18.3.1 · TS 5.6 · Vite 6(기존 골격 유지) + Tailwind 3.4 · TanStack Query 5 · react-router-dom 6 · Vitest | 계획서·Backend 명세와 일치. 화면 8개 규모에 충분 | Next.js(SSR 불필요) · shadcn/ui 전체(필요한 것만 직접) |
| D-15 | 카메라 페이지: **빌드 없는 HTML 1장**, 시스템 서체, 업로드 전 canvas 축소 | 폰에서 QR 스캔 → 15초 안에 차 화면까지(F-30). 외부 요청 0 | React 번들(첫 로딩 지연) · 웹폰트(외부 요청·지연) |
| D-16 | **사용 3단계**: 주행 = 백그라운드(화면 없음), 정차 = Peek(읽기 전용), 주차 = 전체 앱 (1.1) | 사용자 결정. 운전 중 조작 0이 가장 안전하고 심사 위험도 가장 낮다 | 주행 중 표시 전용 Home(시선 유도, 심사 위험) · 정차 중 전체 앱(신호 대기 중 조작 유도) |
| D-17 | **조작 최소화**: 기본값 자동 진행 + 예외만 터치, 작업별 조작 예산 (1.2). 기어 P → 카운트다운 자동 종료 | 사용자 결정. 차량 터치는 팔을 들어야 해서 1회 비용이 크다 | 매번 확인 버튼(조작 2배) · 타이머 없는 자동 종료(사람이 모르게 결정됨) |
| D-19 | **운전자 표시는 화면 오른쪽 위 한 곳**(넷플릭스식 `DriverBadge`: 네모 프로필 아이콘 + "○○님"). 주차(FULL)에선 누르면 운전자 선택, 정차(Peek)에선 표시만. 선택 화면은 사람 타일 끝에 **[+ 새 운전자 추가]**, 미확정 운행 질문은 선택 화면에서 빼고 History에서 1탭 지정(F-20) | 사용자 결정(2026-11-09). 이름이 상태 칩·hero와 섞이면 "누가 운전 중인가"를 매번 찾아야 한다. 고정 자리 하나면 시선이 학습된다. 선택 화면은 "지금 누가 타나"만 묻는다(질문 하나 = 조작 하나) | 기록 중 칩 옆에 이름(상태와 사람이 섞임) · 선택 화면에 이전 운행 질문(첫 화면이 두 질문이 됨) |
| D-20 | **주유 기록 = QR이 기본, 직접 입력은 보조 분기.** Home [주유 기록 추가] → QR(7.4) → 폰으로 영수증 업로드 → OCR 값이 채워진 **입력 창 하나**(7.5 FuelEntry) → [추가하기]. QR 화면 아래 [직접 입력하기]는 같은 입력 창을 빈 칸으로 연다. 영수증 확인 화면과 주유 입력 화면을 따로 두지 않는다 | 사용자 결정(2026-11-09). 사람이 하는 일은 "주유 기록 하나 추가"로 같다 — 출처(영수증·손)가 달라도 같은 창이면 배울 것이 하나다. 영수증이 기본이라 입력 터치가 거의 없다(1.2) | 영수증 저장·주유 기록 버튼 두 개(같은 뜻의 버튼이 나란히) · 영수증 확인 화면과 직접 입력 화면 분리(필드·버튼 위치가 달라 두 번 배움) · 마이크 우선 입력(사용자 흐름에 없음 — 후속 결정으로 미룸) |
| D-21 | **팝업은 화면 가운데 둥근 카드 하나(`ModalCard`)로 통일.** 아래에 붙는 시트는 쓰지 않는다. 결정 버튼은 카드 하단 고정 영역(footer)에 두고 내용만 스크롤한다. 취소·닫기는 모두 오른쪽 위 X(하단 [취소] 없음). 제목·설명 문구는 가운데 정렬. 입력 칸은 채움형 | 사용자 결정(2026-11-09): 아래에 붙는 시트와 테두리 입력 칸이 "옛 안드로이드 초기 앱"처럼 보였다. 가운데 카드는 시선이 화면 중앙(hero 자리)에서 크게 움직이지 않고, 운전석·조수석 어느 쪽에서도 같은 거리다 | 바텀 시트(팔이 닿기 쉽지만 낡아 보이고 가로 화면에서 넓게 퍼짐) · 팝업마다 다른 모양 |
| D-26 | **앱 안 알림 없음 — 시스템 알림만.** Home의 알림 띠·알림 목록을 없앤다(D-25의 알림 띠 부분을 대체). 알려야 할 일은 앱 A의 시스템 알림(헤즈업 "기록을 마칠까요?" 등)만 맡는다. 옮긴 곳: 목적 없는 운행 → 운행 종료 시 ReviewTrip 자동 진입은 그대로, "나중에"면 사용자가 **[운행 기록 조회]에서 직접 고친다**(Home 최근 운행 줄도 누르면 운행 기록 조회, 목적 없음은 회색 "목적 미입력") · 영수증 도착 → QR 화면이 열려 있으면 자동 전환, 닫힌 뒤 도착분은 다음 [주유 기록 추가] 때 바로 그 입력 창 · 계기판 거리 못 읽음 → "주행 거리" 칸 안 한 줄 · 저장 지연 → 표시 안 함(앱 A 내부) · 사내 서버 전송 실패 → Settings "사내 서버"·History 전송 배지 · **놓친 운행(F-10) → 대기 Home에서 팝업 한 번**([운행으로 추가하기] · X=무시) | 사용자 결정(2026-11-09). 처리할 일을 늘 띄우면 Home이 할 일 목록이 되고 재촉하게 된다. 같은 정보가 이미 제자리(운행 기록 조회·설정)에 있다. 놓친 운행만은 기능명세 F-10(P0, 시연 4번 장면)이 [추가][무시]를 묻게 정해 팝업으로 남긴다 — 띠가 아니라 결정 팝업(복구 팝업과 같은 급) | 알림 띠 유지(가늘게 56 dp까지 시도) · 놓친 운행 자동 추가(명세의 [무시]가 사라지고 개인 이동이 일단 운행으로 쌓임) · 놓친 운행 미지원(P0 누락) |
| D-25 | (알림 띠 부분은 **D-26으로 대체**) **Home은 "정상은 침묵, 필요한 것만"**: 알림은 칸이 아니라 상태 줄 아래 **알림 띠 — 있을 때만**(넘치면 "외 N건" → 목록 팝업). 기록 자체의 문제(계기판 거리 못 읽음 · 저장 지연 30초 초과 · 사내 서버 전송 실패)도 알림으로. 상태 표시는 **배경 없는 점 + 글자**(`● 기록 중 · 35분 경과`, Peek도 같음). 가운데 두 섹션 = 기록 중 "주행 거리"(72 sp) · "운행 경로"(경로 선: 출발 ○ → 흐르는 화살표 → 도착 ●, 목적지가 값·출발지는 작은 보조 글자, 목적지 두 줄까지), 대기 "이번 달 운행 정보" · **"최근 운행 기록"(최신 3건 목록, 목적 없으면 경고색 + 1탭 목적 확인)** | 사용자 결정(2026-11-09, 외부 리뷰 중 필요한 것만 수용). 주차 중 사용자가 궁금한 순서: 잘 기록되나 → 어디 가는 운행인가 → 얼마나 왔나 → 처리할 일. 거리는 계기판처럼 계속 볼 값이 아니다. 알림 없음은 늘 보여 줄 정보가 아니다 | 알림이 오른쪽 칸 전체 · 96 sp 거리 · 초록 알약 상태 배지(거리와 경쟁) · 하단 버튼 분리·주유 기록 제외·종료 확인(D-24·D-17과 충돌해 미수용) |
| D-24 | **Home 위쪽 = 좌우 반반 두 섹션, 아래 = 같은 모양 타일 4개.** 왼쪽 섹션 "이번 달 운행 정보"(대기: 이번 달 요약) / "현재 운행 정보"(기록 중: 거리 + 경로), 오른쪽 섹션 "알림"(없으면 "새 알림이 없어요"로 칸 유지). 두 소제목은 같은 `SectionHeader`. 아래 타일: [기록 시작] · [주유 기록 추가] · [운행 기록 조회] · [설정](2026-11-09 사용자 결정 — 주 동작인 기록 시작·종료를 운전석 쪽 끝에) — 같은 크기·모양·간격, 아이콘만 다름. 아이콘 네모 위 · 이름 아래(이름은 작은 설명 글씨, 두 줄까지), 언제나 4칸 한 줄. 기록 중에는 첫 칸이 같은 자리에서 [기록 종료](파란 채움)로 바뀐다 | 사용자 결정(2026-11-09, D-23을 다듬음). 반반·같은 소제목·같은 타일이 정렬을 조화롭게 한다. 수동 시작은 드물지만 필요한 사람이 있어 남긴다. 위치가 고정된 [기록 시작/종료]는 손이 기억한다 | 주인공 55:45 + 알림 없으면 칸 사라짐 · 독 버튼 모양이 제각각(알약·아이콘만·글자 버튼) |
| D-23 | **Home = 상태 한 줄 · 주인공 하나 · 알림 목록 · 하단 독.** 대기 주인공은 누적 거리 대신 **이번 달 요약**(운행 횟수·거리·업무 거리), 기록 중 주인공은 **이번 운행 거리 + 경로**(Peek과 같은 `RouteFlow`). 확인 필요·영수증 도착·놓친 운행은 **"알림" 목록 하나**로. [이력]+[운행기록부]는 **[운행 기록]** 하나(History 안에서 기록부 만들기·보내기). [운행 시작]은 독 끝의 작은 **"직접 시작"**으로 | 사용자 결정(2026-11-09). 누적 거리는 계기판에 이미 있고, 출발하면 자동으로 기록되므로 수동 시작이 가장 큰 버튼일 이유가 없다. 같은 성격(처리할 일)을 칩·칩·배너 세 모양으로 흩어 놓지 않는다. 독은 상태가 바뀌어도 자리가 같다(테슬라·CarPlay) | 누적 거리 hero + 보조 동작 4칸 + 큰 [운행 시작](주인공 다섯 개가 경쟁) |
| D-18 | 기어·시동 판정 우선순위: Pleos Gear → AAOS `GEAR_SELECTION`/`CURRENT_GEAR` → `PARKING_BRAKE_ON` → `IGNITION_STATE` → 최후 규칙(다음 시동 때 자동 확정, 거리 불변 60초 = 주차) (1.1.1). 기어 P 자동 종료 대기 **5초** | 사용자 결정. Pleos Vehicle SDK 2.3.2에 `Gear` API가 있어 1순위로 해결되고(2026-11-09 AAR 확인), 없더라도 표준 속성(`CAR_POWERTRAIN`, normal)으로 대부분 해결되며, 최후에도 거리는 정확하다 | 다음 시동 때 매번 묻기(계기판이 같으면 물을 필요가 없음) · 기어 없으면 전체 앱 불가(파킹 후 목적 확인을 못 함) |

---

## 5. 리포 구조와 빌드

### 5.1 디렉터리

```
AutoLog-Frontend/
├── backend/                      # git submodule → AutoLog-Backend (main에 머지된 커밋만 고정)
├── settings.gradle.kts           # includeBuild("backend") + 카탈로그 2개
├── build.gradle.kts              # 플러그인 apply false, ktlint
├── gradle.properties             # Backend와 동일 값
├── gradle/
│   ├── wrapper/                  # Gradle 8.12 (Backend와 동일)
│   └── fe.versions.toml          # 프론트 전용 라이브러리
├── .editorconfig                 # Backend 파일과 동일 내용
├── vehicle-app/                  # :vehicle-app — Pleos 차량 앱
│   ├── build.gradle.kts
│   ├── src/main/                 # 공용 코드 (6장 패키지)
│   ├── src/demo/                 # DemoAppGraph, 데모 컨트롤 패널
│   ├── src/vehicle/              # VehicleAppGraph
│   ├── src/main/res/font/        # asta_sans.ttf (가변) + OFL.txt
│   ├── src/test/                 # ViewModel·Navigator·토큰 테스트, Roborazzi
│   └── src/test/screenshots/     # Roborazzi 출력(로컬 검토용, .gitignore — 2026-11-09 사용자 결정)
├── web/
│   ├── admin/                    # 운영 웹
│   └── camera/index.html         # 폰 카메라 페이지
├── ops/compose.frontend.yml      # Backend compose에 웹 산출물을 얹는 override (9.3)
├── docs/
│   ├── design-language.md
│   ├── superpowers/specs/        # 설계 스펙(이 문서)
│   ├── superpowers/plans/        # 구현 계획
│   └── verification.md           # [검증 필요] 결과 기록(맥 에뮬레이터)
├── .github/workflows/ci.yml
├── CONVENTIONS.md · CLAUDE.md · README.md
```

### 5.2 submodule

```bash
git submodule add https://github.com/hmg-app-challenge-2026/AutoLog-Backend backend
git clone --recurse-submodules <AutoLog-Frontend URL>      # 새로 받을 때
git submodule update --init --recursive                    # 이미 받은 리포
```

- **반드시** submodule 포인터는 Backend `origin/main`에 머지된 커밋으로만 올린다. 커밋 메시지: `build(backend): submodule을 <짧은 해시>로 갱신 (<무엇이 들어왔나>)`.
- 앱 A·B의 브랜치를 미리 써 보려면 로컬에서 `cd backend && git checkout <branch>` 하고 **포인터 변경은 커밋하지 않는다**.
- `backend/` 안의 파일을 이 리포에서 고치지 않는다. 고칠 일이 있으면 Backend에 PR을 올린다.

### 5.3 Gradle 설정 (그대로 옮겨 쓴다)

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/") // Pleos SDK (vehicle-core 전이 의존)
    }
    versionCatalogs {
        create("libs") { from(files("backend/gradle/libs.versions.toml")) } // Backend와 같은 버전 (D-04)
        create("fe") { from(files("gradle/fe.versions.toml")) }             // 프론트 전용
    }
}
rootProject.name = "AutoLog-Frontend"

includeBuild("backend") {
    dependencySubstitution {
        substitute(module("kr.autolog:vehicle-core")).using(project(":vehicle-core"))
    }
}
include(":vehicle-app")
```

```kotlin
// vehicle-app/build.gradle.kts (핵심만)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(fe.plugins.roborazzi)
}
android {
    namespace = "kr.autolog.vehicle"
    compileSdk = 35
    useLibrary("android.car")                     // CarUxRestrictions (D-07)
    defaultConfig {
        applicationId = "kr.autolog.vehicle"
        minSdk = 28; targetSdk = 35                // Pleos: minSdk 28 이상 필수
        versionCode = 1; versionName = "0.1.0"
    }
    flavorDimensions += "backend"
    productFlavors {
        create("demo")    { dimension = "backend"; applicationIdSuffix = ".demo" } // 전부 Fake
        create("vehicle") { dimension = "backend" }                                // 실구현
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }   // Robolectric·Roborazzi
    buildTypes {
        release {
            isMinifyEnabled = true; isShrinkResources = true          // Backend app/과 동일
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += setOf("META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/*.kotlin_module") }
}
dependencies {
    implementation("kr.autolog:vehicle-core")     // composite build가 backend 프로젝트로 치환
    // Compose — Backend app/build.gradle.kts와 동일 목록
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui); implementation(libs.compose.m3)
    implementation(libs.compose.tooling.preview); debugImplementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.vm.compose); implementation(libs.lifecycle.runtime.compose)
    // 프론트 전용
    implementation(fe.compose.icons.extended)
    implementation(fe.coil.compose)
    implementation(fe.zxing.core)
    testImplementation(libs.junit); testImplementation(libs.coroutines.test)
    testImplementation(fe.turbine); testImplementation(fe.robolectric)
    testImplementation(fe.roborazzi); testImplementation(fe.roborazzi.compose)
    testImplementation(fe.compose.ui.test.junit4); debugImplementation(fe.compose.ui.test.manifest)
}
```

> composite build의 `module("kr.autolog:vehicle-core")` 치환은 Backend가 `group`을 정하지 않아도 동작한다(치환 규칙이 프로젝트 경로로 연결). 첫 스파이크에서 `./gradlew :vehicle-app:assembleDemoDebug`로 확인한다. 실패하면 대안은 `settings.gradle.kts`에서 `include(":vehicle-core"); project(":vehicle-core").projectDir = file("backend/vehicle-core")`와 `:shared` 동일 처리다(이 경우 Backend의 `libs` 카탈로그 이름이 같아서 그대로 빌드된다). 어느 쪽을 썼는지 `docs/verification.md`에 남긴다.

```toml
# gradle/fe.versions.toml
[versions]
coil = "2.7.0"
zxing = "3.5.3"
turbine = "1.2.0"
robolectric = "4.14.1"
roborazzi = "1.32.2"

[libraries]
compose-icons-extended = { module = "androidx.compose.material:material-icons-extended" }   # BOM 버전, release는 R8이 제거
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coil" }
zxing-core   = { module = "com.google.zxing:core", version.ref = "zxing" }
turbine      = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
robolectric  = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
roborazzi    = { module = "io.github.takahirom.roborazzi:roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { module = "io.github.takahirom.roborazzi:roborazzi-compose", version.ref = "roborazzi" }

[plugins]
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
```

- **버전 선정 기준**: Backend 카탈로그(Kotlin 2.0.21, AGP 8.8.2, Compose BOM 2024.12.01)와 **같은 시기의 안정판**으로 고정한다. 위 버전은 2026-11-09에 Maven Central에서 존재를 확인했다(Roborazzi 1.32.2는 kotlin-stdlib 1.9.22 의존이라 Kotlin 2.0.21과 호환). 최신판(예: Roborazzi 1.7x, Robolectric 4.17)은 Kotlin·Compose를 끌어올릴 수 있으므로, Backend 카탈로그가 올라가기 전에는 올리지 않는다.
- `gradle.properties`, `.editorconfig`, ktlint 설정(플러그인 12.1.2, ktlint 1.5.0)은 Backend와 **같은 값**을 쓴다.
- 새 라이브러리를 넣을 때는 `fe.versions.toml`에만 넣고, PR에 "왜 필요한지 / APK에 들어가는지"를 적는다. Backend 카탈로그에 이미 있는 라이브러리를 `fe`에 다시 선언하지 않는다.

### 5.4 Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:name="android.hardware.type.automotive" android:required="false" />
    <application
        android:name=".AutoLogApp"
        android:allowBackup="false"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.AutoLog"
        android:usesCleartextTraffic="true">   <!-- 시연용 relay가 HTTP. 운영은 network-security-config로 제한 -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:resizeableActivity="true"
            android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation|density|uiMode"
            android:windowSoftInputMode="adjustResize">
            <!-- screenOrientation은 지정하지 않는다 (D-08) -->
            <meta-data android:name="distractionOptimized" android:value="true" />  <!-- [검증 필요] V-03 -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- **DO 선언 이유**: 정차(PEEK) 중에도 Peek 패널을 보여 주려면, 플랫폼이 정차 상태에서 DO 미선언 앱을 막을 경우에 대비해 DO가 필요하다. DO를 선언하면 주행 중에도 플랫폼이 앱을 막아 주지 않으므로 **주행 시 숨기는 일은 앱이 스스로 한다**(6.9). **결정 규칙**: V-11에서 "Pleos는 정차(IDLING) 중 DO 미선언 앱을 막지 않는다"가 확인되면 DO 선언을 지운다. 그러면 주행 중 차단을 플랫폼이 보장해 주므로 더 안전하고 심사에도 유리하다. 6.9의 자체 숨김은 그대로 둔다(이중 안전).
- 권한·서비스(Foreground Service, Pleos 권한)는 `vehicle-core` 매니페스트가 병합한다. 이 리포에서 Pleos 권한을 선언하지 않는다.
- `uiMode`를 `configChanges`에 넣는 이유: 주간·야간 전환 때 Activity를 재생성하지 않고 테마만 바꾼다(부드러운 전환).
- 단일 Activity다. 화면 전환은 Compose 안에서 `Navigator`가 한다.

### 5.5 개발 루프 — WSL과 맥

Pleos 에뮬레이터는 외장 GPU가 필요하고, 맥용 시스템 이미지는 arm64(`crp-arm64-sys-img.xml`)다. **WSL에서는 에뮬레이터를 돌리지 않는다.** 시연 환경은 Apple Silicon 맥이다.

| 할 일 | WSL | 맥 |
|---|---|---|
| 빌드 `./gradlew :vehicle-app:assembleDemoDebug` | ✅ | ✅ |
| 단위 테스트 `./gradlew :vehicle-app:testDemoDebugUnitTest` | ✅ | ✅ |
| 스크린샷 기록·검증 `recordRoborazziDemoDebug` / `verifyRoborazziDemoDebug` | ✅ (기준 플랫폼) | 기록만 참고, 커밋하지 않음(폰트 래스터라이즈 차이) |
| 에뮬레이터 실행·FormFactorConsole·VHAL | ❌ | ✅ |
| relay(docker compose) | ✅ | ✅ (시연은 맥에서) |

**맥 에뮬레이터 준비 (Pleos 공식 문서 요약)**

1. Android Studio 최신 → SDK Manager → SDK Update Sites → `+` → 이름 `Pleos Connect System Image`, URL `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. SDK Platforms → Show Package Details → Android API 34 아래 Pleos Connect 이미지 설치 → Studio 재시작
3. Device Manager → Form Factor `Automotive` → `Pleos Connect v2.1.0` → 시스템 이미지 `API 34`의 `Pleos Connect v2.1.0 APIs`
4. Advanced: **Default boot = Cold**(Quick boot면 VHAL 불가), RAM 4 GB
5. 앱 설치: `./gradlew :vehicle-app:installDemoDebug` 또는 WSL에서 만든 APK를 `adb install -r`
6. relay 접속: 에뮬레이터에서 호스트(맥)는 `10.0.2.2`. relay는 맥에서 `docker compose up`.
7. 화면 프리셋 검증: Pleos 폼팩터 테스트 가이드의 FormFactorConsole(Host Display Bridge, 포트 8095·8096)로 12.9″ Portrait / 14.6″ Landscape 등 프리셋을 띄우고, 각 프리셋의 **dp·DPI·px 값**을 `docs/verification.md`에 적는다. 이 값으로 6.8의 임계값과 10.3의 스크린샷 크기를 갱신한다.

- 네이티브 라이브러리 ABI를 제한하지 않는다(`abiFilters` 쓰지 않음). 맥 arm64 이미지와 x86_64 이미지 모두에서 설치돼야 한다.

### 5.6 CI

```yaml
# .github/workflows/ci.yml (요지)
- uses: actions/checkout@v4
  with: { submodules: recursive }
- uses: actions/setup-java@v4  # temurin 17
- uses: gradle/actions/setup-gradle@v4
- run: ./gradlew ktlintCheck :vehicle-app:testDemoDebugUnitTest :vehicle-app:verifyRoborazziDemoDebug :vehicle-app:assembleDemoDebug
- uses: actions/setup-node@v4  # node 20
- run: cd web/admin && npm ci && npm run lint && npm test -- --run && npm run build
```

`vehicle` flavor는 실구현이 준비되기 전까지 CI에서 `assembleVehicleDebug`만 돌린다(컴파일 확인).

---

## 6. 차량 앱 아키텍처

### 6.1 계층과 의존 규칙

```
┌────────────── vehicle-app (이 리포) ──────────────┐
│  ui/<화면>/XxxScreen      Compose. UiState를 그리고 Action을 올린다
│        │  (UiState, onAction)
│  ui/<화면>/XxxViewModel   StateFlow<UiState> 하나를 내보낸다
│        │  (suspend / Flow)
│  data/repository/*        화면이 쓰기 좋은 모양으로 계약을 감싼다
│        │
└────────┼──────────────────────────────────────────┘
         ▼
  backend/vehicle-core  core/Contracts.kt  (+ fake/)      ← 앱 A·B 소유
         ▼
  Gateway (vehicle/ navi/ location/ speech/) → Pleos SDK
```

**반드시 지킬 규칙**

1. `ui/` 패키지는 `data/repository`의 **인터페이스**와 자기 UiState만 안다. `kr.autolog.vehicle.core.*`의 서비스 인터페이스(`ContextApi` 등)를 import하지 않는다. (값 타입인 `PurposeDetail`, `PurposeCategory` 같은 enum은 표시용으로 import해도 된다.)
2. `ai.pleos.*` import는 이 리포 어디에도 없어야 한다. SDK는 Backend Gateway 안에만 있다(Backend CONVENTIONS 7.2).
3. `data/repository`는 `kr.autolog.vehicle.core`·`kr.autolog.vehicle.fake`·`kr.autolog.shared`와 Android 플랫폼 API(`android.car.*`)만 쓴다. Backend의 `session`·`data`·`domain` 같은 내부 패키지를 import하지 않는다.
4. ViewModel은 Android `Context`를 받지 않는다(테스트를 위해). 필요하면 Repository가 받는다.
5. Composable은 ViewModel을 파라미터로 받지 않는다. `XxxRoute`(ViewModel을 얻어 상태를 수집하는 얇은 함수)와 `XxxScreen`(순수 UI)을 나눈다. Preview·스크린샷은 `XxxScreen`만 쓴다.

### 6.2 패키지 (`kr.autolog.vehicle`)

```
AutoLogApp.kt                 Application. AppGraph 생성
MainActivity.kt               setContent { AutoLogRoot(graph) }, onNewIntent → Navigator
di/
  AppGraph.kt                 interface: 모든 Repository + Navigator + Clock
  (demo)  DemoAppGraph.kt     Fake 조립 + DemoController
  (vehicle) VehicleAppGraph.kt 실구현 조립 (6.7)
nav/
  Route.kt                    sealed interface Route
  Navigator.kt                백스택 + 주행 규칙
data/
  repository/                 6.5 표의 인터페이스와 구현
  interim/                    앱 A 계약 초안 + Fake (6.6) — A가 머지하면 삭제
ui/
  design/                     테마, 토큰, 모션, 공용 컴포넌트 (design-language.md)
  root/AutoLogRoot.kt         UsageTier에 따라 Peek / 전체 앱 분기, Navigator 상태 → 화면, 잠금 CompositionLocal 제공
  peek/                       Peek 패널 — 정차 중 읽기 전용 (1.1, 7.0)
  profile/                    ProfilePicker · AddDriverSheet (F-15, F-24)
  home/                       Home (F-02~05, F-10) + 자동 종료 안내 AutoEndPrompt (F-04, 7.2.1)
  review/                     ReviewTrip (F-06, F-08, F-09, F-17)
  receipt/                    ReceiptQr (F-30), FuelEntry = 주유 입력 창 하나(영수증 OCR 채움 F-19 · 직접 입력 F-18) — D-20
  report/                     Report·보내기 (F-21, F-22)
  history/                    History (F-12)
  settings/                   Settings (F-24, F-01, 사내 서버 8장)
```

### 6.3 ViewModel 규칙 (MVVM)

```kotlin
data class HomeUiState(            // 불변. 화면이 그릴 모든 것
    val phase: HomePhase,          // Ready / RecordingParked / AutoEnding / NeedsRecovery
    val tripKm: String?,           // 표시용 문자열까지 만든다 ("23.4")
    val autoEnd: AutoEndUi?,       // 기어 P 자동 종료 카운트다운 (remaining "5초", progress 0..1)
    val destination: String?,
    val driver: DriverChip?,
    val reviewCount: Int,
    val missedTrip: MissedTripBanner?,
)
sealed interface HomeAction {
    data object StartTrip : HomeAction
    data object EndNow : HomeAction          // [기록 종료] — 카운트다운을 기다리지 않고 즉시
    data object KeepRecording : HomeAction   // [계속 기록] — 자동 종료 취소
    /* … */
}
sealed interface HomeEffect { data class OpenReview(val tripId: String) : HomeEffect /* … */ }

class HomeViewModel(
    private val session: SessionRepository,
    private val context: ContextRepository,
    private val usage: VehicleUsageRepository,
    private val navigator: Navigator,
    private val clock: Clock,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(session.state, context.destination, tickEverySecond(clock), /* … */) { s, d, now -> reduce(s, d, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Initial)

    fun onAction(action: HomeAction) { viewModelScope.launch { /* Repository 호출 → 결과를 상태·이펙트로 */ } }
}
```

- **상태는 `StateFlow<XxxUiState>` 하나**, 입력은 **`onAction(XxxAction)` 하나**, 한 번만 일어나는 일(화면 이동, 토스트)은 `SharedFlow<XxxEffect>` 또는 `Navigator` 호출. (Backend CONVENTIONS 7.3)
- 표시용 변환(숫자 포맷, 시각 `Asia/Seoul` 변환, 라벨)은 ViewModel의 `reduce`에서 끝낸다. 화면은 문자열을 그대로 그린다. 포맷 함수는 `ui/design/Format.kt` 한 곳에 둔다(거리 소수 1자리, 금액 `68,000원`, 시각 `09:12`).
- 현재 시각은 `Clock`을 주입받는다. `System.currentTimeMillis()`를 직접 부르지 않는다.
- ViewModel 생성: `viewModel { HomeViewModel(graph.session, graph.context, graph.restriction, graph.navigator) }` (lifecycle-viewmodel-compose의 `viewModelFactory`/`initializer`). DI 프레임워크 없이 `AppGraph`에서 꺼낸다.

### 6.4 오류 처리

- Contracts는 예외를 던지지 않고 결과 값(`null`, `*Result` sealed)을 돌려준다(Backend CONVENTIONS 7.5). Repository는 그 값을 화면 친화적 sealed 타입으로 옮긴다. 예: `QrResult.Failed` → `QrUi.Error("연결을 확인해 주세요")`.
- 예외 가능성이 있는 플랫폼 호출(`CarUxRestrictionsManager` 연결 등)은 Repository 안에서 `runCatching`으로 잡고 **보수적 기본값**(잠금)으로 바꾼 뒤 `Log.w("AutoLog/ui", …)`를 남긴다. 빈 `catch {}` 금지.
- 사용자에게 보이는 오류 문구는 **무엇이 안 됐고, 무엇을 하면 되는지** 한 줄로 쓴다(`design-language.md` 9장).

### 6.5 Repository 목록

| Repository (인터페이스) | 감싸는 계약 | 화면 | 비고 |
|---|---|---|---|
| `SessionRepository` | `TripSessionApi`(A, interim) | Home, Peek, ReviewTrip, ProfilePicker | 운행 상태·시작/종료·자동 종료 카운트다운·목적 확정·운전자 지정·놓친 운행·복구. 화면에는 자기 값 타입(`TripSession`·`ActiveTrip`·`AutoEndReason`·`RecoveryAction`, 놓친 운행은 `missedKm`)만 내보내고, interim `SessionState`에서의 변환은 구현 안 한곳(`toTripSession`) |
| `VehicleUsageRepository` | `TripSessionApi.motion`(기어·움직임) + AAOS `CarUxRestrictionsManager` | Root | `val tier: StateFlow<UsageTier>` (6.9) |
| `ProfileRepository` | `PurposeEngine.orderedUsers`·`unassignedTrips`, `SettingsApi.users`·`addUser`, interim 운전자 선택 | ProfilePicker, Home 상단 칩 | |
| `ContextRepository` | `ContextApi` | Home, ReviewTrip | 목적지·최근 목적지 |
| `TripRepository` | `LocalStore.trips`(A, interim), `PurposeEngine.shouldProposePlace`·`registerClientPlace` | History, ReviewTrip | |
| `VoiceRepository` | `VoiceApi` | ReviewTrip, FuelEntry | `available=false`면 마이크 버튼 숨김 |
| `ReceiptRepository` | `ReceiptApi`, `LocalStore.receiptInbox`(A, interim) | ReceiptQr, FuelEntry | 입력 창이 넘긴 초안 하나를 출처에 따라 `confirm`(영수증) 또는 `saveFuel`(직접 입력)로 보낸다 — 화면은 저장 경로를 모른다 |
| `ReportRepository` | `ReportApi`(A, interim), `MailApi.send`·`smtpConfigured` | Report | |
| `SettingsRepository` | `SettingsApi`, `MailApi.saveSmtp`, `DeviceApi`(A, interim) | Settings | |
| `UploadRepository` | `UploadApi`(A·B, interim, 8장) | Settings, History | 사내 서버 전송 상태 |

Repository는 **interface + `Default*` 구현** 한 쌍이다. 테스트에서는 `Fake*Repository`(test 소스셋)를 쓴다.

### 6.6 앱 A 계약의 공백 — `data/interim/`

앱 A의 `TripSessionApi`·`LocalStore`·`ReportApi`·`DeviceApi`와 사내 서버용 `UploadApi`는 아직 없다. 화면 개발을 막지 않기 위해 **Backend 기술명세 8.2 스케치를 따라 초안을 이 리포에 만든다.**

```kotlin
// data/interim/InterimContracts.kt — 앱 A 계약이 머지되면 이 파일을 지운다
interface TripSessionApi {
    val state: StateFlow<SessionState>        // Idle / Recording(trip, liveKm, lastSavedAt) / StopCandidate(trip, reason, deadlineAt) / PendingReview(trip, suggestion) / NeedsRecovery(trip, extraKm)
    val motion: StateFlow<VehicleMotion?>      // MOVING / STOPPED(기어≠P, 멈춤) / PARKED(기어 P). null = 판정 불가 → BACKGROUND
    val missedTrip: StateFlow<MissedTrip?>     // F-10 배너
    val currentDriver: StateFlow<String?>      // 선택된 운전자 userId (null = 나중에 선택)
    suspend fun selectDriver(userId: String?)
    suspend fun startSession(): StartResult
    suspend fun finalize()                     // [기록 종료] — StopCandidate에서도 즉시 종료
    suspend fun resume()                       // [계속 기록] — StopCandidate → Recording, 카운트다운 취소
    suspend fun confirmPurpose(tripId: String, detail: PurposeDetail, note: String?, inputMode: InputMode)
    suspend fun assignDriver(tripId: String, userId: String, source: DriverSource)
    suspend fun acceptMissedTrip(); suspend fun ignoreMissedTrip()
    suspend fun resolveRecovery(choice: RecoveryChoice)   // RECORD / CONTINUE / DELETE
}
interface LocalStore { fun trips(month: YearMonth): Flow<List<TripRow>>; val receiptInbox: Flow<List<ReceiptPayload>> }
interface ReportApi  { suspend fun build(month: YearMonth): ReportBuildResult }      // → core.ReportFile
interface DeviceApi  { val connection: StateFlow<ConnectionState>; suspend fun register(serverUrl: String, code: String): RegisterResult }
interface UploadApi  { val status: StateFlow<UploadStatus>; suspend fun uploadNow(): UploadResult }   // 8장
```

- 타입(`SessionState`, `TripRow`, `MissedTrip` …)도 같은 파일에 둔다. 값 타입은 `core/ContractTypes.kt`에 있는 것(`PurposeDetail`, `DriverSource`, `ReportFile`, `Suggestion`)을 재사용한다.
- `StopCandidate.reason` = `GEAR_P` / `NO_MOVEMENT` / `NAVI_ARRIVED` / `USER_REQUEST`. `deadlineAt`(epoch millis)은 **자동 종료 시각**이고 앱 A의 세션이 유일한 시계다. 화면은 `deadlineAt − now`를 보여 줄 뿐 타이머를 따로 돌리지 않는다. 대기 시간은 이유별로 다르다: **`GEAR_P` = 5초**(`T_GEAR_P_SEC`, 사용자 결정 2026-11-09), `NO_MOVEMENT`·`NAVI_ARRIVED` = 180초(Backend `T_STOP_SEC`). 상수는 앱 A 소유.
- Fake: `FakeTripSession`(시작하면 1초마다 0.1 km 증가, 23.4 km에서 기어 P → StopCandidate, 데모 패널로 `motion` 전환), `FakeLocalStore`, `FakeReportApi`, `FakeDeviceApi`, `FakeUploadApi`.
- **이 초안을 앱 A에게 contract PR 제안으로 전달한다**(12장). A의 실제 시그니처가 다르면 interim을 지우고 **Repository 구현만** 고친다. ViewModel·화면은 Repository 인터페이스만 보므로 바뀌지 않아야 한다. 바뀐다면 Repository 경계가 잘못된 것이다.

### 6.7 AppGraph와 flavor

```kotlin
interface AppGraph {
    val navigator: Navigator; val clock: Clock
    val session: SessionRepository; val usage: VehicleUsageRepository
    val profile: ProfileRepository; val context: ContextRepository; val trips: TripRepository
    val voice: VoiceRepository; val receipts: ReceiptRepository; val reports: ReportRepository
    val settings: SettingsRepository; val upload: UploadRepository
}
```

| flavor | 조립 | 용도 |
|---|---|---|
| `demo` | `DemoAppGraph`: Backend `fake/`의 B Fake + `data/interim`의 A Fake. **데모 컨트롤 패널** 포함 | 일반 에뮬레이터, Preview, Roborazzi, 시연 영상 백업 |
| `vehicle` | `VehicleAppGraph`: Backend가 제공할 팩토리 `AutoLog.create(context)`(12장 요청 R-1)에서 실구현을 받는다. 팩토리가 오기 전까지는 Fake로 컴파일만 된다 | Pleos 에뮬레이터·실차 |

**데모 컨트롤 패널 (demo 전용)**: 화면 왼쪽 위 구석을 길게 누르면 열리는 팝업(`ModalCard`). `주행·정차·주차 전환`, `기어 P(자동 종료 시작)`, `+1 km`, `내비 도착`, `영수증 도착(정상·불확실·실패)`, `놓친 운행 +15 km`, `앱 강제 종료 후 복구`, `서버 끊김`을 누를 수 있다. 시연 영상 촬영과 VHAL 없이 상태를 재현하는 데 쓴다. `vehicle` 소스셋에는 없다.

### 6.8 화면 크기 — 레이아웃 3단계

Pleos 앱 영역은 차량·디스플레이(Connect-S 17″·14.6″·12.9″, Connect-W, Connect-WF)와 사용자의 창 분할에 따라 계속 바뀐다. 앱은 **현재 창 크기(dp)만 보고** 레이아웃을 고른다. 기기 종류로 분기하지 않는다(`Build.DEVICE`는 에뮬레이터에서 무의미).

```kotlin
enum class WindowLayout { NARROW, WIDE, TALL }

fun windowLayoutOf(widthDp: Float, heightDp: Float): WindowLayout {
    val aspect = widthDp / heightDp
    return when {
        aspect >= 1.25f && widthDp >= 840f -> WindowLayout.WIDE   // 가로 창 (앱 1개가 2칸 사용)
        aspect <= 0.70f                    -> WindowLayout.TALL   // 세로로 긴 창 (12.9″ portrait 등)
        else                               -> WindowLayout.NARROW // 분할 화면의 한 칸, 정사각에 가까운 창
    }
}
```

- 임계값(1.25, 0.70, 840 dp)은 **잠정값 [검증 필요 V-01]**이다. (TALL 기준은 처음 0.80이었으나 분할 화면 한 칸 ≈504×640dp(0.79)까지 TALL로 잡혀 2026-11-09 구현 중 0.70으로 바꿈) FormFactorConsole 프리셋을 측정한 뒤 고친다.
- `AutoLogRoot`가 `BoxWithConstraints`로 계산해 `LocalWindowLayout` CompositionLocal로 내려 준다. 화면은 이 값으로 배치만 바꾸고, **상태·기능은 3단계 모두 같다**(Pleos: 어떤 창 크기에서도 핵심 기능 사용 가능).
- 창 크기 변경은 Activity 재생성 없이 리컴포즈된다(5.4 `configChanges`). 레이아웃 전환은 주차(FULL) 중 `motion.layout`, Peek에서는 즉시 교체(D-11).
- 시스템 UI(상태 표시줄, GNB, 공조 패널, 키보드)는 앱 위에 겹칠 수 있다. 루트에서 `WindowInsets.safeDrawing`을 패딩으로 적용하고, 핵심 정보(거리, 주 버튼)를 창 가장자리 24 dp 안쪽에 두지 않는다.

### 6.9 사용 단계와 잠금 (1.1, F-13)

```
fromVehicle = when (session.motion) {
    PARKED  -> FULL
    STOPPED -> PEEK
    MOVING, null -> BACKGROUND            // 판정 불가 = 주행으로 간주
}
fromPlatform = if (carUx.isRequiresDistractionOptimization) PEEK else FULL   // 연결 실패 = PEEK
tier = minOf(fromVehicle, fromPlatform)   // BACKGROUND < PEEK < FULL, 더 제한적인 쪽
```

1. `VehicleUsageRepository.tier: StateFlow<UsageTier>`가 유일한 진실이다. 화면은 따로 판정하지 않는다.
2. **단계별 루트 동작** (`AutoLogRoot`)
   - `FULL` → Navigator 백스택의 화면을 그린다(전체 앱).
   - `PEEK` → 백스택과 무관하게 **Peek 패널만** 그린다(7.0). 입력 요소가 하나도 없다.
   - `BACKGROUND` → 빈 `bg` 화면을 한 프레임 그리고 `activity.moveTaskToBack(true)`로 물러난다. 앱이 이미 보이지 않으면 아무것도 하지 않는다. 기록은 앱 A의 서비스가 계속한다.
3. **단계가 내려갈 때**(FULL → PEEK/BACKGROUND): `Navigator.onTierDropped()`가 백스택을 `[Home]`으로 줄이고 열린 시트·다이얼로그를 닫는다. **2초 안에**(F-13 수용 기준). 단계가 다시 올라가도 닫힌 화면을 되살리지 않는다.
4. `LocalInputLocked`(= `tier != FULL`)를 내려 준다. 모든 상호작용 컴포넌트(`ui/design/components`)는 이 값을 읽어 스스로 비활성화한다. PEEK에는 원래 입력 요소가 없지만, 단계 전환 순간의 오조작을 막는 이중 안전이다.
5. `CarUxRestrictions`의 `ActiveRestrictions` 비트(`UX_RESTRICTIONS_NO_KEYBOARD` 등)는 `RestrictionFlags`로 옮겨 둔다. FULL이라도 플랫폼이 키보드를 막으면 텍스트 입력 화면은 프리셋·음성 입력만 보인다.
6. `moveTaskToBack`이 Pleos 멀티 윈도우에서 기대대로 동작하는지(다른 앱이 그 창을 채우는지)는 V-12. 안 되면 `BACKGROUND`에서 빈 `bg` + 가운데 작은 "기록 중" 문구만 남긴다(정적, 입력 없음).

### 6.10 Navigator와 딥링크

```kotlin
sealed interface Route {
    data object ProfilePicker : Route; data object Home : Route
    data class ReviewTrip(val tripId: String) : Route
    data object ReceiptQr : Route; data class FuelEntry(val receiptId: String? = null) : Route  // D-20
    data class FuelEntry(val tripId: String?) : Route
    data object Report : Route; data object History : Route; data object Settings : Route
}
```

- `Navigator`는 `StateFlow<List<Route>>` 백스택과 `push/pop/replaceAll`을 가진다. 시스템 뒤로 가기는 `BackHandler`로 `pop`.
- 백스택의 모든 화면은 **주차(FULL) 전용**이다. `tier != FULL`이면 `push`가 무시된다. (PEEK은 Route가 아니라 루트가 그리는 별도 층이다.)
- 앱 시작 경로(FULL일 때): `ProfilePicker`(운전자 미선택 또는 앱 새로 시작) → `Home`. 이미 운행 중이면 바로 `Home`. PEEK·BACKGROUND로 시작하면 6.9 2번을 따른다.
- 알림 탭 딥링크: 앱 A의 `TripRecordingService` 알림이 `MainActivity`를 열 때 extra `autolog.route=review`, `autolog.tripId=<id>`를 넣는다(키 상수는 R-3 요청). `onNewIntent`에서 `Navigator.handleDeepLink`.

### 6.11 스레딩

- Contracts는 `suspend`/`Flow`다. ViewModel은 `viewModelScope`에서 부른다. 디스패처는 Repository 생성자로 주입받는다(`Dispatchers.IO` 직접 사용 금지).
- 거리 표시(`liveKm`)는 **최대 1초에 1번** 갱신한다(F-03 스로틀). `sample(1_000)`을 Repository에서 적용한다.
- 입력 반응: 탭 후 **100 ms 안에** 눌림 피드백, 250 ms 안에 결과 또는 진행 표시(Pleos: 응답 0.25초 이내).

---

## 7. 화면 명세

각 화면은 `XxxRoute` + `XxxScreen` + `XxxViewModel` + `XxxUiState`로 만든다. 레이아웃 그림의 치수·색·글자는 `design-language.md` 토큰 이름이다. 7.1 이후 모든 화면은 **주차(FULL) 전용**이다. 정차 중에는 7.0 Peek만, 주행 중에는 화면이 없다(1.1).

### 7.0 Peek — 정차 중 "지금 어떻게 기록되고 있나" (1.1, F-03) · 정차 전용 · 읽기 전용

| 항목 | 내용 |
|---|---|
| 목적 | 신호 대기처럼 잠깐 멈췄을 때, 사이드 창에서 **기록이 제대로 되고 있는지 한눈에 확인**만 한다. 조작은 하지 않는다 |
| 진입 | 정차(PEEK) 중 사용자가 앱을 연다(런처·사이드 창·알림 탭). 앱이 PEEK 단계면 백스택과 무관하게 이 패널이 보인다 |
| 이탈 | 다시 움직이면(BACKGROUND) 스스로 물러난다. 기어 P(FULL)면 전체 앱 Home으로 바뀐다(기록 중이면 7.2.1 자동 종료 안내가 함께 뜬다). 닫기는 시스템 창 조작으로 |
| 내용 (위→아래) | ① 상태 줄: `● 기록 중`(운전자 이름은 오른쪽 위 배지 — D-19) ② 섹션 **"주행 거리"** `23.4 km`(`type.heroSmall`) ③ 섹션 **"운행 경로"**(`RouteFlow`): 경로 선 — 출발 ○ `본사`(`type.caption`, `textSecondary`) → 위에서 아래로 빛이 흐르는 화살표(갈매기 3개, `accent`) → 도착 ● `판교 테크노밸리 거래처`(`type.label`, 두 줄까지, 없으면 "목적지 없음 — 도착 후 물어볼게요"를 `textSecondary`로). 출발지를 모르면 ●과 목적지만 ④ (비워 둠 — 무엇을 둘지 결정 대기. 예전 "계기판 거리 · 10초 전 저장"은 늘 같은 출처·초 단위로 깜빡이는 내부 정보라 뺐다, 2026-11-09) ⑤ 상황 줄(있을 때만): 움직임 없음 자동 종료 대기면 "멈춰 있으면 2분 41초 후 기록이 자동 종료돼요" |
| 운행이 없을 때 | "기록 중인 운행이 없어요 · 출발하면 자동으로 기록해요" + 누적거리 |
| 입력 요소 | **없다.** 버튼·스크롤·탭 반응 모두 없음. 한 화면에 다 들어가게 줄인다(넘치면 ⑤ → ④ 순으로 생략) |
| 모션 | 화면 모션은 없음. 숫자는 1초 스로틀로 즉시 교체(D-11). **예외: 경로 화살표의 흐름**(반복 상태 표시 — "이 경로를 가는 중", 현대·기아 하이브리드 회생 표시처럼 빛이 주기적으로 흐름). 위치 이동 없이 투명도만 바뀌고 주기 1.4초, 시스템 "애니메이션 줄이기"면 위→아래로 짙어지는 정지 그림(디자인 언어 8.4). 정차 중 표시 허용 여부는 V-17로 확인 |
| 레이아웃 | 사이드 창(NARROW·TALL)이 기본. 세로 스택, 가운데 정렬, 최대 폭 560 dp. WIDE에서도 같은 카드를 가운데에 둔다(넓혀서 정보를 늘리지 않는다) |
| 수용 기준 | 정차 상태에서 앱을 열면 1초 안에 Peek. 입력 노드 0개(UI 테스트). 출발하면 2초 안에 물러남 |

### 7.1 ProfilePicker — "운전자를 선택해주세요" (F-15, F-24) · 주차 전용

| 항목 | 내용 |
|---|---|
| 목적 | 넷플릭스 프로필 선택처럼 **1탭으로 운전자를 정한다**. 폰·비콘 없이 공용차 운전자를 안다. 이 화면은 질문 하나("지금 누가 타나")만 한다 — D-19 |
| 데이터 | `ProfileRepository.orderedProfiles()` = 추천 엔진 순서(`UserCard.badge` SCHEDULED/RECENT) + 엔진이 아직 모르는 직원(방금 추가한 사람 등)을 뒤에 이름순. `addDriver(name, dept)` |
| 구성 | ① 제목 "운전자를 선택해주세요"(`type.display`, NARROW는 어절 단위 두 줄 "운전자를 / 선택해주세요") ② 타일 줄: 사람 타일 최대 5개(배차 → 최근 → 이름순) → 넘치면 `[··· 다른 사람]` → **맨 끝 `[+ 새 운전자 추가]`** ③ 하단 텍스트 버튼 "나중에 선택". 타일 순서는 `ProfilePickerUiState.tiles`(`PickerTile.Person/More/Add`)가 정하고 화면은 그리기만 한다 |
| 타일 | `ProfileTile`: 정사각 아바타(이니셜, 사람별 그라데이션) + 이름 + 부서. 배차 운전자는 `ring.accent` 링 + "배차" 배지, 첫 자리. 동작 타일(`ActionTile`)은 같은 격자·크기의 아이콘 네모(`IconAvatar`) + 동작 이름(`type.label`, `textSecondary`, 두 줄까지) |
| 동작 | 사람 타일 탭 → `selectDriver(userId)` → Home(아바타 shared-element `motion.hero` → 오른쪽 위 배지 자리). "나중에 선택" → `selectDriver(null)` → Home. `[다른 사람]` → 전체 직원 팝업(제목 없음 — 펼친 목록이라 무엇을 고르는지 이미 안다. 오른쪽 위 X로 닫고, 타일 1탭이면 바로 선택·Home). `[+ 새 운전자 추가]` → 추가 팝업 |
| 추가 팝업 | 가운데 카드(D-21), 위에서 아래로 한 흐름: 머리 줄(제목 "새 운전자 추가" 가운데 · 닫기 X 오른쪽) → 아바타 미리보기(이름을 치면 이니셜, accent 그라데이션. TALL 160 dp, 그 외 120 dp) → 입력 "이름"(필수, 예: 홍길동) · "부서 (선택)"(예: 영업1팀). 고정 영역: 안내 메시지(가운데, `danger`) → **[추가 및 시작하기]**(전폭, 이름이 비면 비활성). 누르면 `addDriver` → 그 사람으로 `selectDriver` → Home — 추가와 선택이 한 번에 끝난다(1.2). **이름과 부서가 모두 같은 직원이 있으면** "이미 등록된 사용자에요"(`AddDriverResult.Duplicate`, 부서가 다른 동명이인은 추가 가능). 원천이 거절하면 그 사유. 입력을 고치면 안내를 지운다. 새 사람은 `SettingsApi.addUser`(origin=LOCAL)로 들어가며 "추가 전에 없던 id"로 찾는다. 배치: 모든 크기에서 위→아래 한 흐름. 높이 640 dp 창(WIDE·NARROW)은 이름·부서를 한 줄에 나란히, TALL은 아래로 쌓는다. [추가 및 시작하기]는 전폭, 높이 76 dp(터치 하한). 키보드가 올라오면 카드가 같이 올라간다 |
| 레이아웃 | WIDE: 타일 한 줄 가운데 정렬(타일 200 dp, 넘치면 줄바꿈) · TALL: 3열(160 dp) · NARROW(분할 화면 한 칸): 3열(128 dp) |
| 빈 상태 | 직원 0명 → "아직 등록된 직원이 없어요" + `[+ 새 운전자 추가]` 타일 + "나중에 선택" |
| 오른쪽 위 배지 | 이 화면에는 없다(운전자를 고르는 화면이므로) |
| 수용 기준 | 배차 시간대에 열면 그 운전자가 첫 타일. 1탭으로 Home. 새 운전자는 타일 1탭 + 이름 입력 + 1탭으로 그 사람으로 시작. 오프라인 동작 |

#### 7.1.1 운전자 배지 (DriverBadge) — 모든 화면 오른쪽 위 (D-19)

| 항목 | 내용 |
|---|---|
| 모양 | 네모 프로필 아이콘(`avatarChip` 48 dp, 아바타와 같은 모서리) + "○○님"(`type.label`). 운전자 미선택이면 사람 아이콘 네모 + "운전자 선택"(`accent`) |
| 자리 | `BadgeScaffold`(root)가 ProfilePicker를 뺀 모든 FULL 화면과 Peek 위에 그린다. 위는 화면 여백, 오른쪽은 화면 여백 − s3(글자가 콘텐츠 여백선에 맞게). 화면의 맨 위 줄은 `LocalTopEndReserved`(배지가 실제로 잰 폭 − s3 + s4)만큼 오른쪽을 비운다 — 이름 길이가 달라도 겹치지 않는다 |
| 동작 | FULL: 1탭 → ProfilePicker(운전자 바꾸기). Peek: 표시만(클릭 의미론 없음 — 7.0 입력 0개 검사 그대로) |
| 모달 | `ModalCard`가 떠 있으면 숨긴다(`LocalModalCount`). 팝업이 화면 전체를 덮어야 결정이 하나로 보인다 |

### 7.2 Home (F-02~05, F-10) · 주차 전용

Home은 **상태 4가지 × 레이아웃 3단계**다. 테슬라처럼 차량 상태가 화면의 주인공이다. 주차(FULL)에서만 보이므로 주행 중 변형은 없다.

| 상태 | 주인공 (hero) | 보조 정보 | 동작 |
|---|---|---|---|
| **Ready** (운행 없음) | 왼쪽 "이번 달 운행 정보": `412.6 km`(72 sp) / `23회 운행 · 업무 388.1 km` · 오른쪽 "최근 운행 기록": 최신 3건 `본사 → 판교 거래처` / `오늘 09:12 · 23.4 km · 거래처 방문`(목적 없으면 끝이 회색 `목적 미입력`, 어느 줄이든 1탭 → History — D-26) | 상태 줄 `● 대기 중`(흐린 점). 놓친 운행이 있으면 팝업 한 번(아래) | 타일 4개: [기록 시작] · [주유 기록 추가] · [운행 기록 조회] · [설정] |
| **RecordingParked** (운행 중 주차, 자동 종료를 취소한 뒤) | 왼쪽 "주행 거리": `23.4 km`(72 sp, 계기판을 못 읽으면 "거리 미확정" + 아래 한 줄 "계기판 거리를 읽지 못하고 있어요") · 오른쪽 "운행 경로": 출발지 → 흐르는 화살표 → 목적지(두 줄까지, 넘치면 말줄임) | 상태 줄 `● 기록 중 · 35분 경과`(초록 점) | 첫 칸 타일이 같은 자리에서 **[기록 종료]**(파란 채움) |
| **AutoEnding** (기어 P → 자동 종료 대기) | 7.2.1 AutoEndPrompt가 화면 위에 뜬다 | — | [기록 종료] · [계속 기록] |
| **NeedsRecovery** | — | 복구 다이얼로그: "기록 중이던 운행이 있어요 (+3.2 km)" | [기록하기] · [계속 기록] · [삭제] |

#### 7.2.1 자동 종료 안내 (AutoEndPrompt) — 기어 P (F-04, D-17)

조작 최소화(1.2)의 대표 사례다. **아무것도 누르지 않으면 기록이 끝나는 것이 기본값**이다.

| 항목 | 내용 |
|---|---|
| 트리거 | 운행 중 기어 P → 앱 A 세션이 `StopCandidate(reason=GEAR_P, deadlineAt)`로 전환하고 Odometer를 즉시 저장. 같은 순간 앱 A의 알림이 헤즈업으로 뜬다(R-7) |
| 앱 밖 (대부분의 경우) | 앱은 주행 중 백그라운드였으므로, 사람이 보는 것은 **앱 A의 헤즈업 알림**이다: "기록을 마칠까요? · 23.4 km" + 카운트다운(`setUsesChronometer` + `setChronometerCountDown`, `setWhen(deadlineAt)`) + 액션 [계속 기록] · [기록 종료]. 문구는 이 표와 같게 맞춘다 |
| 앱 안 | 사람이 앱을 열었거나 이미 열려 있으면 Home 위에 가운데 카드(`ModalCard`, D-21)로 뜬다. [기록 종료]·[계속 기록]은 카드 하단 고정 영역(왼쪽·위 [기록 종료], 오른쪽·아래 [계속 기록]). 제목 "기록을 마칠까요?"(`type.headline`) · 이번 운행 요약 `23.4 km · 판교 테크노밸리 거래처`(`type.title`) · 남은 시간 `5초 후 기록이 자동 종료돼요`(`type.body`, tnum) + 줄어드는 진행 막대 |
| 대기 시간 | **5초**(사용자 결정). 기어 P는 "주차했다"는 강한 신호이므로 기다림을 짧게 한다. 5초 안에 [계속 기록]을 못 눌러도 손해가 작다: 다시 출발하면 새 운행이 자동으로 시작된다(F-02). 대신 짧은 주차(드라이브스루·승객 하차)마다 운행이 나뉘어 목적 확인이 늘 수 있다 — 사각지대 B-1 |
| 버튼 | 왼쪽·위 **[기록 종료]**(`PrimaryButton` — 기다리지 않고 바로 끝내기) · 오른쪽·아래 [계속 기록](`SecondaryButton`) |
| 결과 | 시간 만료 또는 [기록 종료] → `finalize()` → PendingReview → ReviewTrip으로 이동(앱이 보이고 있을 때). [계속 기록] → `resume()` → RecordingParked. 다시 출발하면 앱 A가 StopCandidate를 취소한다(Stop만 기록) |
| 시계 | 남은 시간은 `deadlineAt − clock.now()`. 화면이 별도 타이머를 돌리지 않는다(앱 A가 유일한 시계). 1초마다 숫자만 교체 |
| 모션 | 주차 상태이므로 진행 막대의 선형 감소는 허용(상태 설명용, Pleos 애니메이션 원칙의 Linear). 카드 등장은 scrim 투명도 + 카드 0.94→1 배율(`motion.standard`) |
| 다른 종료 신호 | `NO_MOVEMENT`(멈춤 지속)·`NAVI_ARRIVED`(내비 도착)도 같은 카드를 쓰되, 정차(PEEK) 중이면 버튼 없이 Peek ⑤ 줄로만 알린다 |
| 수용 기준 | P 전환 후 아무 조작 없이 5초가 지나면 PendingReview. [계속 기록] 1탭으로 취소. [기록 종료] 1탭으로 즉시 종료 |

- **앱 안 알림 없음**(D-26): 알림 띠·알림 목록을 두지 않는다. 알릴 일은 앱 A의 시스템 알림만, 고칠 일은 사용자가 [운행 기록 조회]에서 고친다.
- **놓친 운행 팝업**(F-10, D-26): 운행이 없고(대기) 앱이 꺼진 동안 계기판 거리가 늘었으면 Home 위에 `ModalCard` 한 번 — 제목 "놓친 운행" · "앱이 꺼진 동안 15.0 km를 달렸어요"(`type.title`) · 하단 [운행으로 추가하기](`PrimaryButton`) · 오른쪽 위 X = 무시(접근성 이름 "무시"). 추가된 운행은 목적이 비어 있다(운행 기록 조회에서 입력). 기록 중에는 묻지 않는다
- **Odometer 미지원·실패**: 기록 중 "주행 거리" 칸에 "거리 미확정"(`label`, `textSecondary`) + 아래 "계기판 거리를 읽지 못하고 있어요"(`caption`) — 이유는 그 칸 안에서만 말한다. 거리 없이 시작 가능. **추정값을 그리지 않는다.**
- **레이아웃**
  - 공통: 맨 위 상태 줄(점 + 글자, 높이 76 dp, 오른쪽은 운전자 배지 — 7.1.1) / 가운데 두 섹션(같은 `SectionHeader`, 내용 위쪽 정렬) / 맨 아래 타일 4개(`DockTile`: 아이콘 네모 80 dp + 아래 이름, 4칸 등분 한 줄).
  - WIDE: 가운데 = 왼쪽 값 50% · 오른쪽 맥락 50%. **소제목은 위에 나란히 고정**하고, 내용은 그 아래 남는 높이의 **세로 가운데**(2026-11-09 사용자 결정 — 짧은 거리·경로가 소제목 밑에 붙어 위로 쏠리지 않게). 길면(최근 운행 3건) 칸을 다 쓰고 그 칸만 스크롤 + 가장자리 흐림. 섹션 내용은 `homeSections`가, 창 크기별 배치는 `HomeScreen`이 정한다.
  - NARROW·TALL: 위아래로 쌓는다 — 값 섹션 → 맥락 섹션. 타일 이름은 폭이 모자라면 두 줄.
  - 타일 순서는 `driverSide`를 따른다(우핸들 시장이면 좌우가 뒤집힌다).
- 수용 기준: 출발(BACKGROUND) 2초 안에 앱이 물러나고 보조 화면이 닫힌다. 기어 P 후 Home(자동 종료 카드)과 시스템 알림에 같은 카운트다운이 보인다. 거리 표시(Peek·RecordingParked)는 Odometer 18,420.5 → 18,443.9 변경 시 2초 안에 "23.4".

### 7.3 ReviewTrip — 목적 확인 (F-06, F-08, F-09, F-17) · 주차 전용

| 항목 | 내용 |
|---|---|
| 진입 | 운행 종료(PendingReview) 자동 진입, 시스템 알림 탭 딥링크, History 미확정 행(목적을 나중에 고치는 곳은 여기 하나 — D-26) |
| 상단 요약 | `09:00 → 09:35 · 23.4 km` / 출발 → 도착 라벨. 도착지가 없으면 후보 선택(최근 목적지 3 → 등록 장소 → 직접 입력) |
| 추천 | `Suggestion.isConfident`(≥ 0.8)면 **큰 추천 버튼 1개** "거래처 방문으로 기록"(`button.primary.xl`) + 아래 "다른 목적" 펼침. 아니면 추천 없이 선택지를 먼저 보여 준다(기획: 확신이 낮으면 반드시 묻는다) |
| 선택지 | `PurposeCategory` 3그룹: 일반 업무용(거래처 방문·외근·출장·회의·지점 이동) / 출퇴근용(출근·퇴근) / 비업무용(개인 이동) + "나중에" |
| 음성 | `VoiceRepository.available`이면 마이크 버튼(`button.icon.xl`). 듣는 중: 파형 대신 **정적** "듣고 있어요…" 표시(최대 8초). 결과: 인식 원문을 보여 주고 매칭된 목적을 선택 상태로 → [저장]. 실패 → "다시 말하기" · 탭으로 선택 |
| 확정 후 | `shouldProposePlace`면 시트 "이곳을 거래처로 등록할까요?" 라벨 입력(프리셋: 내비 목적지명) → [등록] · [아니요] |
| 수용 기준 | 1탭 확정(CONFIRMED). 음성 "판교 A고객사 미팅" → 거래처 방문 + note. 주행 시작 시 2초 안에 닫힘 |

### 7.4 ReceiptQr — 주유 기록 추가의 기본 경로 (F-30, D-20) · 주차 전용

| 항목 | 내용 |
|---|---|
| 진입 | Home 보조 동작 [주유 기록 추가]. 아직 확정하지 않은 영수증이 inbox에 있으면 QR 대신 **가장 최근 영수증의 7.5 입력 창으로 바로** 연다(앱 안 알림이 없으므로 — D-26) |
| 구성 (위→아래) | ① QR(ZXing, 화면 짧은 변의 45%, 흰 바탕 여백 4모듈) — `ReceiptRepository.issueQr()` ② 그 아래 작은 글씨(`type.caption`, `textSecondary`) "스마트폰으로 QR 코드를 인식한 뒤 안내에 따라 영수증을 업로드해주세요" ③ 남은 시간 `9:41`(1초마다 숫자만 교체) ④ 그 아래 **[직접 입력하기]**(`SecondaryButton`) → 7.5 입력 창을 빈 칸으로 |
| 위계 | QR이 주인공(기본 경로), 직접 입력은 보조 분기(부가 기능)라 한 단계 낮은 버튼으로 맨 아래 |
| 영수증 도착 | 열려 있는 동안 10초마다 inbox 확인(앱 A `InboxSyncWorker`, 화면은 `receiptInbox` 구독). 새 영수증이 오면 **자동으로** 7.5 입력 창으로 바뀐다 — OCR 값이 채워진 채로(`FuelEntry(receiptId)`, 백스택에서 QR을 대체) |
| 만료·실패 | 만료 → [새 QR]. 실패 → "서버에 연결하지 못했어요. 네트워크를 확인해 주세요" + [다시 시도]. 실패해도 [직접 입력하기]는 그대로 쓸 수 있다 |
| 수용 기준 | [주유 기록 추가] 1탭으로 QR. 폰 업로드 후 손대지 않아도 입력 창으로 넘어간다. [직접 입력하기] 1탭으로 빈 입력 창 |

### 7.5 FuelEntry — 주유 입력 창 하나 (F-19, F-18, D-20) · 주차 전용

영수증으로 채우든 손으로 채우든 **같은 창, 같은 필드 순서, 같은 [추가하기]**다. 다른 점은 처음에 칸이 채워져 있는지와 영수증 전용 정보(썸네일·신뢰도·검산)가 보이는지뿐이다.

| 항목 | 내용 |
|---|---|
| 진입 | ① QR 화면에서 영수증 도착(자동, `receiptId` 있음) ② [주유 기록 추가] 때 inbox에 이미 도착한 영수증(가장 최근 것, 7.4 진입) ③ QR 화면 [직접 입력하기](`receiptId` 없음, 빈 칸) ④ 주유소 주차 알림 "주유하셨나요?"(빈 칸) |
| 필드 (공통) | 금액(원, 필수) · 수량(L) · 단가(원/L) · 연결 운행. 숫자 칸은 탭 → `NumericKeypad` 시트(큰 키 96 dp) |
| 영수증에서 왔을 때 더 | 썸네일(Coil, 탭하면 확대) · 상호 · 일시 · 유종 · 할인 · 카드 끝 4자리. 각 행 `ConfidenceRow` 신뢰도: 초록(≥ 0.95) / 노랑(0.75–0.95) / 빨강(< 0.75), **색만으로 구분하지 않는다**(노랑·빨강은 "확인 필요" 글자와 테두리). 검산 배지: 통과 "금액 검산 통과" / 실패 "금액이 맞지 않아요" → [추가하기] 비활성 |
| 연결 운행 | 영수증: 후보 ≤ 3개(`TripCandidate.label`, 주유소 정차 배지) + "연결 안 함". 직접 입력: 지금(또는 방금 끝난) 운행이 기본 선택 |
| 버튼 | 맨 아래 **[추가하기]**(`PrimaryButton`). 누르면 주유 기록이 완전히 들어간다. 금액이 없거나 검산 실패면 비활성 |
| 자동 | 영수증 모두 초록 + 검산 통과 + 후보 1개 → 후보가 선택된 상태로 열리고 [추가하기] 1탭으로 끝 |
| 저장 경로 | 화면은 `FuelDraft`(출처 = 영수증 id 또는 직접 입력, 필드, 연결 운행) 하나를 `ReceiptRepository`에 넘긴다. 영수증이면 `ReceiptApi.confirm(receipt, tripId, edits)`, 직접 입력이면 `ReceiptApi.saveFuel(tripId, VoiceFuel(금액, 리터, 단가), source = MANUAL)`. `Rejected` → 사유 한 줄 + 해당 칸 강조 |
| OCR 실패 | `ocrStatus=FAILED` → 썸네일 + 빈 칸. 금액만 넣어도 추가 가능 |
| 계약 공백 | 직접 입력은 지금 계약(`VoiceFuel`)상 금액·리터·단가만 저장된다. 상호·유종도 손으로 받으려면 앱 B `saveFuel` 확장이 필요하다 — R-8. 그 전까지 직접 입력 창에는 공통 필드만 보인다 |
| 마이크 | 이번 범위에서 뺀다(D-20). `VoiceApi.listenFuel`은 계약에 남아 있어 나중에 칸 채우기 보조로 붙일 수 있다 |
| 레이아웃 | WIDE: 좌 썸네일 / 우 필드(직접 입력이면 필드만 가운데, 최대 폭 `sheetMaxWidth`) · NARROW/TALL: 썸네일 상단 1/3 + 필드. [추가하기]는 항상 하단 고정 |
| 수용 기준 | 영수증 경로: 업로드 후 [추가하기] 1탭(자동 조건일 때). 검산 실패 시 비활성. 직접 입력: 금액 없으면 비활성. 카드 16자리가 화면·로그 어디에도 없다 |

### 7.7 Report — 운행기록부·보내기 (F-21, F-22) · 주차 전용

| 단계 | 내용 |
|---|---|
| 기간 | `◀ 2026년 11월 ▶` (미래 달 비활성) |
| 미리보기 | 요약 카드: 운행 N건 · 총 X km · 업무 사용 비율 · 검토 필요 N건(탭하면 History 필터) |
| 생성 | [운행기록부 만들기] → `ReportRepository.build(month)` → 진행 표시(정차 중 `motion.loading`, 5초 목표) → 완료 |
| 보내기 — 메일 | 수신자 체크 목록(서버 배포분 먼저) → [보내기]. SMTP 미설정이면 [설정에서 메일 계정 연결] |
| 보내기 — 사내 서버 | 사내 서버가 연결돼 있으면 "사내 서버로도 자동 전송됩니다 · 마지막 전송 10:24" 한 줄 (8장) |
| 결과 | 성공 "보냈어요 · 3명" / 실패 `MailError`별 문구 + [다시 시도] / 오프라인 "전송 대기 중 — 연결되면 보내요" |
| 수용 기준 | 화면 진입 후 탭 2번(만들기 → 보내기)으로 발송 |

### 7.8 History — 운행 기록 (F-12) · 주차 전용

- Home 독 [운행 기록]으로 들어온다(D-23). 상단에 **[운행기록부 만들기·보내기]** → 7.7 Report. Home에는 기록부 버튼이 따로 없다.

- 이번 달 목록(월 이동 가능). 행: 날짜·시각 · 운전자 · 출발 → 도착 · 거리 · 목적 · 상태 배지(확정/확인 필요/운전자 미확정/거리 미확정) · 사내 서버 전송 배지(8장).
- **운전자 미확정 행**(F-20): 행 안에 후보 아바타(최근 운전자 최대 3명)를 두고 1탭으로 지정(`SessionRepository.assignDriver`). 선택 화면에서 옮겨 왔다 — D-19.
- 미확정 행 탭 → ReviewTrip. 상단 합계: 건수 · km(엑셀 요약과 같은 계산 — `TripRepository`가 계산, 화면은 표시만).
- 긴 목록은 `LazyColumn`. 자동 스크롤 없음.

### 7.9 Settings (F-24, F-01, 8장) · 주차 전용

섹션 순서(위에서 아래):

1. **차량 연결** — 상태(연결됨 · 차량 12가3456 / 연결 안 됨), [연결 코드 입력](6자리 숫자 키패드), [재등록].
2. **사내 서버** — 서버 주소(프리셋: 마지막 주소, `https://` 필수, 개발용 `http://10.0.2.2:8080` 허용), 연결 상태, 전송 대기 N건 · 마지막 전송 시각, [지금 전송]. 8장.
3. **메일** — 발신 계정(SMTP 호스트·포트·아이디·앱 비밀번호), [연결 확인]. 비밀번호 필드는 항상 가림.
4. **수신자** — 서버 배포분(잠금 아이콘) + 직접 추가분(삭제 가능), [추가].
5. **직원** — 목록 + [추가](이름·부서·이메일).
6. **자동화** — 월말 운행기록부 자동 생성·발송 스위치.
7. **동기화** — [지금 동기화], 마지막 동기화 결과.
8. **정보** — 버전, 오픈소스 라이선스(Asta Sans OFL 포함), 데모 모드 표시(demo flavor).

- 텍스트 입력이 많은 화면이다. 플랫폼 키보드 규제(`RestrictionFlags.noKeyboard`)가 켜지면 입력 필드를 비활성화하고 "지금은 입력할 수 없어요"를 보인다.
- WIDE: 좌측 섹션 목록 + 우측 상세(마스터–디테일). NARROW/TALL: 섹션 목록 → 상세 push.

---

## 8. 사내 서버 자동 취합 (기능명세 ②)

### 8.1 개념

- "GitLab 방식" = **AutoLog 서버 소프트웨어(relay + 운영 웹)를 회사가 자기 서버에 설치해 운영**한다. 우리는 배포판(docker compose, 이미지, 설치 문서)을 제공하고 회사 데이터는 회사 서버에만 있다.
- 차량은 운행 원본을 Room에 두고, **사본**을 회사 서버로 올린다. 서버가 꺼져도 차의 기록·엑셀·메일은 그대로 동작한다(원칙 유지).
- 운영 웹에서 회사 전체 차량·운전자의 운행·비용을 자동으로 모아 본다.

### 8.2 프론트가 만드는 것

| 위치 | 화면 요소 | 데이터 |
|---|---|---|
| 차량 앱 Settings | 서버 주소 · 연결 코드 등록 · 연결 상태 · 전송 대기 N건 · 마지막 전송 · [지금 전송] | `SettingsRepository`(DeviceApi), `UploadRepository` |
| 차량 앱 History | 행마다 전송 배지: 대기(회색 점) · 완료(체크) · 실패(경고 + 사유) | `UploadRepository` |
| 차량 앱 Report | 사내 서버 연결 시 안내 한 줄 | `UploadRepository` |
| 운영 웹 | 운행 · 비용 · 월별 취합(전체 차량 합산, 조직 엑셀 다운로드) 3화면 (9.1) | 업로드 API (R-4) |

### 8.3 선행 조건 (A·B 몫, 12장)

- 차 → 서버 운행 업로드 계약(`UploadApi`)과 API(`POST /v1/trips`, 멱등, trip_id 기준).
- vehicle-core의 서버 주소가 `BuildConfig.RELAY_BASE_URL` 고정값이다. **런타임 설정**으로 바꿔야 회사마다 다른 주소를 쓸 수 있다.
- 운영 웹 조회 API(`GET /admin/trips`, `/admin/expenses`, `/admin/reports/summary`, 엑셀 다운로드).

### 8.4 일정

D-12에 따라 11.15 이후. API가 오기 전에는 interim `FakeUploadApi`와 운영 웹 mock 데이터로 화면을 완성한다.

---

## 9. 웹

### 9.1 운영 웹 (`web/admin`)

| 항목 | 값 |
|---|---|
| 스택 | React 18.3.1 · TypeScript 5.6 · Vite 6 · Tailwind 3.4 · TanStack Query 5 · react-router-dom 6 · Vitest + Testing Library · ESLint |
| 배포 경로 | Vite `base: '/admin/'`. API는 같은 오리진 `/admin/api/*`(nginx가 relay `/admin/*`로 프록시) |
| 인증 | 첫 화면에서 관리자 토큰 입력 → `sessionStorage` 저장 → 모든 요청에 `X-Admin-Token`. 403이면 토큰 화면으로 |
| 디자인 | `design-language.md`의 같은 토큰·서체(Asta Sans, `@fontsource-variable/asta-sans` self-host). 사무실 환경이므로 **라이트 기본** + 다크 전환 |
| 타입 | relay에 OpenAPI가 없다. `src/api/types.ts`에 relay DTO를 손으로 옮기고, 각 타입 위에 원본 파일 경로를 주석으로 단다(예: `// relay/admin/AdminVehicleController.kt CreateVehicleRequest`) |

| 화면 | 기능 ID | API | 시점 |
|---|---|---|---|
| 차량 | F-50 | `GET/POST /admin/vehicles`, `POST /{id}/connect-code`, `DELETE /{id}/device` | 11.13–14 |
| 직원 | F-51 | `/admin/users` CRUD + CSV import | 11.16–23 |
| 장소 | F-52 | `/admin/places` CRUD + CSV (주소 → 좌표는 서버 지오코딩) | 11.16–23 |
| 배차 | F-53 | `/admin/schedules` CRUD + CSV. **주간 그리드**(행=차량, 열=요일, 직접 구현) | 11.16–23 |
| 수신자 | F-54 | `/admin/recipients` CRUD + CSV | 11.16–23 |
| 운행 | 기능명세 ② | `GET /admin/trips`(R-4, mock 선행) | 11.16–30 |
| 비용 | 기능명세 ② | `GET /admin/expenses`(R-4, mock 선행) | 11.16–30 |
| 월별 취합 | 기능명세 ② | `GET /admin/reports/summary`, 엑셀 다운로드(R-4, mock 선행) | 11.16–30 |

- 구조: `src/api/`(fetch 래퍼·타입·query 훅) · `src/pages/<화면>/` · `src/components/` · `src/mocks/`(API 전 화면용).
- 에러 응답 `{code, message}`를 그대로 보여 주지 않고 사람 문장으로 바꾼다(`CSV_INVALID` → "CSV 3번째 줄: 이메일 형식이 아니에요").

### 9.2 카메라 페이지 (`web/camera/index.html`)

| 단계 | 화면 |
|---|---|
| 열림 | URL `/r/{vehicleCode}?t={token}`. 큰 버튼 "영수증 찍기"(`<input type="file" accept="image/*" capture="environment">`) |
| 촬영 후 | 미리보기 + [보내기] · [다시 찍기] |
| 전송 | canvas로 긴 변 2000 px, JPEG 품질 0.85로 축소 → `POST /r/{code}/receipt?t=` multipart 필드 `image` → 진행 막대 |
| 완료 | "차량으로 보냈어요. 차 화면에서 확인하세요" |
| 오류 | 토큰 만료·사용됨(4xx) "QR이 만료됐어요. 차에서 새 QR을 띄워 주세요" / 중복(409) "이미 보낸 영수증이에요" / 네트워크 "전송하지 못했어요" + [다시 보내기] |

- 외부 요청 0(웹폰트·분석 스크립트 없음). 서체는 시스템(`-apple-system, "Apple SD Gothic Neo", "Noto Sans KR", sans-serif`). 색은 `design-language.md` 라이트 토큰.
- 파일 하나에 HTML·CSS·JS 인라인. 빌드 단계 없음.

### 9.3 배포 연결

Backend `ops/docker-compose.yml`의 nginx는 지금 `../web/dist`와 `../web/camera`를 마운트한다. 이 리포로 옮긴 뒤에는 Frontend 산출물 경로를 마운트하도록 Backend에 PR을 올린다(R-5).

그 전까지는 **`backend/`를 건드리지 않고** compose override 파일로 nginx 마운트만 바꾼다.

```yaml
# ops/compose.frontend.yml — 상대 경로는 첫 번째 compose 파일 위치(backend/ops) 기준
services:
  nginx:
    volumes:
      - ../../web/admin/dist:/usr/share/nginx/html/web/admin:ro
      - ../../web/camera:/usr/share/nginx/html/camera:ro
```

```bash
(cd web/admin && npm run build)
docker compose -f backend/ops/docker-compose.yml -f ops/compose.frontend.yml --env-file backend/ops/.env up -d
```

Compose는 같은 컨테이너 경로의 볼륨을 override 쪽으로 바꾼다. 동작 여부는 V-10으로 확인한다.

---

## 10. 품질 게이트

### 10.1 PR마다 (CI와 같은 명령)

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck :vehicle-app:testDemoDebugUnitTest :vehicle-app:verifyRoborazziDemoDebug :vehicle-app:assembleDemoDebug
cd web/admin && npm run lint && npm test -- --run && npm run build
```

### 10.2 테스트 종류와 기준

| 대상 | 무엇을 | 도구 |
|---|---|---|
| ViewModel | 상태 전이(Repository Fake로), 잠금 시 Action 무시, 오류 문구 매핑, AutoEnd 남은 시간 계산(`deadlineAt − now`, 가짜 Clock) | JUnit4 + coroutines-test + Turbine |
| `UsageTier` 판정 | 6.9 표의 모든 조합: motion(PARKED·STOPPED·MOVING·null) × carUx(DO 필요·불필요·연결 실패) → 기대 tier | JUnit4 |
| Navigator | tier 하락 → `[Home]`로 축소, FULL이 아니면 push 무시, 딥링크 | JUnit4 |
| 조작 예산 | 1.2 표의 작업별 탭 수를 UI 테스트로 센다(운전자 선택 1, P 자동 종료 0, 목적 확정 1, 영수증 1, 발송 2) | Compose UI test on Robolectric |
| `windowLayoutOf` | 경계값(aspect 1.25·0.70, width 840) | JUnit4 |
| 디자인 토큰 | **모든 텍스트 색 × 배경 색 조합의 대비 ≥ 4.5:1**, 주 버튼 위 텍스트 ≥ 4.5:1, 최소 터치 크기 상수 ≥ 76 dp, 본문 최소 28 sp | JUnit4 (`ContrastTest`) |
| 화면 스크린샷 | 화면별 {WIDE, NARROW, TALL} × {dark, light}. Home은 {Ready, RecordingParked, AutoEnding, NeedsRecovery}, Peek은 {기록 중, 자동 종료 대기, 운행 없음} 추가 | Roborazzi(`@GraphicsMode(NATIVE)`, `@Config(qualifiers=…)`) |
| 잠금 UI | PEEK이면 클릭 가능한 노드 0개, FULL→PEEK 전환 시 시트·다이얼로그 닫힘, BACKGROUND에서 `moveTaskToBack` 호출 | Compose UI test on Robolectric |
| 웹 | 폼 검증, API 오류 문구, 토큰 만료 흐름 | Vitest + Testing Library |

**Roborazzi 잠정 크기 [검증 필요 V-01]**: WIDE `w1024dp-h640dp-xhdpi`, NARROW `w504dp-h640dp-xhdpi`, TALL `w720dp-h1100dp-xhdpi`. FormFactorConsole 측정 후 실제 프리셋 값으로 바꾼다.

### 10.3 맥 에뮬레이터 수동 점검 (마일스톤마다)

`docs/verification.md`에 날짜·환경과 함께 기록한다.

1. 콜드 부팅 → 앱 실행 → ProfilePicker → Home까지 끊김 없음
2. 윈도 바로 portrait ↔ landscape 전환, 다른 앱과 1:1 분할 → 잘림·겹침 없음, 상태 유지
3. 주간·야간 전환 → 깜빡임 없이 테마 변경
4. 사용 3단계(VHAL 기어·속도 주입 또는 데모 패널): 주차(P)에서 설정 화면 열기 → 기어 D·정차 → 2초 안에 Peek로 바뀌고 입력 요소 없음 → 출발 → 2초 안에 앱이 물러나고 내비가 그 창을 채움 → 다시 정차 → 앱을 열면 Peek
5. 기어 P 자동 종료: 운행 중 P → 헤즈업 알림과 앱 시트에 같은 카운트다운 → 아무것도 누르지 않으면 만료 시 ReviewTrip / [계속 기록] 1탭이면 계속 / [기록 종료] 1탭이면 즉시 종료
6. 글자 크기: 주요 정보 높이 ≥ 5 mm, 보조 ≥ 3 mm(화면 실측 또는 DPI로 환산)
7. 시연 6장면(Backend 기능명세 9장) 재생

### 10.4 완료 정의

기능 하나가 "완료"이려면: 화면 3레이아웃 × 2테마 스크린샷을 로컬에 기록해 확인했고(현재 커밋하지 않음), ViewModel 테스트가 있고, 사용 단계(FULL 전용·PEEK·BACKGROUND) 규칙이 적용돼 있고, 1.2 조작 예산 안에 있고, `design-language.md` 13장 체크리스트를 통과하고, 맥 에뮬레이터에서 한 번 실행해 봤다(못 했으면 PR에 적는다).

---

## 11. 일정과 마일스톤

| 기간 | 할 일 | 통과 기준 |
|---|---|---|
| **11.09–10** | 리포 골격, submodule, composite build 스파이크, CI, `ui/design` 토큰·테마·서체, Navigator, `VehicleUsageRepository`(사용 3단계), interim 계약·Fake, 데모 패널 뼈대. A·B에 R-1~R-4·R-7 공유 | `assembleDemoDebug`·테스트 통과, 맥 Pleos 에뮬레이터 실행 |
| **11.10–11 디자인 게이트** | ProfilePicker · Home(4상태) · AutoEndPrompt · Peek 완성 → Roborazzi PNG(3레이아웃 × 2테마)로 디자인 리뷰 → 맥에서 FormFactorConsole 프리셋 측정(V-01), 사용 3단계 점검(V-11·V-12), 기어·시동 신호 확인(V-15·V-16) | 사용자 디자인 승인 |
| 11.11–12 | ReviewTrip(탭·음성·거래처 등록) | 시연 장면 1~3을 demo로 재생 |
| **11.13–14** | ReceiptQr·FuelEntry(주유 입력 창 하나, D-20) · Report·보내기 · Settings(연결·메일·수신자·직원) · 카메라 페이지 · 운영 웹 차량 화면(F-50) → **11.14 통합**(B 실구현 연결, A는 준비된 만큼) | 장면 5·6 재생, 영상·APK |
| 11.15 | 1차 제출 | |
| 11.16–23 | 운영 웹 직원·장소·배차·수신자 · History · 놓친 운행·복구 다이얼로그 · interim → A 실계약 교체 · 사내 서버 설정·전송 상태(Fake) · 운영 웹 운행·비용·월별 취합(mock) | 직원 추가가 1분 안에 ProfilePicker에 반영 |
| 11.24–30 | 업로드 실API 연결 · 맥 에뮬레이터 QA(10.3) · 6장면 리허설 · Backend 정리 PR(R-5, R-6) | P0 완료 정의 |
| 12.01–02 | 시연 리허설, 버그 수정만 | |

---

## 12. 앱 A·B·Backend에 요청할 것

| ID | 요청 | 누구 | 왜 | 언제까지 |
|---|---|---|---|---|
| R-1 | `vehicle-core`에 공개 팩토리 `AutoLog.create(context): AutoLogComponents`(모든 Contracts 구현을 조립해 돌려줌) | A·B | 앱은 `core`·`fake`만 import한다. 내부 클래스를 직접 생성할 수 없다 | 11.13 |
| R-2 | 앱 A 계약(`TripSessionApi`, `LocalStore`, `ReportApi`, `DeviceApi`) — 이 리포 6.6 초안을 출발점으로 | A | 화면 개발 기준. 다르면 프론트가 Repository만 고친다 | 11.12 |
| R-3 | 알림 딥링크 키 상수(`autolog.route`, `autolog.tripId`)를 `core`에 정의 | A | A의 알림과 프론트 Navigator가 같은 키를 써야 함 | 11.14 |
| R-4 | 사내 서버 취합: `UploadApi` 계약, `POST /v1/trips`, 서버 주소 런타임 설정, 운영 웹 조회 API(trips·expenses·summary·엑셀) | A·B | 기능명세 ② (8장) | 계약 11.20, API 11.27 |
| R-5 | Backend `ops/` nginx 마운트를 Frontend 산출물로 변경 | 공동 | 9.3 | 11.27 |
| R-6 | Backend `app/`·`web/` 삭제, CI `:app:assembleDebug` 제거, CONVENTIONS 1.2·기술명세 7·8장에 "화면은 AutoLog-Frontend" 반영 | 공동 | 같은 applicationId 앱이 두 곳에 있으면 혼란 | 11.30 |
| R-7 | 사용 3단계·자동 종료 지원: ① `TripSessionApi.motion`(PARKED·STOPPED·MOVING, 판정 불가 null) ② `StopCandidate(reason, deadlineAt)` ③ 기어 P 즉시 StopCandidate(`deadlineAt` = P 시각 + **5초**) + Odometer 저장. 기어·시동 판정은 1.1.1 우선순위(Pleos Gear → AAOS `GEAR_SELECTION`/`CURRENT_GEAR` → `PARKING_BRAKE_ON` → `IGNITION_STATE` → 최후 규칙: 다음 시동 때 자동 확정, 거리 불변 60초 = 주차). `CAR_POWERTRAIN` 권한은 vehicle-core 매니페스트에 선언 ④ `TripRecordingService` 헤즈업 알림: 제목 "기록을 마칠까요?", 본문 거리, 카운트다운(`setUsesChronometer(true)`·`setChronometerCountDown(true)`·`setWhen(deadlineAt)`), 액션 [계속 기록]=`resume()` · [기록 종료]=`finalize()` ⑤ 주행 중 지속 알림 "기록 중 · 12.3 km · 목적지"(F-03 대체) | A | 1.1·7.2.1. 주행 중 앱 화면이 없으므로 P 순간에 사람이 보는 것은 알림이다 | 계약 11.12, 알림 11.14 |
| R-8 | 직접 입력 주유 기록에 상호·유종 저장: `ReceiptApi.saveFuel`이 `VoiceFuel`(금액·리터·단가)만 받는다. 직접 입력 창(7.5)이 영수증 창과 같은 필드를 받으려면 상호(`station`)·유종(`fuelType`)·일시를 선택 인자로 추가 | B | 7.5, D-20. 없어도 동작한다(공통 필드만) | 11.13 |
| R-9 | 운행 출발지 이름: `LiveTrip.originLabel`(등록 장소와 가까우면 그 이름, 아니면 동 단위 주소, 모르면 null). 지금은 interim에 프론트가 추가해 둠 | A | 7.0 Peek 경로(출발지 → 목적지) | 11.13 |

---

## 13. 검증 필요 목록

결과는 `docs/verification.md`에 날짜·환경(에뮬레이터/실차)과 함께 적고, 이 표의 상태를 갱신한다.

| ID | 항목 | 영향 | 확인 방법 | 상태 |
|---|---|---|---|---|
| V-01 | FormFactorConsole 프리셋별 앱 창 dp·DPI·px | 6.8 임계값, 10.2 스크린샷 크기, 글자 mm 환산 | 맥 에뮬레이터 + Host Display Bridge | 대기 |
| V-02 | composite build `dependencySubstitution` 동작 | 5.3 | `assembleDemoDebug` | 대기 |
| V-03 | DO 선언 방식(`distractionOptimized` meta-data)이 Pleos에서 유효한지 | 5.4 | VHAL로 기어 D·속도 주입 후 앱 상태 | 대기 |
| V-04 | `CarUxRestrictionsManager`를 일반 앱에서 얻을 수 있는지(권한), Pleos 에뮬레이터에서 값이 바뀌는지 | 6.9 | 로그 | 대기 |
| V-05 | Asta Sans의 고정폭 숫자(`tnum`) 지원 여부 | 거리 숫자 흔들림 | Roborazzi로 `fontFeatureSettings="tnum"` 비교 | 대기 |
| V-06 | 1 sp가 Pleos 디스플레이에서 몇 mm인지 → `type.title` 36 sp가 5 mm 이상인지 | 타이포 최소값 | V-01 DPI로 계산 + 실측 | 대기 |
| V-07 | 헤드유닛 햅틱 지원 여부(`HapticFeedbackConstants.CONFIRM`) | 눌림 피드백 | 실차(불가 시 생략) | 대기 |
| V-08 | 시스템 야간 모드 전환 시 `uiMode` configChange로 재생성 없이 테마가 바뀌는지 | 5.4 | 에뮬레이터 차량 설정 | 대기 |
| V-09 | Roborazzi + Robolectric 4.14가 Compose BOM 2024.12.01·SDK 35에서 Asta Sans를 정상 렌더링하는지 | D-13 | `recordRoborazziDemoDebug` | 대기 |
| V-10 | compose override(`ops/compose.frontend.yml`)가 nginx 마운트를 바꾸는지 | 9.3 | `docker compose … config`로 병합 결과 확인 후 `/admin/` 접속 | 대기 |
| V-11 | Pleos가 **정차(IDLING, 기어 ≠ P·속도 0)** 중 DO 미선언 앱을 막는지 | 5.4 DO 결정 규칙, Peek | DO 선언 뺀 빌드로 기어 D·속도 0 주입 | 대기 |
| V-12 | Pleos 멀티 윈도우에서 `moveTaskToBack(true)` 후 그 창을 다른 앱(내비)이 채우는지 | 6.9 BACKGROUND | 1:1 분할 상태에서 주행 주입 | 대기 |
| V-13 | 헤즈업 알림의 카운트다운(`setChronometerCountDown`)과 액션 버튼이 Pleos 알림 UI에 보이는지 | 7.2.1, R-7 | 앱 A 알림으로 확인 | 대기 |
| V-14 | Pleos Gear SDK(`registerGearSelection`)가 에뮬레이터 VHAL `GEAR_SELECTION` 주입에 반응하는지(Backend sdk-findings #5). 시그니처는 AAR로 확인됨 — DrivingMode는 기어가 아님 | 1.1.1 1순위 | Backend 검증 결과 확인 | 대기 |
| V-15 | Pleos에서 AAOS `GEAR_SELECTION`·`CURRENT_GEAR`·`PARKING_BRAKE_ON`·`IGNITION_STATE`가 읽히는지(권한 `CAR_POWERTRAIN`), 에뮬레이터 VHAL로 값이 바뀌는지 | 1.1.1 2~4순위 | `CarPropertyManager.getProperty`·`registerCallback` 로그 | 대기 |
| V-16 | 시동 OFF→ON 뒤 앱 프로세스가 살아 있는지 / 부팅 후 서비스를 다시 띄울 수 있는지(Backend sdk-findings #4b) | 1.1.1 최후 규칙의 처리 시점 | 에뮬레이터 전원 OFF→ON | 대기 |
| V-17 | 정차(PEEK, CarUxRestrictions 적용 중)에 Peek 경로 화살표의 반복 투명도 애니메이션이 Pleos 운전자 주의 분산 기준에 맞는지(안 되면 정차 중엔 정지 그림, `MotionTokens.ambient` 한 곳만 바꾸면 됨) | 7.0 | 맥 에뮬레이터 + Pleos 가이드 확인 | 대기 |


### 13.1 알고 받아들인 사각지대

설계상 알면서 받아들인 약점이다. 시연·심사 Q&A에서 이 표로 답한다. 해소하려면 결정 표(4장)를 고친다.

| ID | 사각지대 | 왜 받아들였나 | 나중에 줄이는 방법 |
|---|---|---|---|
| B-1 | 기어 P 5초 자동 종료 때문에 짧은 주차(드라이브스루, 승객 하차, 주유)마다 운행이 나뉘고, 나뉜 운행마다 목적 확인이 생긴다 | 운행 종료 0탭이 더 자주, 더 확실하게 이득이다. 다시 출발하면 새 운행이 자동으로 시작되므로 기록이 빠지지는 않는다 | 같은 운전자·같은 장소에서 N분 안에 이어진 운행은 앞 운행의 목적을 추천 1순위로(PurposeEngine, 앱 B) 또는 이어 붙이기 제안 |
| B-2 | 기어·주차 브레이크·시동 신호가 모두 없으면 '거리 불변 60초'를 주차로 보므로, 60초가 넘는 신호 대기에서 전체 앱이 열릴 수 있다 | 신호가 없을 때 전체 앱을 영영 못 쓰는 것보다 낫다. Pleos 정책은 움직이는 주행만 막는다 | V-15에서 2~3순위 신호가 확인되면 이 규칙은 쓰이지 않는다 |

---

## 14. 용어

| 용어 | 뜻 |
|---|---|
| 계약(Contracts) | `backend/vehicle-core/.../core/Contracts.kt`의 인터페이스. 화면과 로직의 경계 |
| interim | 앱 A 계약이 오기 전 이 리포가 임시로 정한 인터페이스와 Fake |
| 주행 / 정차 / 주차 | 움직이는 중 / 기어 P가 아니고 멈춤(신호 대기 등) / 기어 P (1.1) |
| `UsageTier` | `BACKGROUND`(주행, 화면 없음) · `PEEK`(정차, 읽기 전용 패널) · `FULL`(주차, 전체 앱) |
| Peek | 정차 중에만 보이는 읽기 전용 상태 패널(7.0) |
| AutoEndPrompt | 기어 P 후 카운트다운이 끝나면 기록이 자동 종료되는 안내(7.2.1) |
| 조작 예산 | 작업별 허용 탭 수(1.2) |
| 잠금(locked) | `tier != FULL`. 입력을 받지 않는 상태 |
| WIDE / NARROW / TALL | 앱 창 크기에 따른 레이아웃 3단계(6.8) |
| 주차 전용 화면 | FULL에서만 열리고, 단계가 내려가면 닫히는 화면(백스택의 모든 화면) |
| 데모 패널 | demo flavor 전용 상태 조작 시트(6.7) |
| 사내 서버 | 회사가 자체 호스팅하는 AutoLog 서버(relay + 운영 웹) |

---

## 15. 출처

- Pleos: [에뮬레이터 설치](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) · [에뮬레이터 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/configure-pleos-connect-emulator) · [폼팩터 테스트](https://document.pleos.ai/docs/pleos-only/app-test-guide/form-factor-test-guide) · [디스플레이 레이아웃](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) · [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) · [앱 구조·화면 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model) · [주행 규제 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy) · [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide) · [운전자 주의 분산 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) · [UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro) · [타이포그래피](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography) · [앱 심사 가이드](https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide)
- 서체: [Asta Sans 메타데이터(google/fonts, OFL, 42dot)](https://raw.githubusercontent.com/google/fonts/main/ofl/astasans/METADATA.pb) · [42dot/Asta-Sans](https://github.com/42dot/Asta-Sans)
- Backend: `backend/docs/AutoLog_기능명세.md`, `AutoLog_기술명세.md`, `CONVENTIONS.md`, `vehicle-core/.../core/Contracts.kt`, `ContractTypes.kt`, `vehicle-core/docs/sdk-findings.md`

---

## 16. 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| 1.0 | 2026-11-09 | 최초 확정 (사용 3단계·조작 최소화, 리포 구조, 스택, MVVM·Repository, 가변 레이아웃, 사용 단계 잠금, 화면 명세(Peek·AutoEndPrompt 포함), 사내 서버 취합, 웹, 품질 게이트, 일정) |
| 1.1 | 2026-11-09 | D-26 앱 안 알림 없음(시스템 알림만): Home 알림 띠·목록 삭제, 놓친 운행은 대기 Home 팝업, 목적 없는 운행은 운행 기록 조회에서 직접, 미확정 영수증은 [주유 기록 추가] 진입 때 바로 입력 창 |
| 1.2 | 2026-11-09 | 구조 점검: `SessionRepository`가 화면용 세션 타입을 내보내 `ui/`의 interim 의존 제거(6.5·6.6), Home·Peek 상태 계산을 `XxxReducer.kt`로 분리 |
