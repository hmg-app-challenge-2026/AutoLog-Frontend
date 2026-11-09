package kr.autolog.vehicle.ui.peek

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.data.repository.ContextRepository
import kr.autolog.vehicle.data.repository.ProfileRepository
import kr.autolog.vehicle.data.repository.SessionRepository

/** 읽기 전용 — Action이 없다. */
class PeekViewModel(
    session: SessionRepository,
    profile: ProfileRepository,
    context: ContextRepository,
    clock: AppClock,
    ticker: Flow<Long> = tickerFlow(clock),
) : ViewModel() {
    val uiState: StateFlow<PeekUiState> =
        combine(session.state, session.totalOdometerKm, profile.currentDriver(), context.destinationLabel, ticker) {
                s,
                odo,
                driver,
                dest,
                now,
            ->
            reducePeek(s, odo, driver?.name, dest, now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeekUiState.Loading)
}
