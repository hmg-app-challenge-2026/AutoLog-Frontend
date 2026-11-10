package kr.autolog.vehicle.ui.peek

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import kr.autolog.vehicle.ui.design.components.FitOrDrop
import kr.autolog.vehicle.ui.design.components.HeroMetric
import kr.autolog.vehicle.ui.design.components.MetricSize
import kr.autolog.vehicle.ui.design.components.RouteFlow
import kr.autolog.vehicle.ui.design.components.Section
import kr.autolog.vehicle.ui.design.components.StatusLine
import kr.autolog.vehicle.ui.design.components.TopLine
import kr.autolog.vehicle.ui.design.durationText
import kr.autolog.vehicle.ui.design.screenMargin

@Composable
fun PeekRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { PeekViewModel(graph.session, graph.context, graph.clock) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    PeekScreen(state)
}

/**
 * 스펙 7.0. 클릭 가능한 요소 0개. 모션은 경로 화살표 흐름(반복 상태 표시)만.
 * 기록 중: 맨 위 줄(`TopLine`)에 Home과 같은 상태 줄 — 오른쪽 위 운전자 배지와 같은 높이 — 그 아래 가운데에 섹션들.
 * 스크롤이 없으므로 다 들어가지 않으면 맨 아래 상황 줄(⑤)을 통째로 뺀다(`FitOrDrop`) — 반쯤 잘린 글자를 그리지 않는다.
 */
@Composable
fun PeekScreen(state: PeekUiState, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Column(modifier.fillMaxSize().padding(screenMargin())) {
        if (state is PeekUiState.Recording) {
            // Home과 같은 상태 줄(점 + 글자). 운전자는 오른쪽 위 배지에만
            TopLine { StatusLine(text = stringResource(R.string.status_recording), dot = c.success) }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = Size.peekMaxWidth), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                when (state) {
                    PeekUiState.Loading -> Unit
                    is PeekUiState.Recording -> FitOrDrop(
                        gap = Space.s5,
                        main = {
                            Column(verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                                // Home과 같은 섹션·소제목("주행 거리", "운행 경로") — 같은 정보는 어느 화면에서나 같은 이름
                                Section(stringResource(R.string.section_distance)) {
                                    HeroMetric(
                                        value = state.tripKm,
                                        unit = stringResource(R.string.unit_km),
                                        label = null,
                                        unavailableText = stringResource(R.string.distance_unavailable),
                                        size = MetricSize.MEDIUM,
                                    )
                                }
                                Section(stringResource(R.string.section_route)) {
                                    RouteFlow(
                                        origin = state.origin,
                                        destination = state.destination ?: stringResource(R.string.peek_no_destination),
                                        destinationColor = if (state.destination !=
                                            null
                                        ) {
                                            c.textPrimary
                                        } else {
                                            c.textSecondary
                                        },
                                    )
                                }
                            }
                        },
                        tail = {
                            state.autoEndInSec?.let {
                                Text(
                                    stringResource(R.string.peek_auto_end, durationText(it)),
                                    style = t.body,
                                    color = c.warning,
                                )
                            }
                        },
                    )
                    is PeekUiState.NoTrip -> {
                        Text(stringResource(R.string.peek_no_trip), style = t.title, color = c.textPrimary)
                        Text(stringResource(R.string.peek_no_trip_hint), style = t.body, color = c.textSecondary)
                        Section(stringResource(R.string.section_odometer)) {
                            HeroMetric(
                                value = state.odometerKm,
                                unit = stringResource(R.string.unit_km),
                                label = null,
                                unavailableText = stringResource(R.string.distance_unavailable),
                                size = MetricSize.COMPACT,
                            )
                        }
                    }
                }
            }
        }
    }
}
