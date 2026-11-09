package kr.autolog.vehicle.ui.profile

import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.data.repository.UnassignedTripItem

/** 스펙 7.1: 타일 최대 5개, 넘치면 6번째 자리에 [다른 사람]. */
const val PICKER_VISIBLE_MAX = 5

data class ProfilePickerUiState(
    val profiles: List<DriverProfile> = emptyList(),
    val unassigned: UnassignedTripItem? = null,
    val showAll: Boolean = false,
    val loaded: Boolean = false,
) {
    val visible: List<DriverProfile> get() = profiles.take(PICKER_VISIBLE_MAX)
    val hasMore: Boolean get() = profiles.size > PICKER_VISIBLE_MAX
}

sealed interface ProfilePickerAction {
    data class Select(val userId: String) : ProfilePickerAction

    data object Later : ProfilePickerAction

    data class AssignPrevious(val tripId: String, val userId: String) : ProfilePickerAction

    data object ShowAll : ProfilePickerAction

    data object HideAll : ProfilePickerAction
}
