package kr.autolog.vehicle.ui.peek

import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.remainingSeconds

sealed interface PeekUiState {
    data object Loading : PeekUiState

    /** [tripKm] null = 계기판 미확정. [autoEndInSec] = 움직임 없음·내비 도착 자동 종료까지 남은 초. */
    data class Recording(
        val driverName: String?,
        val tripKm: String?,
        val destination: String?,
        val savedSecondsAgo: Int,
        val autoEndInSec: Int?,
    ) : PeekUiState

    data class NoTrip(val odometerKm: String?) : PeekUiState
}

/** 스펙 7.0 내용 ①~⑤. 순수 함수(테스트 대상). */
internal fun reducePeek(
    state: SessionState,
    odometerKm: Double?,
    driverName: String?,
    destination: String?,
    now: Long,
): PeekUiState = when (state) {
    is SessionState.Recording ->
        PeekUiState.Recording(
            driverName = driverName,
            tripKm = formatKm(state.liveKm),
            destination = destination,
            savedSecondsAgo = ((now - state.lastSavedAt) / 1000).toInt().coerceAtLeast(0),
            autoEndInSec = null,
        )
    is SessionState.StopCandidate ->
        PeekUiState.Recording(
            driverName = driverName,
            tripKm = formatKm(state.liveKm),
            destination = destination,
            savedSecondsAgo = 0,
            autoEndInSec = if (state.reason == StopReason.GEAR_P) null else remainingSeconds(state.deadlineAt, now),
        )
    else -> PeekUiState.NoTrip(formatKm(odometerKm))
}
