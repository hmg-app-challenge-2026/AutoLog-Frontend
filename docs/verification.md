# 검증 기록

스펙 13장 V-xx 결과. 날짜·환경(WSL/맥 에뮬레이터/실차)을 꼭 적는다.

| ID | 결과 | 환경 | 날짜 | 메모 |
|---|---|---|---|---|
| V-01 | | 맥 에뮬레이터 | | FormFactorConsole 프리셋별 dp·DPI·px |
| V-02 | 성공 — `includeBuild("backend") { dependencySubstitution { substitute(module("kr.autolog:vehicle-core")).using(project(":vehicle-core")) } }` 그대로 동작. 대안(include) 불필요 | WSL | 2026-11-09 | `:backend:vehicle-core` 태스크로 빌드됨 |
| V-03 | | 맥 에뮬레이터 | | |
| V-04 | | 맥 에뮬레이터 | | |
| V-05 | 지원 — Asta Sans GSUB 기능 목록에 `tnum` 있음(aalt·calt·ccmp·frac·fwid·liga·ordn·sinf·ss01·subs·sups·tnum·vert·vrt2) | WSL | 2026-11-09 | `fontFeatureSettings = "tnum"` 그대로 사용 |
| V-06 | | 맥 에뮬레이터 | | |
| V-07 | | 실차 | | |
| V-08 | | 맥 에뮬레이터 | | |
| V-09 | 정상 — Roborazzi 1.32.2 + Robolectric 4.14.1(SDK 34, NATIVE)에서 Asta Sans 한글·숫자 렌더링 확인(`components_tall_*.png`) | WSL | 2026-11-09 | **무한 애니메이션(CircularProgressIndicator 등)이 화면에 있으면 캡처 1장당 약 3분 대기** → 스크린샷에는 로딩 회전을 넣지 않는다 |
| V-10 | | WSL | | |
| V-11 | | 맥 에뮬레이터 | | |
| V-12 | | 맥 에뮬레이터 | | |
| V-13 | | 맥 에뮬레이터 | | |
| V-14 | | — | | Backend sdk-findings #5 |
| V-15 | | 맥 에뮬레이터 | | |
| V-16 | | 맥 에뮬레이터 | | |

## 구현 중 발견한 사실 (2026-11-09, WSL)

- **Robolectric에서 한국어 구문 줄바꿈(`LineBreak.WordBreak.Phrase`)이 적용되지 않는다** — 스크린샷에서 "달 / 렸어요"처럼 어절 중간이 끊긴다. 실기기(Android 13+)에서는 적용될 것으로 보이나 맥 에뮬레이터에서 확인 필요. 그동안은 끊기기 쉬운 문장을 나눠 설계한다.
- **Robolectric 기본 화면(320×470dp)에서는 큰 타일이 화면 밖으로 밀려 클릭 테스트가 실패한다** — 동작 테스트에 `@Config(qualifiers = "w1024dp-h640dp")`를 준다.
- **무한 애니메이션이 화면에 있으면 Roborazzi 캡처 1장당 약 3분** — 스크린샷에 로딩 회전을 넣지 않는다(V-09 메모).
