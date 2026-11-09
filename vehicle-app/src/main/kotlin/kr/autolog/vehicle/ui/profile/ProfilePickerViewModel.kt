package kr.autolog.vehicle.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.autolog.vehicle.data.repository.ProfileRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.repository.UnassignedTripItem
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

class ProfilePickerViewModel(
    private val session: SessionRepository,
    profile: ProfileRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val unassigned = MutableStateFlow<UnassignedTripItem?>(null)
    private val showAll = MutableStateFlow(false)

    init {
        viewModelScope.launch { unassigned.value = profile.unassignedTrip() }
    }

    val uiState: StateFlow<ProfilePickerUiState> =
        combine(profile.orderedProfiles(), unassigned, showAll) { p, u, s ->
            ProfilePickerUiState(profiles = p, unassigned = u, showAll = s, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilePickerUiState())

    fun onAction(action: ProfilePickerAction) {
        viewModelScope.launch {
            when (action) {
                is ProfilePickerAction.Select -> {
                    session.selectDriver(action.userId)
                    navigator.replaceAll(Route.Home)
                }
                ProfilePickerAction.Later -> {
                    session.selectDriver(null)
                    navigator.replaceAll(Route.Home)
                }
                is ProfilePickerAction.AssignPrevious -> {
                    session.assignDriver(action.tripId, action.userId)
                    unassigned.value = null
                }
                ProfilePickerAction.ShowAll -> showAll.value = true
                ProfilePickerAction.HideAll -> showAll.value = false
            }
        }
    }
}
