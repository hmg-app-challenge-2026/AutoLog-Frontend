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
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.repository.ContextRepository
import kr.autolog.vehicle.data.repository.ProfileRepository
import kr.autolog.vehicle.data.repository.ReceiptRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.repository.TripRepository
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

class HomeViewModel(
    private val session: SessionRepository,
    profile: ProfileRepository,
    context: ContextRepository,
    trips: TripRepository,
    receipts: ReceiptRepository,
    private val navigator: Navigator,
    clock: AppClock,
    ticker: Flow<Long> = tickerFlow(clock),
) : ViewModel() {
    private val pendingIds = trips.pendingReviewIds.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val receiptIds =
        receipts.inbox.map { list ->
            list.map { it.receiptId }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<HomeUiState> =
        combine(
            combine(session.state, session.totalOdometerKm, session.missedTrip, ::Triple),
            combine(profile.currentDriver(), context.destinationLabel, ::Pair),
            combine(pendingIds, receiptIds, ::Pair),
            ticker,
        ) { (s, odo, missed), (driver, dest), (pending, receipts), now ->
            reduceHome(s, odo, missed, driver, dest, now, pending, receipts)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // 운행이 끝나 목적 확인 대기가 되면 바로 ReviewTrip을 연다(스펙 7.3 진입 1번). 같은 운행은 한 번만.
        viewModelScope.launch {
            var opened: String? = null
            session.state
                .filterIsInstance<SessionState.PendingReview>()
                .map { it.trip.tripId }
                .distinctUntilChanged()
                .collect { id -> if (id != opened && navigator.push(Route.ReviewTrip(id))) opened = id }
        }
    }

    fun onAction(action: HomeAction) {
        val state = uiState.value
        viewModelScope.launch {
            when (action) {
                HomeAction.StartTrip -> session.startTrip()
                HomeAction.EndNow -> session.endNow()
                HomeAction.KeepRecording -> session.keepRecording()
                HomeAction.OpenProfilePicker -> navigator.push(Route.ProfilePicker)
                HomeAction.AcceptMissedTrip -> session.acceptMissedTrip()
                HomeAction.IgnoreMissedTrip -> session.ignoreMissedTrip()
                is HomeAction.Recover -> session.resolveRecovery(action.choice)
                HomeAction.OpenReview -> pendingIds.value.firstOrNull()?.let { navigator.push(Route.ReviewTrip(it)) }
                HomeAction.OpenReceipt -> receiptIds.value.firstOrNull()?.let {
                    navigator.push(Route.ReceiptReview(it))
                }
                HomeAction.OpenReceiptQr -> navigator.push(Route.ReceiptQr)
                HomeAction.OpenFuel -> navigator.push(Route.FuelEntry(state.currentTripId))
                HomeAction.OpenReport -> navigator.push(Route.Report)
                HomeAction.OpenHistory -> navigator.push(Route.History)
                HomeAction.OpenSettings -> navigator.push(Route.Settings)
            }
        }
    }
}
