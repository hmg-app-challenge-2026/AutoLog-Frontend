package kr.autolog.vehicle.ui.result

import kr.autolog.vehicle.data.repository.TripResult
import kr.autolog.vehicle.ui.design.formatKm

/** 운행 결과 상태 계산(순수 함수, TripResultReducerTest 대상). [result] null + [loaded] = 운행을 찾지 못함. */
internal fun reduceTripResult(
    result: TripResult?,
    loaded: Boolean,
    interaction: TripResultInteraction,
): TripResultUiState {
    if (!loaded) return TripResultUiState(loading = true)
    if (result == null) return TripResultUiState(loading = false, notFound = true)
    return TripResultUiState(
        loading = false,
        km = formatKm(result.distanceKm),
        minutes = result.endTime?.let { ((it - result.startTime) / 60_000).toInt().coerceAtLeast(0) },
        origin = result.origin,
        destination = result.destination,
        selected = interaction.selected ?: result.category,
        editing = interaction.editing,
        autoCloseSec = interaction.autoCloseSec,
        saving = interaction.saving,
    )
}
