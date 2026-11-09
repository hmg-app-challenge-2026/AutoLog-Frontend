package kr.autolog.vehicle.ui.peek

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.ChipTone
import kr.autolog.vehicle.ui.design.components.HeroMetric
import kr.autolog.vehicle.ui.design.components.StatusChip
import kr.autolog.vehicle.ui.design.durationText

@Composable
fun PeekRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { PeekViewModel(graph.session, graph.profile, graph.context, graph.clock) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    PeekScreen(state)
}

/** 스펙 7.0. 클릭 가능한 요소 0개, 모션 없음. */
@Composable
fun PeekScreen(state: PeekUiState, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Box(modifier.fillMaxSize().padding(Space.s6), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = Size.peekMaxWidth), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
            when (state) {
                PeekUiState.Loading -> Unit
                is PeekUiState.Recording -> {
                    val driver = state.driverName ?: stringResource(R.string.peek_driver_unknown)
                    StatusChip(
                        text = stringResource(R.string.peek_recording) + " · " + driver,
                        tone = ChipTone.SUCCESS,
                        icon = Icons.Rounded.FiberManualRecord,
                    )
                    HeroMetric(
                        value = state.tripKm,
                        unit = stringResource(R.string.unit_km),
                        label = stringResource(R.string.peek_trip_label),
                        unavailableText = stringResource(R.string.distance_unavailable),
                        large = false,
                    )
                    Text(
                        state.destination ?: stringResource(R.string.peek_no_destination),
                        style = t.title,
                        color = c.textPrimary,
                    )
                    Text(
                        stringResource(R.string.peek_source_saved, state.savedSecondsAgo),
                        style = t.body,
                        color = c.textSecondary,
                    )
                    state.autoEndInSec?.let {
                        Text(
                            stringResource(R.string.peek_auto_end, durationText(it)),
                            style = t.body,
                            color = c.warning,
                        )
                    }
                }
                is PeekUiState.NoTrip -> {
                    Text(stringResource(R.string.peek_no_trip), style = t.title, color = c.textPrimary)
                    Text(stringResource(R.string.peek_no_trip_hint), style = t.body, color = c.textSecondary)
                    HeroMetric(
                        value = state.odometerKm,
                        unit = stringResource(R.string.unit_km),
                        label = stringResource(R.string.odometer_label),
                        unavailableText = stringResource(R.string.distance_unavailable),
                        large = false,
                    )
                }
            }
        }
    }
}
