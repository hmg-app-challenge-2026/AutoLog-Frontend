package kr.autolog.vehicle.ui.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle

/**
 * 창 크기([WindowLayout])에 따라 글자 크기가 달라지는 **역할** 모음(디자인 언어 6.2).
 * 화면·컴포넌트는 `LocalWindowLayout`으로 글자를 직접 고르지 않고 `AutoLogTheme.windowType.<역할>`을 쓴다 —
 * 창 크기별 글자 결정이 이 파일 한 곳에 모여, 표 하나만 보면 좁은 창에서 무엇이 한 단계 작아지는지 알 수 있다.
 * 값은 Pleos Connect v2.1.0 에뮬레이터(2560×1440 px, 160 dpi)에서 맞춘 것(PR #4, 2026-11-10 사용자 확인).
 */
@Immutable
data class WindowType(
    /** 전체 화면 제목("운전자를 선택해주세요"). */
    val screenTitle: TextStyle,
    /** 팝업 제목(`ModalTitle`). */
    val modalTitle: TextStyle,
    /** 운전자 타일의 이름. */
    val tileName: TextStyle,
    /** 동작 타일의 이름("새 운전자 추가") — 사람 이름보다 길어 한 단계 작다. */
    val tileAction: TextStyle,
    /** 타일 아래 보조 줄(부서). */
    val tileCaption: TextStyle,
    /** 하단 타일(`DockTile`) 이름. */
    val dockLabel: TextStyle,
    /** 운행 결과 맨 위 제목("운행 기록을 완료했어요"). */
    val resultTitle: TextStyle,
    /** 운행 결과 분류 문구("일반 업무용 거리 48.2 km로 분류돼요"). */
    val resultClassification: TextStyle,
    /** 운행 결과 [메인 화면으로 · n초] 글자 — 넓은 창은 남은 시간까지 한 줄에 들어가게 한 단계 작게. */
    val resultHomeButton: TextStyle,
    /** 기간 이동 가운데 글자("2026년 11월" — 운행 기록 조회·운행기록부). */
    val periodTitle: TextStyle,
    /** 보조 화면 머리 줄 제목(`ScreenHeader` — "운행 기록", "운행기록부 내보내기"). */
    val headerTitle: TextStyle,
)

/** [layout]에 맞는 역할별 글자. 순수 함수 — `WindowTypeTest`가 표를 고정한다. */
fun windowTypeOf(layout: WindowLayout, t: AutoLogType): WindowType = when (layout) {
    WindowLayout.WIDE -> WindowType(
        screenTitle = t.display,
        modalTitle = t.headline,
        tileName = t.title,
        tileAction = t.label,
        tileCaption = t.caption,
        dockLabel = t.label,
        resultTitle = t.title,
        resultClassification = t.title,
        resultHomeButton = t.caption,
        periodTitle = t.title,
        headerTitle = t.title,
    )
    WindowLayout.TALL -> WindowType(
        screenTitle = t.display,
        modalTitle = t.headline,
        tileName = t.title,
        tileAction = t.label,
        tileCaption = t.caption,
        dockLabel = t.label,
        resultTitle = t.title,
        resultClassification = t.label,
        resultHomeButton = t.label,
        periodTitle = t.title,
        headerTitle = t.title,
    )
    // 분할 화면 한 칸: 한 단계씩 작게
    WindowLayout.NARROW -> WindowType(
        screenTitle = t.headline,
        modalTitle = t.label,
        tileName = t.label,
        tileAction = t.caption,
        tileCaption = t.fine,
        dockLabel = t.caption,
        resultTitle = t.label,
        resultClassification = t.label,
        resultHomeButton = t.label,
        periodTitle = t.label,
        headerTitle = t.label,
    )
}
