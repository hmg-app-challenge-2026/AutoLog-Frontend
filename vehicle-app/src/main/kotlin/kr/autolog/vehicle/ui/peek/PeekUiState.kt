package kr.autolog.vehicle.ui.peek

import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.remainingSeconds

sealed interface PeekUiState {
    data object Loading : PeekUiState

    /** [tripKm] null = 계기판 미확정. [origin] null = 출발지 모름(경로 대신 목적지만). [autoEndInSec] = 움직임 없음·내비 도착 자동 종료까지 남은 초. */
    data class Recording(val tripKm: String?, val origin: String?, val destination: String?, val autoEndInSec: Int?) :
        PeekUiState

    data class NoTrip(val odometerKm: String?) : PeekUiState
}

/** 스펙 7.0 내용 ①~⑤. 순수 함수(테스트 대상). */
internal fun reducePeek(state: SessionState, odometerKm: Double?, destination: String?, now: Long): PeekUiState =
    when (state) {
        is SessionState.Recording ->
            PeekUiState.Recording(
                tripKm = formatKm(state.liveKm),
                origin = state.trip.originLabel,
                destination = destination,
                autoEndInSec = null,
            )
        is SessionState.StopCandidate ->
            PeekUiState.Recording(
                tripKm = formatKm(state.liveKm),
                origin = state.trip.originLabel,
                destination = destination,
                autoEndInSec = if (state.reason == StopReason.GEAR_P) null else remainingSeconds(state.deadlineAt, now),
            )
        else -> PeekUiState.NoTrip(formatKm(odometerKm))
    }
