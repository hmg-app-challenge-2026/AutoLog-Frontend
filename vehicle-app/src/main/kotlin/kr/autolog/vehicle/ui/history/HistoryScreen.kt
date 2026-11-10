package kr.autolog.vehicle.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.Avatar
import kr.autolog.vehicle.ui.design.components.PeriodSwitcher
import kr.autolog.vehicle.ui.design.components.ScreenHeader
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.components.TripListRow
import kr.autolog.vehicle.ui.design.components.tappable
import kr.autolog.vehicle.ui.design.screenMargin
import kr.autolog.vehicle.ui.trips.TripRowUi
import kr.autolog.vehicle.ui.trips.tripMetaText
import kr.autolog.vehicle.ui.trips.tripRouteText

@Composable
fun HistoryRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { HistoryViewModel(graph.trips, graph.profile, graph.session, graph.navigator, graph.clock) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, vm::onAction)
}

/**
 * 운행 기록 조회(스펙 7.8, F-12). 위에서 아래로: 머리 줄(X · "운행 기록") → 달 이동 · 합계 · [운행기록부 내보내기]
 * → 운행 목록(최신 순, 넘치면 스크롤 — 자동 스크롤 없음). 넓은 화면에서도 읽기 쉬운 폭(`sheetMaxWidth`)으로 가운데.
 * 줄을 누르면 운행 결과 카드(사후 확인 — 카운트다운 없음, D-29). 운전자 미확정 줄은 그 자리에서 후보 1탭으로 지정(F-20).
 */
@Composable
fun HistoryScreen(state: HistoryUiState, onAction: (HistoryAction) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(screenMargin()),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        ScreenHeader(stringResource(R.string.history_title)) { onAction(HistoryAction.Back) }
        Column(
            Modifier.weight(1f).fillMaxWidth().widthIn(max = Size.sheetMaxWidth).align(Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            Toolbar(state, onAction)
            when {
                state.loading -> Unit
                state.rows.isEmpty() ->
                    Text(
                        stringResource(R.string.history_empty),
                        style = AutoLogTheme.type.body,
                        color = AutoLogTheme.colors.textTertiary,
                    )
                else -> TripList(state, onAction, Modifier.weight(1f))
            }
        }
    }
}

/**
 * 달 이동(◀ 2026년 11월 ▶) · [운행기록부 내보내기] · 그 아래 합계("23회 운행 · 412.7 km").
 * 넓은 창은 달 이동과 버튼이 한 줄(버튼 오른쪽 끝), 좁은·세로 창은 달 이동 / 합계 / 버튼(전폭)을 위아래로(한 줄이면 버튼 글자가 꺾인다).
 */
@Composable
private fun Toolbar(state: HistoryUiState, onAction: (HistoryAction) -> Unit) {
    val report = @Composable { modifier: Modifier ->
        SecondaryButton(stringResource(R.string.history_report), onClick = {
            onAction(HistoryAction.OpenReport)
        }, modifier = modifier)
    }
    if (LocalWindowLayout.current != WindowLayout.WIDE) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            MonthSwitcher(state, onAction)
            Summary(state)
            report(Modifier.fillMaxWidth())
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonthSwitcher(state, onAction)
                Spacer(Modifier.weight(1f))
                report(Modifier)
            }
            Summary(state)
        }
    }
}

@Composable
private fun MonthSwitcher(state: HistoryUiState, onAction: (HistoryAction) -> Unit) = PeriodSwitcher(
    state.month,
    state.canGoNext,
    onPrev = { onAction(HistoryAction.PrevMonth) },
    onNext = { onAction(HistoryAction.NextMonth) },
)

@Composable
private fun Summary(state: HistoryUiState) {
    Text(
        stringResource(R.string.history_summary, state.tripCount, state.totalKm),
        style = AutoLogTheme.type.body,
        color = AutoLogTheme.colors.textSecondary,
    )
}

@Composable
private fun TripList(state: HistoryUiState, onAction: (HistoryAction) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        items(state.rows, key = { it.tripId }) { row ->
            TripListRow(
                title = tripRouteText(row),
                subtitle = tripMetaText(row, withDriver = true, withUpload = true),
                onClick = { onAction(HistoryAction.Open(row.tripId)) },
                extra =
                if (row.driverUnset && state.driverChoices.isNotEmpty()) {
                    { DriverPicker(row, state.driverChoices, onAction) }
                } else {
                    null
                },
            )
        }
    }
}

/** 운전자 미확정 줄 안: "운전자 지정" + 후보(아바타 + 이름) 1탭(F-20, D-19). 좁으면 다음 줄로 넘긴다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DriverPicker(row: TripRowUi, choices: List<DriverChoice>, onAction: (HistoryAction) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    FlowRow(
        Modifier.padding(top = Space.s2),
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.history_assign_driver),
            modifier = Modifier.align(Alignment.CenterVertically),
            style = t.caption,
            color = c.textSecondary,
        )
        choices.forEach { choice ->
            Row(
                Modifier
                    .heightIn(min = Size.touchMin)
                    .tappable({ onAction(HistoryAction.AssignDriver(row.tripId, choice.userId)) })
                    .padding(horizontal = Space.s2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
            ) {
                Avatar(choice.userId, choice.name, Size.avatarChip)
                Text(choice.name, style = t.label, color = c.textPrimary)
            }
        }
    }
}
