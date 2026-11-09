package kr.autolog.vehicle.ui.result

import kr.autolog.vehicle.core.PurposeCategory

/** 결과를 보여 주고 이 시간이 지나면 지금 선택으로 저장하고 메인 화면으로(D-27). */
const val AUTO_CLOSE_SEC = 5

/** 고를 수 있는 분류(위에서부터): 일반 업무 · 출퇴근 · 개인 용무. "미확정"은 고르는 값이 아니다. */
val PURPOSE_CHOICES: List<PurposeCategory> =
    listOf(PurposeCategory.BUSINESS, PurposeCategory.COMMUTE, PurposeCategory.NON_BUSINESS)

/**
 * 운행 결과 카드(스펙 7.3, D-27). 처음엔 **보기 전용** + 5초 카운트다운, [수정하기]를 누르면 카운트다운이 멈추고 목적을 바꿀 수 있다.
 * [minutes] null = 끝난 시각 모름, [km] null = 거리 미확정(추정 금지).
 */
data class TripResultUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val km: String? = null,
    val minutes: Int? = null,
    val origin: String? = null,
    val destination: String? = null,
    val selected: PurposeCategory = PurposeCategory.BUSINESS,
    val editing: Boolean = false,
    /** 메인 화면으로 돌아가기까지 남은 초. null = 멈춤(수정 중). */
    val autoCloseSec: Int? = null,
    val saving: Boolean = false,
) {
    val autoCloseProgress: Float get() = (autoCloseSec ?: 0) / AUTO_CLOSE_SEC.toFloat()
}

/** 사용자 조작으로 바뀌는 값 — ViewModel이 들고 있고 [reduceTripResult]가 운행 정보와 합친다. */
data class TripResultInteraction(
    /** 사용자가 고른 분류. null = 아직 안 건드림(저장된 분류 또는 기본값). */
    val selected: PurposeCategory? = null,
    val editing: Boolean = false,
    val autoCloseSec: Int? = null,
    val saving: Boolean = false,
)

sealed interface TripResultAction {
    /** 수정 모드에서만 받는다. */
    data class Select(val category: PurposeCategory) : TripResultAction

    /** 카운트다운을 멈추고 목적을 바꿀 수 있게 한다. */
    data object Edit : TripResultAction

    /** 고친 목적을 저장하고 보기 모드로 — 카운트다운이 다시 돈다. */
    data object Save : TripResultAction

    /** 지금 선택으로 저장하고 메인 화면으로(언제든). */
    data object GoHome : TripResultAction
}
