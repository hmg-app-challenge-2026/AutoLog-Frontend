package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.progressOf
import kr.autolog.vehicle.ui.design.remainingSeconds

/** 스펙 7.2 상태 4가지. */
enum class HomePhase { READY, RECORDING_PARKED, AUTO_ENDING, NEEDS_RECOVERY }

data class AutoEndUi(val remainingSec: Int, val progress: Float, val reason: StopReason)

data class HomeUiState(
    val phase: HomePhase = HomePhase.READY,
    val odometerKm: String? = null,
    /** 기록 중 거리. null = 계기판 미확정(추정 금지). */
    val tripKm: String? = null,
    val destination: String? = null,
    val elapsedMin: Int? = null,
    val autoEnd: AutoEndUi? = null,
    /** 목적 확인이 필요한 운행 수(저장소 기준, 방금 끝난 운행 포함). */
    val reviewCount: Int = 0,
    /** 칩을 누르면 열 운행(가장 최근). */
    val latestReviewId: String? = null,
    /** inbox로 도착해 아직 확정하지 않은 영수증. */
    val receiptCount: Int = 0,
    val latestReceiptId: String? = null,
    val missedTripKm: String? = null,
    val recoveryExtraKm: String? = null,
)

sealed interface HomeAction {
    data object StartTrip : HomeAction

    data object EndNow : HomeAction

    data object KeepRecording : HomeAction

    data object AcceptMissedTrip : HomeAction

    data object IgnoreMissedTrip : HomeAction

    data class Recover(val choice: RecoveryChoice) : HomeAction

    data object OpenReview : HomeAction

    /** "영수증 도착" 칩 — 가장 최근 영수증의 OCR 값이 채워진 주유 입력 창(스펙 7.5). */
    data object OpenReceipt : HomeAction

    /** 주유 기록 추가 — 영수증 QR로 들어간다(스펙 7.4). */
    data object AddFuelRecord : HomeAction

    data object OpenReport : HomeAction

    data object OpenHistory : HomeAction

    data object OpenSettings : HomeAction
}

internal fun reduceHome(
    state: SessionState,
    odometerKm: Double?,
    missed: MissedTrip?,
    destination: String?,
    now: Long,
    pendingReviewIds: List<String> = emptyList(),
    receiptIds: List<String> = emptyList(),
): HomeUiState {
    val base =
        HomeUiState(
            odometerKm = formatKm(odometerKm),
            destination = destination,
            reviewCount = pendingReviewIds.size,
            latestReviewId = pendingReviewIds.firstOrNull(),
            receiptCount = receiptIds.size,
            latestReceiptId = receiptIds.firstOrNull(),
        )
    return when (state) {
        SessionState.Idle -> base.copy(missedTripKm = formatKm(missed?.gapKm))
        is SessionState.PendingReview -> base.copy(missedTripKm = formatKm(missed?.gapKm))
        is SessionState.Recording ->
            base.copy(
                phase = HomePhase.RECORDING_PARKED,
                tripKm = formatKm(state.liveKm),
                elapsedMin = ((now - state.trip.startTime) / 60_000).toInt().coerceAtLeast(0),
            )
        is SessionState.StopCandidate ->
            base.copy(
                phase = HomePhase.AUTO_ENDING,
                tripKm = formatKm(state.liveKm),
                autoEnd =
                AutoEndUi(
                    remainingSec = remainingSeconds(state.deadlineAt, now),
                    progress = progressOf(state.since, state.deadlineAt, now),
                    reason = state.reason,
                ),
            )
        is SessionState.NeedsRecovery -> base.copy(
            phase = HomePhase.NEEDS_RECOVERY,
            recoveryExtraKm = formatKm(state.extraKm),
        )
    }
}
