# AutoLog 디자인 언어

| 항목 | 내용 |
|---|---|
| 문서 목적 | AutoLog 화면이 **왜 이렇게 생겼고, 어떻게 움직이며, 무엇을 하지 않는지**를 정한다. 디자이너가 없어도 이 문서만으로 같은 화면이 나와야 한다 |
| 적용 범위 | Pleos 차량 앱(1순위), 운영 웹, 폰 카메라 페이지 |
| 상태 | 확정 v1.0 (2026-11-09). 11.10–11 디자인 게이트에서 스크린샷 리뷰 후 값만 조정할 수 있다 |
| 함께 읽을 문서 | [설계 스펙](superpowers/specs/2026-11-09-autolog-frontend-design.md) — 사용 3단계·조작 예산·화면 구조·데이터 |
| 코드 위치 | `vehicle-app/src/main/kotlin/kr/autolog/vehicle/ui/design/` — 이 문서의 토큰 이름이 코드 이름이다 |

> 규범 용어: **반드시** · **하지 않는다** · **권장** — 설계 스펙과 같다.

---

## 1. 한 문장

> **차가 조용히 기록하고, 사람은 한 번만 확인한다.**

AutoLog의 UI는 존재감을 줄이는 쪽으로 설계한다. **주행 중에는 보이지 않고**(백그라운드에서 기록), **정차 중에는 옆에서 잠깐 보여 주기만** 하고, **주차하면** 필요한 것 하나를 크고 분명하게 묻는다. 그리고 묻더라도 **사람이 누르지 않아도 되는 쪽이 기본값**이다. 화려함보다 **신뢰**가, 기능의 수보다 **조작 없이 끝나는 경험**이 중요하다.

---

## 2. 세 가지 레퍼런스를 어떻게 해석하나

레퍼런스는 **느낌과 원리**만 가져온다. 로고·고유 색·아이콘·레이아웃을 복제하지 않는다(심사·저작권 위험).

### 2.1 테슬라 — "차가 주인공, UI는 침묵한다"

| 가져오는 원리 | AutoLog에서의 모습 |
|---|---|
| 차량 상태가 화면의 중심이다 | Home의 주인공은 **계기판 거리 숫자** 하나다. 메뉴·장식이 숫자보다 앞에 나오지 않는다 |
| 크롬(테두리·구분선·장식)이 거의 없다 | 구분은 선이 아니라 **면의 명도 차이**와 **여백**으로 한다 |
| 어두운 바탕, 밝은 정보 | 다크가 기본. 넓은 순백 면을 쓰지 않는다(야간 눈부심) |
| 강조색은 하나만 | 파랑 `accent` 하나. 나머지는 회색 단계와 의미색(성공·경고·위험)뿐 |
| 큰 숫자, 가벼운 굵기 | 거리 숫자는 300 굵기의 아주 큰 글자. 무게감은 크기에서 나오고 굵기에서 나오지 않는다 |

### 2.2 넷플릭스 — "사람이 먼저, 1탭으로 들어간다"

| 가져오는 원리 | AutoLog에서의 모습 |
|---|---|
| 앱을 열면 "누구세요?"부터 묻는다 | 앱 진입 = **"누가 운전하나요?"** 화면. 공용차에서 폰·비콘 없이 운전자를 안다 |
| 프로필은 크고 친근한 타일 | 큰 정사각 아바타(이니셜 + 사람마다 다른 그라데이션). 이름은 타일 아래 |
| 고르면 바로 들어간다 | 1탭 → 아바타가 Home 상단 칩으로 **이어져 날아가는** 전환. 확인 단계 없음 |
| 선택지는 적게 | 타일 최대 5개 + [다른 사람]. 배차 운전자가 첫 자리에서 링으로 강조된다 |

### 2.3 애플 — "물리적으로 자연스럽고, 예측 가능하다"

| 가져오는 원리 | AutoLog에서의 모습 |
|---|---|
| 움직임은 물리를 따른다 | 모든 위치·크기 변화는 **스프링**. 시간이 아니라 질량과 탄성으로 움직인다 |
| 튀지 않는다 | 감쇠비 0.85 이상. 오버슈트(튕김)를 거의 쓰지 않는다 |
| 손끝에 즉시 반응한다 | 누르는 순간 100 ms 안에 살짝 눌림(0.97배) + 햅틱. 손을 떼면 부드럽게 복원 |
| 여백이 많고 정돈돼 있다 | 8 dp 그리드, 큰 바깥 여백, 한 화면 한 가지 주 동작 |
| 같은 것은 항상 같다 | 같은 의미는 같은 색·같은 자리·같은 말. 화면마다 새로 발명하지 않는다 |

### 2.4 셋이 부딪힐 때

**운전 안전 > 신뢰(정확한 데이터) > 조작 최소화 > 간결함 > 아름다움** 순서로 고른다. 예: 아바타 전환이 아름다워도 주차 상태가 아니면 하지 않는다(안전). 추천 목적이 확신이 낮으면 큰 추천 버튼 대신 선택지를 보여 준다(신뢰가 조작 최소화보다 앞선다).

### 2.5 사용 단계 — 주행·정차·주차

화면이 허용하는 범위는 차량 상태로 정해진다(설계 스펙 1.1). 디자인도 단계마다 다르다.

| 단계 | 사람의 상황 | 디자인의 역할 | 디자인 규칙 |
|---|---|---|---|
| **주행** (`BACKGROUND`) | 도로를 본다 | **없음.** 앱 화면을 띄우지 않는다. 지속 알림(앱 A) 한 줄이 전부 | 이 단계용 화면을 디자인하지 않는다 |
| **정차** (`PEEK`) | 신호 대기 중 1~2초 힐끗 본다 | "기록이 잘 되고 있다"는 **안심**을 준다 | 읽기 전용 Peek 패널 하나. 버튼·스크롤·움직임 없음. 정보 5줄 이하, 사이드 창 폭에 맞춘 세로 스택 |
| **주차** (`FULL`) | 손이 자유롭고 시간이 있다 | 남은 결정을 **최소 조작으로 끝내게** 한다 | 전체 앱. 이 문서의 모든 컴포넌트·모션 사용 |

### 2.6 조작 최소화 — 팔을 드는 비용

차량 디스플레이는 팔을 뻗어 들어 올려야 닿는다. 팔꿈치를 받쳐 주는 것이 없어 노트북 터치스크린처럼 **터치 한 번의 신체 부담이 스마트폰·태블릿보다 훨씬 크고**, 반복할수록 어깨가 빨리 지친다("고릴라 팔" 효과). 그래서 같은 결정을 하더라도 **팔을 드는 횟수**를 최소로 설계한다.

| 규칙 | 뜻 | 대표 사례 |
|---|---|---|
| **기본값은 무조작으로 진행** | 가장 흔한 결과는 사람이 아무것도 안 해도 일어난다. 사람은 예외일 때만 누른다(opt-out) | 기어 P → "기록을 마칠까요?" 5초 카운트다운 → 만료되면 자동 종료. [계속 기록]을 눌러야만 이어짐 |
| **고르면 바로 실행** | 선택 + [확인]의 2단계를 만들지 않는다 | 넷플릭스식 운전자 선택: 타일 1탭 = 선택 + 진입 |
| **같은 결정을 두 번 묻지 않는다** | 확인 다이얼로그는 되돌리기 어려운 동작(삭제)에만 | [기록 종료]는 확인 없이 즉시 종료 |
| **입력보다 선택, 선택보다 추천** | 키보드 < 목록 < 프리셋 < 큰 추천 버튼 1개 | 목적 확인: 확신 높은 추천 1탭 |
| **자동 진행은 보이게** | 무조작 진행에는 항상 남은 시간과 1탭 취소가 함께 보인다 | 카운트다운 숫자 + 줄어드는 막대 + 가장 큰 버튼이 [계속 기록] |
| **기본값에서 벗어나는 버튼을 가장 크게** | 자동 진행 화면에서 `PrimaryButton`은 "취소·계속" 쪽이다. 기본값을 앞당기는 버튼은 `SecondaryButton` | AutoEndPrompt: [계속 기록] 주, [기록 종료] 보조 |

작업별 **조작 예산**(설계 스펙 1.2의 표와 같다): 운전자 선택 1 · 운행 시작 0 · 운행 종료 0 · 목적 확정 1 · 영수증 확정 1 · 운행기록부 발송 2 · 정차 중 확인 0. 화면을 설계할 때 먼저 "이 결정은 몇 번 팔을 들게 하나"를 세고, 예산을 넘으면 기본값·추천·자동 진행으로 줄인다.

---

## 3. 설계 원칙 11가지

1. **1~2초 안에 읽힌다.** 운전자는 한 번에 1~2초만 본다(Pleos). 화면마다 "1초에 알아야 할 것" 하나를 정하고 가장 크게 그린다.
2. **같은 결정은 최소 조작으로.** 기본값은 조작 없이 진행되고, 사람은 예외일 때만 누른다(2.6). 조작 예산을 넘지 않는다.
3. **화면당 주 동작은 하나.** 가장 중요한 버튼 하나만 `primary`다. 나머지는 `secondary`·`tertiary`.
4. **손이 닿는 곳에 둔다.** 주 버튼은 운전자에 가까운 쪽 아래(6.3). 화면 위쪽 구석에 중요한 버튼을 두지 않는다.
5. **움직임은 상태를 설명할 때만.** 장식 애니메이션, 반복 애니메이션, 자동 스크롤을 하지 않는다(Pleos 애니메이션 원칙).
6. **주행 중엔 보이지 않고, 정차 중엔 보기만 한다.** 주행 = 화면 없음, 정차 = 읽기 전용 Peek, 조작은 주차에서만(2.5).
7. **정직한 데이터.** 모르는 값은 모른다고 쓴다. 추정 거리를 그리지 않는다. 자동값에는 출처를 붙이고, 확정 전 값은 "추천"·"확인 필요"로 구분해 보인다.
8. **색에는 의미가 있다.** 파랑 = 할 수 있는 것, 초록 = 확정·통과, 노랑 = 확인 필요, 빨강 = 막힘·위험. 색만으로 전달하지 않고 아이콘·글자를 함께 쓴다.
9. **되돌릴 수 있게.** 삭제·무시처럼 되돌리기 어려운 동작만 확인을 묻는다. 나머지는 바로 실행하고 "되돌리기"를 준다.
10. **같은 것은 같게.** 이 문서의 컴포넌트만 쓴다. 화면 안에서 새 버튼 모양을 만들지 않는다.
11. **모두가 쓸 수 있게.** 대비 4.5:1 이상, 터치 76 dp 이상, 한 손으로 조작, 글자 최소 크기 준수.

---

## 4. 인간 요인 근거 — 왜 이 숫자인가

| 근거 | 출처 | 반영 |
|---|---|---|
| 1회 시선 1~2초 | Pleos UI Design Guide | 원칙 1, 정보 위계 |
| 주요 텍스트 높이 ≥ 5 mm, 보조 ≥ 3 mm | Pleos UI Design Guide | `type.title` 36 sp 이상을 주요 정보 하한으로(V-06에서 실측 확인) |
| 대비 ≥ 4.5:1 | Pleos UI Design Guide | 5장 토큰 전부 계산 완료, 단위 테스트로 강제 |
| 입력 후 응답 ≤ 0.25초 | Pleos UI Design Guide | 눌림 피드백 100 ms, 결과·진행 표시 250 ms |
| 한 줄 ≤ 80자(영문), 문단 ≤ 3줄 | Pleos UI Design Guide | 9장 문구 규칙 |
| 한 손 조작 | Pleos UI Design Guide | 양손 제스처·핀치 확대를 필수 동작으로 쓰지 않음 |
| 주행 중 애니메이션·자동 스크롤 금지 | Pleos UI Design Guide · 주의 분산 가이드 | 주행 중 화면 없음(2.5), Peek 정적(8.4) |
| 팔을 받침 없이 들어 올리는 터치는 피로가 빨리 쌓인다("고릴라 팔") | 수직 터치스크린 인간공학 일반 | 조작 최소화(2.6), 조작 예산 |
| 사람은 기본값을 그대로 두는 경향이 강하다(default effect) | 행동경제학 일반 | 가장 흔한 결과를 기본값으로, 예외만 터치(2.6) |
| 터치 타깃 ≥ 76 dp | AAOS 디자인 가이드라인 | `size.touchMin` |
| 피츠의 법칙: 목표가 클수록, 가까울수록 빨리 누른다 | HCI 일반 | 주 버튼 96–120 dp 높이, 운전자 쪽 하단 |
| 주행 진동 속 터치 정확도 저하 | 차량 HMI 일반 | 버튼 사이 간격 ≥ 16 dp, 작은 아이콘 단독 버튼 금지 |
| 야간 암순응: 넓은 밝은 면은 눈부심 | 차량 HMI 일반 | 다크 기본, 다크에서 순백 면적 최소화 |
| 주변시는 움직임에 민감하다 | 지각 심리 | Peek 정적, 깜빡임·펄스·슬라이드 금지 |

---

## 5. 색

### 5.1 원칙

- 다크가 기본, 라이트는 주간 모드. **시스템 야간 모드(`uiMode`)를 따른다.** 앱 안에 테마 스위치를 두지 않는다(차가 결정한다).
- 강조색은 `accent` 하나. 의미색은 성공·경고·위험 3가지만.
- 아래 모든 텍스트 색은 **모든 서피스(bg, s1, s2, s3) 위에서 4.5:1 이상**으로 계산을 마쳤다. 표의 "최소 대비"는 가장 불리한 서피스 기준이다.

### 5.2 다크 (기본)

| 토큰 | 값 | 용도 | 최소 대비 |
|---|---|---|---|
| `bg` | `#0A0B0D` | 화면 바탕 | — |
| `surface1` | `#131518` | 카드, 리스트 행 | — |
| `surface2` | `#1B1E22` | 시트, 눌린 카드, 입력 필드 | — |
| `surface3` | `#252930` | 칩, 선택된 행 | — |
| `outline` | `#2E333A` | 장식 구분선(정보 전달용 아님) | — |
| `outlineStrong` | `#6B7280` | 입력 필드 테두리, 체크박스 | 3.02 (비텍스트 3:1) |
| `textPrimary` | `#F4F5F7` | 본문, 숫자 | 13.38 |
| `textSecondary` | `#B0B6BF` | 보조 설명 | 7.15 |
| `textTertiary` | `#8D949E` | 단위(km), 메타 정보 | 4.77 |
| `accent` | `#5B9BFF` | 텍스트 링크, 아이콘 강조, 선택 링 | 5.27 |
| `accentFill` | `#3268F8` | 주 버튼 채움 | 흰 글씨 4.70 · 서피스 대비 3.11 |
| `onAccent` | `#FFFFFF` | 주 버튼 글자 | — |
| `success` | `#3DD68C` | 확정·통과 | 7.78 |
| `warning` | `#F7B955` | 확인 필요 | 8.36 |
| `danger` | `#FF6B6B` | 오류·막힘 텍스트 | 5.26 |
| `dangerFill` | `#D93A3F` | 삭제 버튼 채움 | 흰 글씨 4.54 · 서피스 대비 3.22 |
| `scrim` | `#000000` 56% | 팝업 뒤 덮개 | — |

### 5.3 라이트 (주간)

| 토큰 | 값 | 최소 대비 |
|---|---|---|
| `bg` | `#F4F5F7` | — |
| `surface1` | `#FFFFFF` | — |
| `surface2` | `#ECEEF1` | — |
| `surface3` | `#E1E4E9` | — |
| `outline` | `#D5D9DF` | — |
| `outlineStrong` | `#767D88` | 3.26 (비텍스트) |
| `textPrimary` | `#0D0F12` | 15.06 |
| `textSecondary` | `#454C56` | 6.80 |
| `textTertiary` | `#59606B` | 4.98 |
| `accent` | `#1F55D1` | 5.03 |
| `accentFill` | `#2B63F0` | 흰 글씨 5.07 · 서피스 대비 3.97 |
| `onAccent` | `#FFFFFF` | — |
| `success` | `#0B6B3C` | 5.18 |
| `warning` | `#7A4E00` | 5.65 |
| `danger` | `#B02525` | 5.25 |
| `dangerFill` | `#D93A3F` | 흰 글씨 4.54 · 서피스 대비 3.56 |
| `scrim` | `#000000` 32% | — |

### 5.4 아바타 그라데이션 (ProfileTile)

사람마다 `userId` 해시로 8개 중 하나를 고른다(같은 사람은 항상 같은 색). 위→아래 135° 선형. 흰 이니셜이 **두 끝 색 모두에서 4.5:1 이상**이다.

| # | 시작 | 끝 | 흰 글씨 대비(밝은 쪽) |
|---|---|---|---|
| 0 | `#3B5BDB` | `#1E2F8C` | 5.67 |
| 1 | `#7048E8` | `#3A1F8F` | 5.55 |
| 2 | `#C2255C` | `#6E1236` | 5.66 |
| 3 | `#C2410C` | `#8A2E06` | 5.18 |
| 4 | `#237A36` | `#134A20` | 5.37 |
| 5 | `#0B7285` | `#064B57` | 5.59 |
| 6 | `#5C6B7A` | `#2C343D` | 5.47 |
| 7 | `#B5447F` | `#5E1F44` | 5.12 |

### 5.5 Material 3 매핑

M3 컴포넌트를 쓸 때 색이 어긋나지 않도록 `AutoLogTheme`이 `ColorScheme`을 아래처럼 채운다. 화면 코드는 M3 `MaterialTheme.colorScheme`이 아니라 **`AutoLogTheme.colors.*`**(위 토큰)를 쓴다.

| M3 | 토큰 |
|---|---|
| `primary` / `onPrimary` | `accentFill` / `onAccent` |
| `background`, `surface` | `bg` |
| `surfaceContainerLow` / `surfaceContainer` / `surfaceContainerHigh` | `surface1` / `surface2` / `surface3` |
| `onBackground`, `onSurface` | `textPrimary` |
| `onSurfaceVariant` | `textSecondary` |
| `outline` / `outlineVariant` | `outlineStrong` / `outline` |
| `error` / `onError` | `danger` / `onAccent` |
| `scrim` | `scrim` |

### 5.6 검증

`ContrastTest`가 5.2·5.3의 모든 텍스트 토큰 × 모든 서피스, `onAccent` × `accentFill`, 흰 글씨 × 5.4의 모든 색을 계산해 4.5:1 미만이면 실패한다. 비텍스트(`outlineStrong`, `accentFill`, `dangerFill`) × 서피스는 3:1. **토큰을 바꾸면 이 테스트가 먼저 통과해야 한다.**

---

## 6. 타이포그래피와 레이아웃

### 6.1 서체

- **Asta Sans**(42dot, SIL OFL 1.1, 가변 굵기 300–800, 한글·라틴). Pleos 시스템 UI 서체와 같다.
- 파일: `vehicle-app/src/main/res/font/asta_sans.ttf`(google/fonts의 `AstaSans[wght].ttf`) + `OFL.txt`. 설정 > 정보 > 오픈소스 라이선스에 표기한다.
- Compose: 굵기별 `Font(R.font.asta_sans, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))`로 `FontFamily`를 만든다(가변 서체, API 26+).
- **차량 앱 어디에도 다른 서체가 나오지 않는다.** 화면 글자는 모두 `AutoLogTheme.type.*`(Asta Sans) style을 쓰고, Material 3 글자 슬롯 15개(`autoLogMaterialTypography`)와 XML 앱 테마(`android:fontFamily`, View 기반 시스템 대화상자·토스트)도 Asta Sans다. 코드에서 `fontFamily`·`FontFamily`·`Typeface`를 직접 지정하지 않는다(`Type.kt` 제외). `FontCoverageTest`가 이를 지킨다. 앱이 보여 주는 모든 글자가 Asta Sans 글리프 안에 있음을 2026-11-09 확인(대체 서체로 떨어지는 글자 없음)
- 운영 웹: `@fontsource-variable/asta-sans`(self-host). 카메라 페이지: 시스템 서체(설계 스펙 D-15).

### 6.2 글자 크기 (`AutoLogTheme.type.*`)

| 토큰 | 크기 | 굵기 | 행간 | 자간 | 용도 |
|---|---|---|---|---|---|
| `hero` | 140 sp | 300 | 1.0 | −2% | 이번 운행 거리 숫자(Home RecordingParked) |
| `heroSmall` | 96 sp | 300 | 1.0 | −1.5% | Peek의 이번 운행 거리, 좁은 창의 이번 운행 거리, 요약 숫자 |
| `heroCompact` | 72 sp | 300 | 1.0 | −1.5% | 자릿수가 많은 누적거리(18,420.5)를 좁은 창·Peek에 쓸 때 |
| `display` | 56 sp | 600 | 1.15 | −1% | 화면 제목 ("누가 운전하나요?") |
| `headline` | 44 sp | 600 | 1.2 | −0.5% | 섹션 제목, 다이얼로그 제목 |
| `title` | 36 sp | 600 | 1.25 | 0 | **주요 정보 하한**: 목적지, 추천 목적, 카드 제목 |
| `body` | 28 sp | 400 | 1.4 | 0 | **본문 하한**: 설명, 리스트 내용 |
| `label` | 28 sp | 600 | 1.2 | 0 | 버튼, 칩 |
| `caption` | 22 sp | 500 | 1.3 | +0.5% | **보조 하한**: 메타 정보. **Peek에 쓰지 않는다** |
| `section` | 22 sp | 700 | 1.3 | +1% | 영역 소제목(`SectionHeader`). 크기는 caption과 같고 굵기로 제목임을 드러낸다 |

- 숫자와 단위는 **줄바꿈하지 않는다**("k / m" 방지). 넘치면 크기 단계를 `hero` → `heroSmall` → `heroCompact`로 낮춘다(`HeroMetric`의 `MetricSize`).
- 숫자(거리·금액·시각)는 **고정폭 숫자** `fontFeatureSettings = "tnum"`. 숫자가 바뀔 때 폭이 흔들리지 않아야 한다. Asta Sans가 tnum을 지원하지 않으면(V-05) 숫자 영역의 너비를 최대 자릿수 기준으로 고정한다.
- 단위는 숫자보다 작고 옅게: `23.4` (`hero`, `textPrimary`) + `km` (`headline`, `textTertiary`), 기준선 정렬.
- 한 줄 최대 80자(영문 기준, 한글은 약 40자), 한 덩어리 최대 3줄. 넘치면 문장을 줄인다(말줄임표로 자르는 것은 최후의 수단).
- `caption`보다 작은 글자를 만들지 않는다.
- 모든 글자 스타일은 한국어를 어절 중간에서 자르지 않는 줄바꿈(`LineBreak.WordBreak.Phrase`, Android 13+)을 쓴다. 단, Robolectric 스크린샷에서는 적용되지 않아(2026-11-09 확인) 끊김 위험이 있는 곳은 문장을 나눠 설계한다(예: 자동 종료 시트의 거리·목적지 두 줄).

### 6.3 운전자 쪽 (`driverSide`)

- 한국은 좌핸들이므로 기본값 `START`(왼쪽). Pleos의 Driving view도 운전자 가까운 쪽에 고정돼 있다.
- **주 버튼과 동작 컬럼은 `driverSide` 쪽 아래에** 둔다. 읽기 전용 정보(거리·목적지)는 반대쪽.
- 레이아웃 코드는 `Arrangement.Start/End`를 직접 쓰지 않고 `driverSide`에서 계산한 값을 쓴다. 우핸들 시장은 설정 값만 바꾼다.

### 6.4 간격 (8 dp 그리드)

| 토큰 | dp | 쓰임 |
|---|---|---|
| `space.1` | 4 | 아이콘–글자 사이 |
| `space.2` | 8 | 관련 요소 사이 |
| `space.3` | 12 | 칩 안쪽 여백 |
| `space.4` | 16 | 버튼 사이 최소 간격 |
| `space.5` | 24 | 카드 안쪽 여백, 그리드 거터 |
| `space.6` | 32 | 섹션 사이 |
| `space.7` | 48 | 화면 바깥 여백(WIDE) |
| `space.8` | 64 | 큰 덩어리 사이 |
| `space.9` | 96 | 제목 위 여백(TALL) |

| 레이아웃 | 바깥 여백 | 거터 |
|---|---|---|
| WIDE | `space.7` 48 | `space.5` 24 |
| NARROW | `space.6` 32 | `space.5` 24 |
| TALL | 40 | `space.5` 24 |

### 6.5 크기

| 토큰 | dp | 쓰임 |
|---|---|---|
| `size.touchMin` | 76 | 모든 터치 대상의 최소 높이·너비 |
| `size.buttonMd` | 76 | 보조 버튼, 리스트 행 |
| `size.buttonLg` | 96 | 주 버튼 |
| `size.buttonXl` | 120 | Home의 [운행 시작]·[기록 종료] (NARROW에서는 높이가 모자라 `buttonLg` 96) |
| `size.iconButton` | 96 | 마이크, 키패드 키 |
| `size.icon` | 36 | 버튼 안 아이콘 |
| `size.iconLg` | 48 | 상태 아이콘 |
| `size.profileTileWide` / `Narrow` | 200 / 160 | 프로필 타일 |

### 6.6 모양과 깊이

- 모서리: `radius.sm` 14 · `radius.md` 20(카드) · `radius.lg` 28(시트, 큰 버튼) · `radius.xl` 40(프로필 타일) · `radius.full`(칩, 원형 버튼). 버튼은 높이에 비례해 `radius.lg`를 기본으로 한다.
- 깊이는 **그림자 대신 서피스 단계**(`bg` < `surface1` < `surface2` < `surface3`)로 표현한다. 팝업도 그림자 없이 `surfaceRaised` + 머리카락 테두리 + `scrim`으로 분리한다(그림자는 렌더러마다 다르게 그려졌다, 2026-11-09).
- 테두리는 입력 필드와 선택 상태에만. 카드에 테두리를 두르지 않는다.

---

## 7. 레이아웃 패턴

화면 크기 3단계(WIDE·NARROW·TALL) 판정은 설계 스펙 6.8. 같은 화면은 **같은 정보·같은 기능**을 갖고 배치만 다르다.

| 패턴 | WIDE | NARROW | TALL |
|---|---|---|---|
| **Status · Sections · Tiles** (Home, 스펙 D-23~D-26) | 위 상태 줄(점 + 글자) · 배지 / 가운데 좌우 반반: 기록 중 "주행 거리"(72 sp) · "운행 경로", 대기 "이번 달 운행 정보" · "최근 운행 기록"(3건) / 아래 타일 4개. 단 사이 16 dp | 상태 줄 / 값 → 맥락 / 타일 4개 한 줄 | NARROW와 같음(거리 96 sp) |
| **Peek** (정차) | 가운데 카드(최대 폭 560 dp), 정보 늘리지 않음 | 세로 스택 가운데 정렬 — 기본 형태(사이드 창) | NARROW와 같음 |
| **Countdown** (AutoEndPrompt) | 가운데 카드, 버튼 2개 가로(왼쪽 [기록 종료], 오른쪽 [계속 기록]) | 가운데 카드, 버튼 2개 세로(위 [기록 종료], 아래 [계속 기록]) | NARROW와 같음 |
| **Form Dialog** (새 운전자 추가) | 머리 줄(제목 가운데 · X 오른쪽) → 아바타(120 dp) → 이름·부서 한 줄에 나란히 → 주 버튼(전폭) | WIDE와 같음 | 머리 줄 → 아바타(160 dp) → 이름 → 부서 → 주 버튼 |
| **Picker** (ProfilePicker) | 타일 한 줄 가운데(200 dp), 끝에 `[+ 새 운전자 추가]` | 3열(128 dp), 제목은 어절 단위 두 줄 | 3열(160 dp) |
| **Media + Form** (FuelEntry — 영수증에서 왔을 때) | 좌 이미지 / 우 필드 | 이미지 상단 1/3 + 필드 | NARROW와 같음 |
| **Master–Detail** (Settings) | 좌 목록 / 우 상세 | 목록 → 상세 push | NARROW와 같음 |
| **Decision** (ReviewTrip) | 좌 요약 / 우 선택지·추천 | 요약 → 추천 → 선택지 세로 | NARROW와 같음 |

- 주 버튼은 스크롤 영역 밖 **하단 고정**. 스크롤해야 보이는 주 버튼을 만들지 않는다.
- **오른쪽 위는 예약 자리**(운전자 배지). 화면의 맨 위 줄은 `padding(end = LocalTopEndReserved.current)`로 비운다 — 배지가 실제로 잰 폭이 들어온다. 화면이 배지 폭을 숫자로 가정하지 않는다.
- 스크롤 영역에 더 볼 내용이 있으면 그 가장자리를 `Modifier.verticalScrollFade(state, space.7)`로 흐린다. 잘린 글자가 "깨짐"이 아니라 "더 있음"으로 읽힌다.
- 창 크기가 바뀔 때 레이아웃 전환: 주차 중 `motion.layout`, Peek에서는 즉시.
- 시스템 UI가 겹칠 수 있으므로 핵심 정보는 창 가장자리에서 24 dp 안쪽에 두지 않는다.

---

## 8. 모션

### 8.1 원칙

- 모션은 **상태 변화를 설명**할 때만 쓴다: 어디서 왔고 어디로 갔는지, 무엇이 눌렸는지, 무엇이 바뀌었는지.
- 위치·크기는 스프링, 투명도는 짧은 tween.
- **무한 반복 애니메이션은 쓰지 않는다.** 예외: 주차 중 1초 이상 걸리는 로딩의 진행 표시.

### 8.2 토큰 (`AutoLogTheme.motion.*`)

| 토큰 | 정의 | 쓰임 |
|---|---|---|
| `spring.standard` | `spring(dampingRatio = 0.9f, stiffness = 400f)` | 일반 상태 변화(칩 등장, 펼침) |
| `spring.gentle` | `spring(dampingRatio = 1.0f, stiffness = 200f)` | 화면 전환, 큰 면, 레이아웃 전환 |
| `spring.snappy` | `spring(dampingRatio = 0.85f, stiffness = 700f)` | 눌림 복원, 토글, 선택 |
| `fade.quick` | `tween(150, easing = LinearOutSlowInEasing)` | 숫자 교체, 작은 요소 |
| `fade.standard` | `tween(220, easing = FastOutSlowInEasing)` | 시트·다이얼로그 등장 |
| `press` | 누름: 0.97배, `tween(90)` / 뗌: `spring.snappy`로 1.0 | 모든 버튼·타일·카드 |
| `motion.screen` | 들어옴: `fadeIn(fade.standard)` + 4% 거리 슬라이드 + 0.98→1 확대(`spring.gentle`) / 나감: `fadeOut(fade.quick)` | 화면 push·pop |
| `motion.hero` | `SharedTransitionLayout` + `boundsTransform = spring.gentle` | 프로필 타일 → Home 칩 |
| `motion.layout` | `animateContentSize`·`Modifier.animateBounds` + `spring.gentle` | WIDE↔NARROW 전환 |
| `motion.loading` | 결정형 진행 막대 우선. 불확정이면 1200 ms 주기의 느린 밝기 변화(주차 전용) | 엑셀 생성, QR 발급 |

### 8.3 눌림 피드백

1. 손가락이 닿는 즉시(100 ms 안) `press` 축소 + 서피스 한 단계 밝게.
2. 햅틱 `HapticFeedbackConstants.CONFIRM`(API 30+, 없으면 `VIRTUAL_KEY`). 헤드유닛 지원 여부는 V-07.
3. 손을 떼면 `spring.snappy`로 복원하고 동작을 실행한다. 결과가 250 ms 안에 안 나오면 버튼을 `loading` 상태로.

### 8.4 단계별 모션 정책 (반드시)

`tier != FULL`(잠금)인 동안 `AutoLogTheme.motion`은 **정지형 세트**로 바뀐다. 컴포넌트는 토큰만 쓰므로 자동으로 따라간다.

반복 상태 표시(경로 화살표 흐름)는 화면 모션과 따로 `motion.ambient`가 정한다 — 정차에서도 켜지고, 시스템 "애니메이션 줄이기"·스크린샷에서만 꺼진다(`MotionTokens.Still`). 꺼지면 같은 자리에 방향이 읽히는 정지 그림을 그린다. 정차 중 허용은 Pleos 확인 대기(스펙 V-17).

| 항목 | 주차 (FULL) | 정차 (PEEK) | 주행 (BACKGROUND) |
|---|---|---|---|
| 위치·크기 변화 | 스프링 | `snap()` (즉시) | 화면 없음 |
| 투명도 변화 | `fade.*` | FULL→PEEK 전환 한 번만 `fade.quick` | 화면 없음 |
| 화면 전환 | `motion.screen` | 없음(Peek 한 장) | 화면 없음 |
| 숫자 갱신 | 크로스페이드 | 즉시 교체, 1초 스로틀 | 화면 없음 |
| shared-element | `motion.hero` | 사용 안 함 | — |
| 로딩 | `motion.loading` | 정적 문구 | — |
| 카운트다운 막대(AutoEndPrompt) | 선형 감소 허용(상태 설명) | Peek에서는 숫자만 | — |
| 흐름 표시(경로 화살표 `RouteFlow`) | 허용 | **허용(예외)** — 투명도만 바뀌고 위치 이동 없음, 주기 1.4초(`motion.flowPeriodMs`) | 화면 없음 |
| 그 밖의 깜빡임·펄스·자동 스크롤 | 사용 안 함 | 사용 안 함 | — |

### 8.5 시스템 "애니메이션 줄이기"

`Settings.Global.ANIMATOR_DURATION_SCALE == 0`이면 주차 중에도 정지형 세트를 쓴다(카운트다운은 숫자만).

---

## 9. 문구

### 9.1 목소리

- **해요체**, 짧게, 운전자를 "당신"이라 부르지 않는다. 주어를 생략한다.
- **숫자와 결론 먼저.** "운행 23.4 km를 기록했어요" (O) / "기록이 완료되었습니다. 거리는 23.4km입니다" (X)
- 기계 용어를 쓰지 않는다: Odometer → "계기판 거리", inbox → 쓰지 않음, SMTP → "메일 계정", sync → "동기화".
- 버튼은 **동사로 끝나는 결과**: [운행 시작] [보내기] [거래처로 등록]. [확인] [OK]만 단독으로 쓰지 않는다.

### 9.2 오류 문구 형식

`무엇이 안 됐는지` + `무엇을 하면 되는지`, 한두 줄.

| 상황 | 문구 |
|---|---|
| 서버 연결 실패 | "서버에 연결하지 못했어요. 네트워크를 확인해 주세요" |
| 계기판 읽기 실패 | "계기판 거리를 읽지 못했어요. 거리는 나중에 직접 입력할 수 있어요" |
| 메일 인증 실패 | "메일 계정 비밀번호가 맞지 않아요. 설정에서 앱 비밀번호를 확인해 주세요" |
| 검산 실패 | "금액이 맞지 않아요. 수량·단가·할인을 확인해 주세요" |
| 플랫폼이 입력을 막음 | "지금은 입력할 수 없어요" |

- 오류 코드·예외 메시지·영문 원문을 화면에 그대로 보이지 않는다.
- 사과("죄송합니다")를 반복하지 않는다. 해결 방법이 사과보다 낫다.

### 9.3 상태 이름 (고정)

| 의미 | 화면 표기 | 색 |
|---|---|---|
| CONFIRMED | 확정 | `success` |
| PENDING_REVIEW | 확인 필요 | `warning` |
| 운전자 미확정 | 운전자 확인 필요 | `warning` |
| 거리 UNAVAILABLE | 거리 미확정 | `warning` |
| 추천값 | 추천 | `accent` |
| 실패·막힘 | 실패 / 보낼 수 없음 | `danger` |

---

## 10. 아이콘과 이미지

- Material Symbols **Rounded** 스타일(`Icons.Rounded.*`)만 쓴다. 굵기 일정, 모서리 둥근 형태가 Asta Sans와 어울린다.
- 아이콘 크기 `size.icon` 36 dp, 상태 아이콘 `size.iconLg` 48 dp. 아이콘 단독 버튼은 `size.iconButton` 96 dp 원형 + 접근성 설명(`contentDescription`) 필수.
- 아바타는 사진 없이 이니셜(이름 첫 글자, 영문이면 첫 두 글자) + 5.4 그라데이션.
- 일러스트·사진·장식 그래픽을 쓰지 않는다(테슬라식 절제). 빈 상태에도 아이콘 하나와 문장 한 줄이면 충분하다.

---

## 11. 컴포넌트

모든 컴포넌트는 `ui/design/components/`에 있다. **`LocalInputLocked`를 스스로 읽어** 잠금이면 비활성으로 그려지고 탭을 무시한다.

| 컴포넌트 | 사양 |
|---|---|
| `PrimaryButton` | 높이 `buttonLg`(Home은 `buttonXl`), `radius.lg`, `accentFill` 바탕, `label` `onAccent`. 화면당 1개. 상태: 기본 / 눌림(0.97배) / 비활성(바탕 `surface3`, 글자 `textTertiary`) / 로딩(글자를 투명하게 남겨 폭 유지 + 진행 표시, 탭 무시). 로딩 회전은 무한 애니메이션이라 스크린샷 테스트에 넣지 않는다 |
| `SecondaryButton` | 높이 `buttonMd`, `surface3` 바탕(팝업 `surface2` 위에서도 면이 보이게), `textPrimary` 글자 |
| `TertiaryButton` | 바탕 없음, `accent` 글자, 최소 터치 76 dp 유지("나중에 선택" 등) |
| `DangerButton` | `dangerFill` 바탕. 확인 다이얼로그 안에서만 |
| `IconButtonXL` | 96 dp 원형, `surface2` 바탕, 아이콘 36 dp. 마이크·키패드 |
| `ProfileTile` | 정사각, `radius.xl`, 그라데이션 바탕 + 이니셜(흰색, 아바타 크기의 40%) + 아래 이름(`title`)·부서(`caption`). 배차면 3 dp `accent` 링 + 아바타 아래 가장자리에 걸친 "배차" 배지(`accentFill`, `caption`), 최근이면 "최근" 배지(`surface3`). 배지는 이니셜을 가리지 않는다. 크기: WIDE 200 / TALL 160 / NARROW 128 dp. 누르면 `press` |
| `HeroMetric` | 숫자(`hero`/`heroSmall`, tnum) + 단위(`headline`, `textTertiary`) + 아래 라벨(`body`, `textSecondary`). 값이 없으면 숫자 대신 상태 문장 |
| `StatusLine` | 화면 맨 위 상태 한 줄. **배경 없는 점(12 dp) + 글자**: `● 기록 중 · 35분 경과`. 점 색이 상태(기록 중 `success`, 대기 `textTertiary`), 글자 `label` 본문색, 보조(`detail`)는 `body` 회색. 알약 배경이 없어 주인공 숫자와 경쟁하지 않는다. 누를 수 없다(Home·Peek 공용) |
| `DockTile` | 화면 맨 아래 동작. **아이콘 네모 + 그 아래 이름**(앱 아이콘·테슬라 하단 바 방식). 모든 타일이 같은 모양: 네모 80 dp(`surface1`, `radius.lg`) 안에 아이콘 36 dp(`accent`), 아래 이름(`caption`, `textSecondary`, 가운데, 두 줄까지). 이름은 버튼 밖 설명 글씨라 버튼 글자 하한(28 sp) 대신 `caption`. 터치 영역은 네모 + 이름 칸 전체. 4칸 등분(`weight`) 한 줄 — 네모가 같은 간격. `emphasized` = 같은 자리에서 상태가 바뀐 주 동작(네모 `accentFill`, 이름 `accent`) |
| `DriverBadge` | 화면 오른쪽 위 고정(넷플릭스식, 스펙 7.1.1·D-19). 네모 아바타(`avatarChip` 48 dp) + "○○님"(`label`). 미선택이면 `IconAvatar`(사람) + "운전자 선택"(`accent`). FULL은 누르면 운전자 선택, Peek은 표시만. 모달이 뜨면 숨는다. 운전자 이름은 앱 전체에서 **이 자리에만** 보인다 |
| `IconAvatar` | 아바타와 같은 모양(`avatarShape` = 크기의 20% 모서리)의 아이콘 네모, `surface2` 바탕, `textSecondary` 아이콘. 사람 대신 동작·자리표시를 나타낼 때 |
| `ActionTile` | `ProfileTile`과 같은 격자·크기의 동작 타일: `IconAvatar` + 아래 동작 이름(`label`, `textSecondary`, 가운데, 두 줄까지). `[+ 새 운전자 추가]`·`[··· 다른 사람]`. 사람 타일과 같은 뼈대(`TileFrame`)를 공유한다 |
| `SectionHeader` | 한 화면 안 영역의 소제목("이번 달 운행 정보", "최근 운행 기록"). 모든 섹션이 같은 모양: `section`(22 sp 굵게), `textSecondary`, 아래 `space.3`. **섹션 안에는 소제목보다 큰 회색 설명 글씨를 두지 않는다** — 위계는 소제목 → 데이터 값(숫자·목적지, 커도 됨) → 보조 사실(`caption`, `textSecondary`) → 안내(`caption`, `textTertiary`). 카드 안 목록 항목(`TripListRow` 위 줄)은 예외 |
| `Section` | `SectionHeader` + 내용 한 묶음. **같은 정보는 어느 화면·창 크기에서나 같은 소제목**(정보 종류마다 문구 하나): 주행 거리 · 운행 경로 · 누적 거리 · 이번 달 운행 정보 · 최근 운행 기록. Home(WIDE·NARROW·TALL)과 Peek이 함께 쓴다 |
| `TripListRow` | 운행 한 건 목록 줄(Home 최근 운행 기록, 이력). `surface1`, `radius.md`, 위 경로(`label`, 한 줄) · 아래 날짜·거리·목적(`caption`, `textSecondary`). 목적이 없으면 끝이 "목적 미입력" — **색으로 재촉하지 않는다**(D-26). 줄 전체가 터치 대상, 끝에 › |
| `ModalCard` | **앱의 유일한 팝업 모양**(D-21). 화면 가운데 둥근 카드: `radius.xl`, 바탕 `surfaceRaised`(다크 `surface2` / 라이트 `surface1`), `outline` 머리카락 테두리(1 dp), 그림자 없음, 뒤 `scrim`. 기본 최대 폭 640 dp(목록·두 칸은 960 dp), 화면 가장자리에서 `space.6`. 안쪽 `space.6`. 내용은 넘치면 스크롤 + 가장자리 흐림, **결정 버튼은 `footer`(하단 고정)**. 등장: scrim 투명도 + 카드 0.94→1 배율(`motion.standard`). 키보드가 올라오면 같이 올라간다. 바깥 탭은 삼키고 닫지 않는다(입력이 날아가지 않게). 단계가 FULL에서 내려가면 즉시 닫힘. 아래에 붙는 시트는 쓰지 않는다. **제목·설명 문구는 가운데 정렬** — 직접 `Text`를 쓰지 않고 `ModalTitle`(`headline`)·`ModalText`(기본 `body`·`textSecondary`, 강조 시 style·color만 바꿈)를 쓴다. **취소·닫기는 모두 오른쪽 위 X**(`onClose` → `CloseButton`) — 하단 [취소]·[닫기] 버튼은 두지 않는다. 반드시 골라야 하는 결정 팝업(복구 첫 단계 등)은 X 없이 선택지로만 끝난다. 제목이 있으면 `title`로 X와 같은 머리 줄 가운데에 둔다(iOS 시트 배치). 앞 화면에서 무엇을 펼쳤는지 분명하면 제목을 두지 않는다(같은 정보 반복 금지) X가 닫기 이상의 뜻이면(놓친 운행의 "무시") `closeLabel`로 접근성 이름을 그 뜻으로 바꾼다 |
| `CloseButton` | 닫기 X. 보이는 원 48 dp(`surface3`) + 아이콘 28 dp(`textSecondary`), 터치 영역 76 dp. 접근성 이름 "닫기" |
| `ConfirmDialog` | 제목(`headline`) + 설명 1~2줄 + 버튼 2개(위험 동작이면 `DangerButton`). 되돌리기 어려운 동작에만 |
| `InputField` | **채움형**: 라벨(`caption`, `textSecondary`) 위, 칸은 `surface3` 바탕·`radius.md`·높이 76 dp·테두리 없음, 초점이면 3 dp `accent` 링. 값 `body`, 자리표시(`textTertiary`)는 "예: 홍길동"처럼 예시로. 잠금이면 `surface2` + `textTertiary` + "지금은 입력할 수 없어요". 텍스트 입력은 최후의 수단 |
| `NumericKeypad` | 3×4 키, 키 96 dp, 키 사이 `space.4`. 입력값은 위에 `heroSmall`로 표시 |
| `ConfidenceRow` | 영수증 필드 한 줄: 라벨 · 값 · 신뢰도 점(초록/노랑/빨강) + 노랑·빨강이면 "확인 필요" 글자와 `warning`/`danger` 테두리 |
| `OptionGroup` | 선택지 묶음 하나(예: 목적 확인의 일반 업무용·출퇴근용·비업무용). 묶음 이름(`caption`, `textSecondary`) 아래 `OptionButton`(`buttonMd`)들을 줄바꿈하며 놓는다. 선택되면 `accent` 링 + 체크. 고르면 바로 실행되는 곳에 쓴다 |
| `TopLine` · `ScrollColumn` | 전체 화면 공용 뼈대. `TopLine` = 맨 위 76 dp 줄(상태 줄·화면 제목), 오른쪽은 운전자 배지 자리로 비운다. `ScrollColumn` = 위쪽 정렬 + 넘치면 스크롤 + 가장자리 흐림 |
| `EmptyState` | 아이콘(`iconLg`, `textTertiary`) + 문장(`body`) + 선택 버튼 1개 |
| `PeekPanel` | 정차 전용 읽기 패널. `bg` 위 가운데 세로 스택, 최대 폭 560 dp. ① 상태 줄(`StatusLine`: 초록 점 + `기록 중`, 터치 불가. 운전자는 오른쪽 위 `DriverBadge`) ② 섹션 "주행 거리"(`HeroMetric`, `heroSmall`) ③ 섹션 "운행 경로"(`RouteFlow`: 출발지 → 흐르는 화살표 → 목적지) — 소제목·모양은 Home과 같은 `Section` ④ 비워 둠(결정 대기) ⑤ 상황 줄(있을 때만, `body`, `warning`). **클릭 가능한 요소 0개**, 모션은 경로 화살표 흐름만 |
| `RouteFlow` | **경로 선**: 왼쪽 표시 칸(폭 28 dp)에 출발 ○(속 빈 원 12 dp, `textSecondary`) → 갈매기 3개 화살표(폭 24 dp, 칸 12 dp, 선 3 dp 둥근 끝, `accent`) → 도착 ●(채운 원 12 dp, `accent`)을 세로로, 오른쪽 글자 칸에 장소 이름. 위계는 소제목(`section` 22 굵게) → **목적지(`label` 28, `textPrimary`, 두 줄까지 + 말줄임 — 값)** → 출발지(`caption` 22 보통, `textSecondary`, 한 줄 — 보조). 표시는 첫 줄 글자 높이의 가운데에 맞춘다. 빛의 머리가 위에서 아래로 지나가며 가까운 갈매기를 밝힌다(꺼진 갈매기도 투명도 0.18로 남아 방향이 늘 읽힘), 한 바퀴 1.4초 뒤 짧은 쉼. `motion.ambient`가 꺼지면 위→아래로 짙어지는 정지 그림. 출발지를 모르면 ●과 목적지만 `live = false`(끝난 운행 — 목적 확인·이력)면 흐름 모션 없이 정지 그림 |
| `AutoEndPrompt` | 기어 P 자동 종료 팝업(`ModalCard`, 버튼은 footer). 제목 "기록을 마칠까요?"(`headline`) · 요약 `23.4 km · 목적지`(`title`) · 남은 시간 `5초 후 기록이 자동 종료돼요`(`body`, tnum, 1초마다 숫자만 교체 5→1) · 줄어드는 진행 막대(높이 8 dp, `radius.full`, 채움 `accent`, 바탕 `surface3`, 선형) · 왼쪽 [기록 종료](`PrimaryButton`) · 오른쪽 [계속 기록](`SecondaryButton`). 남은 시간은 `deadlineAt − now`로 계산하고 컴포넌트가 타이머를 갖지 않는다 |

새 컴포넌트가 필요하면 이 표에 먼저 추가하는 PR을 올린다.

---

## 12. 상태 표현

| 상태 | 표현 |
|---|---|
| 로딩 < 250 ms | 아무것도 보이지 않는다(깜빡임 방지) |
| 로딩 250 ms – 1 s | 해당 버튼만 로딩 상태 |
| 로딩 > 1 s | `motion.loading` + 무엇을 하는지 한 줄("운행기록부를 만들고 있어요") |
| 빈 목록 | `EmptyState`: 왜 비었는지 + 할 수 있는 일 |
| 오프라인 | 화면을 막지 않는다. 상단 칩 "오프라인 · 연결되면 보내요". 로컬 기능은 그대로 |
| 값 없음(계기판 실패 등) | 숫자 자리에 상태 문장. 0이나 추정값을 그리지 않는다 |
| 서버 배포 항목 | 잠금 아이콘 + "회사에서 등록" 캡션, 삭제 불가 |
| 정차 (PEEK) | 전체 앱 대신 Peek 패널 하나. 입력 요소 없음 |
| 주행 (BACKGROUND) | 화면 없음. 앱이 스스로 물러난다 |
| FULL로 바뀌는 순간 | 오조작 방지로 첫 300 ms 동안 입력 무시(이중 안전) |

---

## 13. 리뷰 체크리스트 (화면 PR마다)

- [ ] 1초에 알아야 할 것이 가장 크다
- [ ] 주 버튼이 하나이고 운전자 쪽 하단에 있다
- [ ] 모든 색이 토큰이고, `ContrastTest`가 통과한다
- [ ] 글자가 `caption` 이상이고, 주요 정보는 `title` 이상이다
- [ ] 모든 터치 대상이 76 dp 이상, 서로 16 dp 이상 떨어져 있다
- [ ] WIDE·NARROW·TALL × 다크·라이트 스크린샷이 있고 잘림·겹침이 없다
- [ ] 주차(FULL) 전용 화면이다 — 정차면 Peek, 주행이면 화면 없음으로 바뀐다
- [ ] 조작 예산 안이다(이 결정에 팔을 몇 번 드는지 셌다)
- [ ] 자동 진행이 있으면 남은 시간과 1탭 취소가 보이고, 취소 쪽이 주 버튼이다
- [ ] 모션은 상태를 설명할 때만, 토큰으로만 썼다
- [ ] 문구가 해요체·결론 먼저·한두 줄이다. 오류 문구에 해결 방법이 있다
- [ ] 모르는 값을 추정해 그리지 않았다
- [ ] 아이콘 단독 버튼에 `contentDescription`이 있다

## 14. 하지 않는 것

- 주행 중 화면 노출, 정차(Peek) 중 버튼·입력·스크롤
- Peek의 애니메이션, 깜빡임, 펄스, 자동 스크롤, 슬라이드 전환
- 선택 후 [확인]을 한 번 더 누르게 하는 2단계 조작, 되돌릴 수 있는 동작의 확인 다이얼로그
- 남은 시간·취소 없이 자동으로 결정하는 것
- 장식 애니메이션, 무한 반복 애니메이션(주차 중 로딩 제외)
- 그림자로 계층 표현, 바텀 시트, 테두리만 있는 입력 칸, 그라데이션 배경(아바타 제외)
- 강조색 두 번째 추가, 의미 없는 색
- 일러스트·사진·이모지 장식
- 아이콘만 있는 작은 버튼, 76 dp 미만 터치 대상
- 화면 위쪽 구석의 주 동작
- 한 화면에 주 버튼 두 개
- 영문·기계 용어, 오류 코드 노출
- 추정 거리 표시
- 테슬라·넷플릭스의 로고·고유 색·아이콘·레이아웃 복제

---

## 15. 출처

- Pleos: [UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro) · [타이포그래피](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/typography) · [컬러](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/colors) · [애니메이션](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/animation) · [디스플레이 레이아웃](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/layout) · [Window Policy](https://document.pleos.ai/docs/connect/guide/docs-design/getting-started/window) · [운전자 주의 분산 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)
- AAOS: [Driver distraction guidelines](https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko)
- 서체: [Asta Sans (google/fonts 메타데이터)](https://raw.githubusercontent.com/google/fonts/main/ofl/astasans/METADATA.pb) · [42dot/Asta-Sans](https://github.com/42dot/Asta-Sans)
- 대비 계산: WCAG 2.x 상대 휘도 공식. 계산 스크립트는 `ContrastTest`로 옮긴다.

## 16. 변경 이력

| 버전 | 날짜 | 내용 |
|---|---|---|
| 1.0 | 2026-11-09 | 최초 확정 (사용 3단계·조작 최소화 포함) |
| 1.1 | 2026-11-09 | 운전자 배지 오른쪽 위 고정(D-19) · `DriverBadge`·`IconAvatar`·`ActionTile` 추가 · `ActionCard` 세로 배치 · 축약 `StatusChip` · 오른쪽 위 예약 자리(`LocalTopEndReserved`) · 스크롤 가장자리 흐림 · Home WIDE 45:55 · 주유 기록 버튼 하나로(D-20) |
| 1.2 | 2026-11-09 | 팝업을 가운데 카드 `ModalCard` 하나로 통일(D-21, `ScrimSheet` 삭제), 팝업 제목·설명 가운데 정렬(`ModalTitle`·`ModalText`) · 취소·닫기는 모두 오른쪽 위 X(`CloseButton`, `ModalCard.onClose`), 제목은 X와 같은 줄(`ModalCard.title`) · `surfaceRaised` · 채움형 `InputField` · `AvatarFace` · 새 운전자 추가 팝업 고도화 |
| 1.3 | 2026-11-09 | Home 재구성(스펙 D-23): 상태 줄 · 주인공 하나(이번 달 요약 / 이번 운행 + 경로) · 알림 목록(`NoticeRow`) · 하단 독(`DockButton`). `ActionCard`·`Banner`·`CountBadge` 삭제, `StatusChip` `MUTED` 톤 추가. 경로 화살표 `RouteFlow`·반복 상태 모션(`motion.ambient`) |
| 1.4 | 2026-11-09 | Home 위쪽 좌우 반반 두 섹션 + `SectionHeader`, 아래 같은 모양 타일 4개 `DockTile`(스펙 D-24) — 아이콘 네모 80 dp + 아래 `caption` 이름, 언제나 한 줄. `DockButton`·`QuickAction` 삭제 |
| 1.5 | 2026-11-09 | 글자 위계: `section` 토큰(22 sp 700) 추가, `SectionHeader`가 사용. Home 왼쪽 = 소제목 "이번 달 운행 정보" → 숫자 → 보조 사실 한 줄(`23회 운행 · 업무 388.1 km`, caption). `RouteFlow` 출발지 caption, "새 알림이 없어요" caption |
| 1.6 | 2026-11-09 | Home "정상은 침묵"(스펙 D-25): `StatusLine`(점 + 글자)이 `StatusChip`을 대체(삭제), 알림 띠 `NoticePill` + `OverflowRow`(있을 때만), `TripListRow`(최근 운행 기록 3건), 기록 문제 알림 3종, 거리 72 sp, `RouteFlow` 장소 두 개 같은 스타일 |
| 1.7 | 2026-11-09 | 소제목 통일: "이번 운행" → "주행 거리", 공용 `Section`으로 Home·Peek이 같은 소제목(주행 거리·운행 경로·누적 거리). 상태 문구 키 `status_recording`·`status_ready` 하나로 |
| 1.8 | 2026-11-09 | `RouteFlow`를 경로 선으로: 출발 ○ · 흐르는 화살표 · 도착 ● 표시 칸 + 글자 칸. 출발지 caption(보조) · 목적지 label(값)으로 위계를 되돌림(1.6의 "같은 스타일"은 좁은 창에서 출발지가 소제목보다 커 보이는 역전을 만들었다). 화살표 칸 16 → 12 dp로 줄여 504×640에서도 목적지까지 스크롤 없이. `Size.routeDot` 추가 |
| 1.9 | 2026-11-09 | 앱 안 알림 없음(스펙 D-26): `NoticeRow`·`NoticePill`·`OverflowRow` 삭제, Home은 상태 줄 → 두 섹션 → 타일. `TripListRow` 경고색 제거(목적 없음은 회색 문장). 놓친 운행은 `ModalCard` 팝업(X = 무시, `ModalCard.closeLabel`로 접근성 이름을 그 뜻으로) |
| 1.10 | 2026-11-09 | 누를 수 있는 요소의 공통 동작을 `Modifier.tappable` 하나로(입력 잠금·누름 0.97배·햅틱, 물결 없음) — `ConfidenceRow`도 누름 피드백·햅틱을 갖게 됨. 화면 바깥 여백 `screenMargin()` 하나로 |
| 1.11 | 2026-11-09 | 목적 확인 화면(스펙 7.3): `IconButtonXL`(마이크) 구현, `SegmentedPurpose` → 범용 `OptionGroup`, 공용 뼈대 `TopLine`·`ScrollColumn`(Home에서 승격), `RouteFlow(live)`. 팝업 머리 줄 제목은 짧은 명사("거래처 등록"), 질문은 본문 첫 줄 |
