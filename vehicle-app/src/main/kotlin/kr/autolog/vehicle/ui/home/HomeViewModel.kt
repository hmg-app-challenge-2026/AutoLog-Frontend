package kr.autolog.vehicle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.common.currentMonth
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.data.repository.ContextRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.repository.TripRepository
import kr.autolog.vehicle.data.repository.TripSession
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

class HomeViewModel(
    private val session: SessionRepository,
    context: ContextRepository,
    trips: TripRepository,
    private val navigator: Navigator,
    clock: AppClock,
    ticker: Flow<Long> = tickerFlow(clock),
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            combine(session.state, session.missedKm, context.destinationLabel, ::Triple),
            // 대기 왼쪽 = 이번 달 요약, 오른쪽 = 최근 운행 기록(D-25). 화면이 열려 있는 동안 달이 바뀌는 경우는 다음 진입 때 맞춘다
            combine(trips.summary(clock.currentMonth()), trips.recent(RECENT_TRIPS), ::Pair),
            ticker,
        ) { (s, missed, dest), (month, recent), now ->
            reduceHome(s, missed, dest, now, month, recent)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // 운행이 끝나 목적 확인 대기가 되면 바로 ReviewTrip을 연다(스펙 7.3 진입 1번). 같은 운행은 한 번만.
        viewModelScope.launch {
            var opened: String? = null
            session.state
                .filterIsInstance<TripSession.PendingReview>()
                .map { it.tripId }
                .distinctUntilChanged()
                .collect { id -> if (id != opened && navigator.push(Route.ReviewTrip(id))) opened = id }
        }
    }

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            when (action) {
                HomeAction.StartTrip -> session.startTrip()
                HomeAction.EndNow -> session.endNow()
                HomeAction.KeepRecording -> session.keepRecording()
                HomeAction.AcceptMissedTrip -> session.acceptMissedTrip()
                HomeAction.IgnoreMissedTrip -> session.ignoreMissedTrip()
                is HomeAction.Recover -> session.resolveRecovery(action.action)
                HomeAction.AddFuelRecord -> navigator.push(Route.ReceiptQr)
                HomeAction.OpenRecords -> navigator.push(Route.History)
                HomeAction.OpenSettings -> navigator.push(Route.Settings)
            }
        }
    }
}
