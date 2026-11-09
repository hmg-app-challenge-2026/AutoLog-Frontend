package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.progressOf
import kr.autolog.vehicle.ui.design.remainingSeconds

/** 스펙 7.2 상태 4가지. */
enum class HomePhase { READY, RECORDING_PARKED, AUTO_ENDING, NEEDS_RECOVERY }

data class AutoEndUi(val remainingSec: Int, val progress: Float, val reason: StopReason)

data class HomeUiState(
    val phase: HomePhase = HomePhase.READY,
    val driver: DriverProfile? = null,
    val odometerKm: String? = null,
    /** 기록 중 거리. null = 계기판 미확정(추정 금지). */
    val tripKm: String? = null,
    val destination: String? = null,
    val elapsedMin: Int? = null,
    val autoEnd: AutoEndUi? = null,
    val reviewCount: Int = 0,
    val missedTripKm: String? = null,
    val recoveryExtraKm: String? = null,
)

sealed interface HomeAction {
    data object StartTrip : HomeAction

    data object EndNow : HomeAction

    data object KeepRecording : HomeAction

    data object OpenProfilePicker : HomeAction

    data object AcceptMissedTrip : HomeAction

    data object IgnoreMissedTrip : HomeAction

    data class Recover(val choice: RecoveryChoice) : HomeAction
}

internal fun reduceHome(
    state: SessionState,
    odometerKm: Double?,
    missed: MissedTrip?,
    driver: DriverProfile?,
    destination: String?,
    now: Long,
): HomeUiState {
    val base = HomeUiState(driver = driver, odometerKm = formatKm(odometerKm), destination = destination)
    return when (state) {
        SessionState.Idle -> base.copy(missedTripKm = formatKm(missed?.gapKm))
        is SessionState.PendingReview -> base.copy(reviewCount = 1, missedTripKm = formatKm(missed?.gapKm))
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
        is SessionState.NeedsRecovery -> base.copy(phase = HomePhase.NEEDS_RECOVERY, recoveryExtraKm = formatKm(state.extraKm))
    }
}
