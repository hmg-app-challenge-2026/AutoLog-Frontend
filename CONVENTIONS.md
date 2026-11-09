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

- 브랜치: `fe/<요약>`(영어 소문자 + `-`). 예: `fe/profile-picker`, `fe/admin-vehicles`. 설계 문서만 바꾸면 `docs/<요약>`.
- `main`에 직접 push하지 않는다. PR + 리뷰 1명 + CI 통과 → **Squash merge**.
- 커밋: `<type>(<scope>): <한국어 요약> (<기능ID>)`. type은 Backend와 같다(feat·fix·test·refactor·docs·build·chore).
- scope: `app`(차량 앱 공통) `design` `nav` `repo` `profile` `home` `review` `receipt` `report` `history` `settings` `admin` `camera` `backend`(submodule 갱신) `build` `spec`
- 예: `feat(profile): 넷플릭스식 운전자 선택 화면 (F-15)` · `build(backend): submodule을 49eddf2로 갱신 (STT 게이트웨이)`
- PR 설명 형식과 400줄 목표는 Backend CONVENTIONS 4장과 같다. 화면 PR에는 **스크린샷(WIDE·NARROW·TALL × 다크·라이트)**을 첨부한다(Roborazzi 출력).

## 3. 코드 스타일

- Kotlin: Backend CONVENTIONS 6장(ktlint, 이름, 단위 접미어)을 그대로 따른다. 커밋 전 `./gradlew ktlintFormat`.
- Compose 파일 구성: 화면 하나 = `XxxRoute.kt`(ViewModel 연결) · `XxxScreen.kt`(순수 UI + Preview) · `XxxViewModel.kt` · `XxxUiState.kt`(UiState·Action·Effect).
- Composable 이름은 PascalCase 명사, 파라미터 순서: 필수 데이터 → `onXxx` 콜백 → `modifier: Modifier = Modifier` (마지막).
- 색·글자·간격·모양·모션은 **반드시 `AutoLogTheme` 토큰**. 리터럴 `Color(0xFF…)`, `16.dp`, `24.sp`를 화면 코드에 쓰지 않는다(토큰 정의 파일 제외).
- 문자열은 `strings.xml`. 화면 코드에 한국어 리터럴을 쓰지 않는다(Preview·테스트 제외).
- TypeScript(web/admin): ESLint 기본 + `strict: true`. 컴포넌트 PascalCase, 훅 `useXxx`, API 함수 동사형.

## 4. 테스트

| 대상 | 규칙 |
|---|---|
| ViewModel | 화면마다 1개 이상. 정상·오류·잠금 상태 각 1개 이상 |
| 디자인 토큰 | 토큰을 바꾸면 `ContrastTest` 통과 필수 |
| 스크린샷 | 화면을 추가·변경하면 `./gradlew :vehicle-app:recordRoborazziDemoDebug`로 기준 이미지를 갱신해 같은 PR에 커밋. 기준 플랫폼은 Linux(WSL·CI). 맥에서 기록한 이미지는 커밋하지 않는다 |
| 웹 | 폼·API 오류 문구 테스트 |

테스트 이름은 백틱 문장: ``fun `주행이 시작되면 보조 화면을 모두 닫는다`()``.

## 5. PR 전 체크리스트

```bash
./gradlew ktlintFormat
./gradlew ktlintCheck :vehicle-app:testDemoDebugUnitTest :vehicle-app:verifyRoborazziDemoDebug :vehicle-app:assembleDemoDebug
(cd web/admin && npm run lint && npm test -- --run && npm run build)   # web을 바꿨을 때
```

- [ ] 소유 경계를 지켰다(`backend/` 미수정, 금지 import 없음)
- [ ] 리터럴 색·크기 없이 토큰만 썼다
- [ ] 스크린샷 6장(3레이아웃 × 2테마)을 갱신했다
- [ ] 사용 3단계를 지켰다: 주차(FULL) 전용 화면이고, 정차면 Peek·주행이면 화면 없음으로 바뀐다 (스펙 1.1)
- [ ] 조작 예산 안이다. 자동 진행에는 남은 시간과 1탭 취소가 있다 (스펙 1.2)
- [ ] [디자인 언어 13장 체크리스트](docs/design-language.md#13-리뷰-체크리스트-화면-pr마다)를 통과했다
- [ ] 맥 에뮬레이터 확인 여부를 PR에 적었다
- [ ] 비밀값·카드번호·정밀 좌표가 코드·로그에 없다
