# AutoLog Frontend — AI 코딩 도구용 안내

이 리포에서 코드를 쓰거나 고칠 때는 아래 문서를 따른다.

@CONVENTIONS.md

- 설계 기준: `docs/superpowers/specs/2026-11-09-autolog-frontend-design.md`. 결정(4장)을 바꾸려면 스펙을 먼저 고친다.
- 화면을 만들기 전에 `docs/design-language.md`를 읽는다. 색·글자·간격·모션은 토큰만 쓴다.
- 모든 화면의 전제: **주행 = 백그라운드(화면 없음) · 정차 = 읽기 전용 Peek · 주차 = 전체 앱**(스펙 1.1), **같은 결정은 최소 조작 — 기본값은 자동 진행, 예외만 터치**(스펙 1.2, 조작 예산).
- `backend/`는 submodule(AutoLog-Backend)이다. 이 리포에서 고치지 않는다. 계약은 `backend/vehicle-core/src/main/kotlin/kr/autolog/vehicle/core/Contracts.kt`.
- WSL에서는 Pleos 에뮬레이터가 돌지 않는다. 화면 확인은 Roborazzi 스크린샷(`recordRoborazziDemoDebug`)으로 하고, 실행 검증은 맥에서 한다.
- 커밋 전 `./gradlew ktlintFormat` 후 CONVENTIONS 5장 명령을 통과시킨다.
