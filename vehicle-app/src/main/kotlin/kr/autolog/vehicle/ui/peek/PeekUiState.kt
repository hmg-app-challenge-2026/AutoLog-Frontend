package kr.autolog.vehicle.ui.peek

/** 정차 패널 상태(스펙 7.0). 계산은 [reducePeek](PeekReducer.kt). */
sealed interface PeekUiState {
    data object Loading : PeekUiState

    /** [tripKm] null = 계기판 미확정. [origin] null = 출발지 모름(경로 대신 목적지만). [autoEndInSec] = 움직임 없음·내비 도착 자동 종료까지 남은 초. */
    data class Recording(val tripKm: String?, val origin: String?, val destination: String?, val autoEndInSec: Int?) :
        PeekUiState

    data class NoTrip(val odometerKm: String?) : PeekUiState
}
