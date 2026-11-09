# 검증 기록

스펙 13장 V-xx 결과. 날짜·환경(WSL/맥 에뮬레이터/실차)을 꼭 적는다.

| ID | 결과 | 환경 | 날짜 | 메모 |
|---|---|---|---|---|
| V-01 | | 맥 에뮬레이터 | | FormFactorConsole 프리셋별 dp·DPI·px |
| V-02 | 성공 — `includeBuild("backend") { dependencySubstitution { substitute(module("kr.autolog:vehicle-core")).using(project(":vehicle-core")) } }` 그대로 동작. 대안(include) 불필요 | WSL | 2026-11-09 | `:backend:vehicle-core` 태스크로 빌드됨 |
| V-03 | | 맥 에뮬레이터 | | |
| V-04 | | 맥 에뮬레이터 | | |
| V-05 | | WSL | | |
| V-06 | | 맥 에뮬레이터 | | |
| V-07 | | 실차 | | |
| V-08 | | 맥 에뮬레이터 | | |
| V-09 | | WSL | | |
| V-10 | | WSL | | |
| V-11 | | 맥 에뮬레이터 | | |
| V-12 | | 맥 에뮬레이터 | | |
| V-13 | | 맥 에뮬레이터 | | |
| V-14 | | — | | Backend sdk-findings #5 |
| V-15 | | 맥 에뮬레이터 | | |
| V-16 | | 맥 에뮬레이터 | | |
