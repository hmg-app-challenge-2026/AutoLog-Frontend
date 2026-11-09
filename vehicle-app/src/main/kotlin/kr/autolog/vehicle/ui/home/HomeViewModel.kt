package kr.autolog.vehicle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.data.repository.ContextRepository
import kr.autolog.vehicle.data.repository.ProfileRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

class HomeViewModel(
    private val session: SessionRepository,
    profile: ProfileRepository,
    context: ContextRepository,
    private val navigator: Navigator,
    clock: AppClock,
    ticker: Flow<Long> = tickerFlow(clock),
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> =
        combine(
            combine(session.state, session.totalOdometerKm, session.missedTrip, ::Triple),
            profile.currentDriver(),
            context.destinationLabel,
            ticker,
        ) { (s, odo, missed), driver, dest, now -> reduceHome(s, odo, missed, driver, dest, now) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onAction(action: HomeAction) {
        viewModelScope.launch {
            when (action) {
                HomeAction.StartTrip -> session.startTrip()
                HomeAction.EndNow -> session.endNow()
                HomeAction.KeepRecording -> session.keepRecording()
                HomeAction.OpenProfilePicker -> navigator.push(Route.ProfilePicker)
                HomeAction.AcceptMissedTrip -> session.acceptMissedTrip()
                HomeAction.IgnoreMissedTrip -> session.ignoreMissedTrip()
                is HomeAction.Recover -> session.resolveRecovery(action.choice)
            }
        }
    }
}
