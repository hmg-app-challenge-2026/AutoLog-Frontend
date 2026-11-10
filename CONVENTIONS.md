# AutoLog Frontend 작업 규칙

> 이 리포에서 코드를 쓰거나 고치는 모든 사람(과 AI 도구)이 따른다.
> 설계는 [설계 스펙](docs/superpowers/specs/2026-11-09-autolog-frontend-design.md), 디자인은 [디자인 언어](docs/design-language.md)가 기준이다. 이 문서와 스펙이 다르면 **스펙을 따르고 이 문서를 고친다.**
> Backend의 [CONVENTIONS.md](backend/CONVENTIONS.md)와 같은 규칙은 그대로 따르고, 다른 점만 여기 적는다.

---

## 1. 소유와 경계

| 경로 | 소유 | 규칙 |
|---|---|---|
| `vehicle-app/` · `web/` · `docs/` | 프론트 | 이 리포에서 자유롭게 고친다 |
| `backend/` (submodule) | 앱 A·B | **이 리포에서 고치지 않는다.** 필요하면 Backend에 PR |
| `vehicle-app/.../data/interim/` | 프론트(임시) | 앱 A 계약이 머지되면 삭제한다. 새 코드가 interim 타입에 의존하지 않게 Repository로 감싼다 |

- `ai.pleos.*` import 금지(스펙 6.1).
- `ui/` 패키지는 `data/repository`의 인터페이스만 쓴다. `kr.autolog.vehicle.core`의 서비스 인터페이스를 import하지 않는다(값 enum은 가능).
- Backend 내부 패키지(`session`, `data`, `domain`, `navi` …) import 금지. `core`·`fake`·`shared`만.

## 2. 브랜치·커밋·PR

- 브랜치: **작업(계획의 Task) 하나 = 브랜치 하나**, `feat/<요약>`(영어 소문자 + `-`). 예: `feat/foundation`, `feat/main-screen`, `feat/review-trip`. 앞 작업 브랜치에서 갈라져 나와 이어 쌓는다. 버그만 고치면 `fix/<요약>`, 설계 문서만 바꾸면 `docs/<요약>`(2026-11-09 사용자 결정 — 예전 `fe/` 접두어는 쓰지 않는다).
- `main`에 직접 push하지 않는다. PR + 리뷰 1명 + CI 통과 → **Squash merge**.
- 커밋: `<type>(<scope>): <한국어 요약> (<기능ID>)`. type은 Backend와 같다(feat·fix·test·refactor·docs·build·chore).
- scope: `app`(차량 앱 공통) `design` `nav` `repo` `profile` `home` `review` `receipt` `report` `history` `settings` `admin` `camera` `backend`(submodule 갱신) `build` `spec`
- 예: `feat(profile): 넷플릭스식 운전자 선택 화면 (F-15)` · `build(backend): submodule을 49eddf2로 갱신 (STT 게이트웨이)`
- PR 설명 형식과 400줄 목표는 Backend CONVENTIONS 4장과 같다. 화면 PR에는 **스크린샷(WIDE·NARROW·TALL × 다크·라이트)**을 첨부한다(Roborazzi 출력).

## 3. 코드 스타일

- Kotlin: Backend CONVENTIONS 6장(ktlint, 이름, 단위 접미어)을 그대로 따른다. 커밋 전 `./gradlew ktlintFormat`.
- Compose 파일 구성: 화면 하나 = `XxxRoute.kt`(ViewModel 연결) · `XxxScreen.kt`(순수 UI + Preview) · `XxxViewModel.kt` · `XxxUiState.kt`(UiState·Action·Effect).
- Composable 이름은 PascalCase 명사, 파라미터 순서: 필수 데이터 → `onXxx` 콜백 → `modifier: Modifier = Modifier` (마지막).
- **누를 수 있는 요소는 모두 `Modifier.tappable`**(`ui/design/components/Press.kt`)로 만든다. 입력 잠금(스펙 6.9)·누름 피드백·햅틱이 한곳에 있다 — `clickable`을 직접 쓰지 않는다.
- 화면 바깥 여백은 `screenMargin()`(창 크기별 하나). 화면마다 `Space.s7`/`tallMargin`을 따로 고르지 않는다.
- 창 크기에 따라 달라지는 글자는 `AutoLogTheme.windowType.<역할>`(`ui/design/WindowType.kt`). 화면·컴포넌트에서 `if (LocalWindowLayout.current == NARROW) type.a else type.b`로 글자를 고르지 않는다 — 새 역할이 필요하면 그 표에 한 줄 더한다.
- 입력 칸이 여러 개인 화면은 칸 정의(이름·단위·입력 방법·계약 키)를 enum 하나에 모은다(예: `ui/receipt/FuelField.kt`). 칸마다 `when`을 여러 곳에 두지 않는다.
- ViewModel·Reducer는 화면 문구를 모른다. 사람에게 보일 이름·오류는 sealed 타입(예: `FuelTripLabel`, `FuelEntryError`)으로 내보내고 화면이 `strings.xml`로 바꾼다.
- 화면 하나의 상태 계산은 `XxxReducer.kt`의 순수 함수(`reduceXxx`)로 두고 Repository 값 타입만 입력으로 받는다. `data/interim` 타입은 `ui/`에 들어오지 않는다(스펙 6.6).
- 색·글자·간격·모양·모션은 **반드시 `AutoLogTheme` 토큰**. 리터럴 `Color(0xFF…)`, `16.dp`, `24.sp`를 화면 코드에 쓰지 않는다(토큰 정의 파일 제외).
- 모션은 `ui/design/motion/`의 도우미로만(디자인 언어 8.2): 팝업은 `ModalHost`로 감싸고(닫힘 모션), 같은 자리 교체 `FadeThrough`, 달 이동 `DirectionalContent`, 생겼다 사라지는 것 `ExpandVisibility`·`ExpandSwap`, 선택 `animateSelectColor`. 화면 코드에 `AnimatedContent`·`animate*AsState`·`spring(...)`·`tween(...)`을 직접 쓰지 않는다. 화면 전환 모양은 `FullApp`이 스택 변화로 정한다 — 화면은 Navigator만 부른다.
- 문자열은 `strings.xml`. 화면 코드에 한국어 리터럴을 쓰지 않는다(Preview·테스트 제외).
- TypeScript(web/admin): ESLint 기본 + `strict: true`. 컴포넌트 PascalCase, 훅 `useXxx`, API 함수 동사형.

## 4. 테스트

| 대상 | 규칙 |
|---|---|
| ViewModel | 화면마다 1개 이상. 정상·오류·잠금 상태 각 1개 이상 |
| 디자인 토큰 | 토큰을 바꾸면 `ContrastTest` 통과 필수 |
| 스크린샷 | 화면을 추가·변경하면 `./gradlew :vehicle-app:recordRoborazziDemoDebug`로 로컬에 기록해 검토한다. 파일은 창 크기별 폴더 `src/test/screenshots/{wide,narrow}/<이름>_<dark|light>.png`(`captureAll`이 정한다). **실제 Pleos 화면 두 크기만** 찍는다(2026-11-10 사용자 지시 — 근거 없는 비율은 지움): `wide/` = 앱 하나가 앱 영역 전체(Pleos Connect v2.1.0 에뮬레이터 2560×1268 dp), `narrow/` = 1:1 분할 한 칸(1280×1268 dp, Pleos Window Policy 기본 1:1). 둘 다 밀도 1.0이라 에뮬레이터와 같은 픽셀이다. **현재는 리포에 커밋하지 않는다**(`.gitignore`, 2026-11-09 사용자 결정 — 로컬 검토용). 그래서 CI는 `verifyRoborazzi`(기준 이미지와 비교 — 기준이 없으면 모든 스크린샷 테스트가 "original file was not found"로 실패)가 아니라 `recordRoborazzi`로 찍기만 하고, 캡처는 Actions 결과물 `screenshots`로 올린다(리뷰어가 내려받아 본다). 화면이 그려지다 죽는 문제는 잡지만 픽셀 회귀는 잡지 못한다. 기준 이미지를 커밋하기로 바꾸면 `.gitignore`에서 빼고 CI를 `verifyRoborazzi`로 되돌린 뒤 이 행을 고친다 |
| 컴포넌트 갤러리 | `ui/design/components/` 컴포넌트를 추가·변경하면 `GalleryScreenshotTest`(components·components_tiles·components_forms·components_modal)도 같이 고친다. 갤러리는 지금 컴포넌트 목록과 같아야 한다. UI를 바꾸면 `src/test/screenshots/`를 지우고 전부 다시 기록한다(옛 캡처를 남기지 않는다) |
| 웹 | 폼·API 오류 문구 테스트 |

테스트 이름은 백틱 문장: ``fun `주행이 시작되면 보조 화면을 모두 닫는다`()``.

## 5. PR 전 체크리스트

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck :vehicle-app:testDemoDebugUnitTest :vehicle-app:recordRoborazziDemoDebug :vehicle-app:assembleDemoDebug
(cd web/admin && npm run lint && npm test -- --run && npm run build)   # web을 바꿨을 때
```

- [ ] 소유 경계를 지켰다(`backend/` 미수정, 금지 import 없음)
- [ ] 리터럴 색·크기 없이 토큰만 썼다
- [ ] 스크린샷 4장(넓은 화면·분할 화면 × 2테마)을 로컬에 기록해 확인했다(커밋하지 않음)
- [ ] 사용 3단계를 지켰다: 주차(FULL) 전용 화면이고, 정차면 Peek·주행이면 화면 없음으로 바뀐다 (스펙 1.1)
- [ ] 조작 예산 안이다. 자동 진행에는 남은 시간과 1탭 취소가 있다 (스펙 1.2)
- [ ] [디자인 언어 13장 체크리스트](docs/design-language.md#13-리뷰-체크리스트-화면-pr마다)를 통과했다
- [ ] 맥 에뮬레이터 확인 여부를 PR에 적었다
- [ ] 비밀값·카드번호·정밀 좌표가 코드·로그에 없다
