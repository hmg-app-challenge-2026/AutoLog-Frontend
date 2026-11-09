# AutoLog Frontend

> 운전만 하세요. 기록은 차가 합니다.

AutoLog의 모든 화면을 만드는 리포다 — **Pleos 차량 앱**(Kotlin · Jetpack Compose · Material 3), **운영 웹**(React · TS · Vite), **폰 영수증 카메라 페이지**(HTML 1장). 차량 로직과 중계 서버는 [AutoLog-Backend](https://github.com/hmg-app-challenge-2026/AutoLog-Backend)에 있고, 이 리포는 그것을 `backend/` submodule로 가져와 공개 계약(`vehicle-core/.../core/Contracts.kt`)만 쓴다.

## 먼저 읽을 문서

1. [설계 스펙](docs/superpowers/specs/2026-11-09-autolog-frontend-design.md) — 무엇을 왜, 어떻게 만드는지 (결정·구조·화면·일정)
2. [디자인 언어](docs/design-language.md) — 철학·원칙·토큰·모션·컴포넌트
3. [작업 규칙](CONVENTIONS.md) — 브랜치·커밋·코드·테스트

## 받기

```bash
git clone --recurse-submodules https://github.com/hmg-app-challenge-2026/AutoLog-Frontend
# 이미 받았다면
git submodule update --init --recursive
```

요구: JDK 17, Android SDK 35, Node 20. 실행 검증은 Apple Silicon 맥의 Pleos Connect Emulator(API 34)에서 한다(스펙 5.5).
