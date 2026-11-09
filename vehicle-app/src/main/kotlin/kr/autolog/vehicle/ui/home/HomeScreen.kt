package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kr.autolog.vehicle.ui.design.LocalTopEndReserved
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.StatusLine
import kr.autolog.vehicle.ui.design.verticalScrollFade

@Composable
fun HomeRoute() {
    val graph = LocalAppGraph.current
    val vm =
        viewModel {
            HomeViewModel(
                graph.session,
                graph.context,
                graph.trips,
                graph.navigator,
                graph.clock,
            )
        }
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, vm::onAction)
}

/**
 * 스펙 7.2 (D-23·D-24·D-25·D-26). 위에서 아래로:
 * 상태 줄(`● 기록 중 · 35분 경과`, 오른쪽은 운전자 배지) → 가운데 두 섹션(값 · 맥락) → 같은 모양 타일 4개.
 * 앱 안 알림은 없다(D-26) — 알려야 할 일은 시스템 알림(앱 A)이 맡고, 고칠 일은 사용자가 운행 기록 조회에서 고친다.
 * 예외는 결정이 꼭 필요한 팝업 둘뿐: 복구([RecoveryDialog])와 놓친 운행([MissedTripDialog]).
 */
@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    // 복구 중에는 팝업이 모든 정보를 말한다 — 뒤에 섹션을 그리지 않는다
    val showBody = state.phase != HomePhase.NEEDS_RECOVERY
    Box(modifier.fillMaxSize()) {
        if (layout == WindowLayout.WIDE) {
            // 높이 640 dp 안에 상태 줄·두 섹션·타일이 다 들어가게 단 사이 간격은 16 dp
            Column(Modifier.fillMaxSize().padding(Space.s7), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                TopRow { HomeStatus(state) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                    if (showBody) {
                        // 좌우 반반. 섹션마다 넘치면 그 섹션만 스크롤
                        ScrollRegion(Modifier.weight(1f).fillMaxHeight()) { HomePrimarySection(state) }
                        ScrollRegion(Modifier.weight(1f).fillMaxHeight()) { HomeSecondarySection(state, onAction) }
                    }
                }
                HomeDock(state, onAction)
            }
        } else {
            val margin = if (layout == WindowLayout.TALL) Space.tallMargin else Space.s6
            Column(Modifier.fillMaxSize().padding(margin), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                TopRow { HomeStatus(state) }
                // 가운데만 남는 높이를 쓴다 — 넘치면 이 영역이 스크롤되고 타일은 늘 보인다
                ScrollRegion(Modifier.weight(1f).fillMaxWidth()) {
                    if (showBody) {
                        // 한 칸에 위아래로: 값 → 맥락
                        HomePrimarySection(state)
                        Spacer(Modifier.height(Space.s6))
                        HomeSecondarySection(state, onAction)
                    }
                }
                HomeDock(state, onAction)
            }
        }
        val autoEnd = state.autoEnd
        if (state.phase == HomePhase.AUTO_ENDING && autoEnd != null) {
            AutoEndPrompt(
                autoEnd,
                state.tripKm,
                state.destination,
                onKeep = { onAction(HomeAction.KeepRecording) },
                onEnd = { onAction(HomeAction.EndNow) },
            )
        }
        if (state.phase == HomePhase.NEEDS_RECOVERY) {
            RecoveryDialog(state.recoveryExtraKm) { onAction(HomeAction.Recover(it)) }
        }
        val missedKm = state.missedKm
        if (state.phase == HomePhase.READY && missedKm != null) {
            MissedTripDialog(
                missedKm,
                onAdd = { onAction(HomeAction.AcceptMissedTrip) },
                onIgnore = { onAction(HomeAction.IgnoreMissedTrip) },
            )
        }
    }
}

/** 상태 줄: `● 대기 중` / `● 기록 중 · 35분 경과`. 점 색이 상태를 말한다(기록 중 초록, 대기 흐린 회색). */
@Composable
private fun HomeStatus(state: HomeUiState) {
    val c = AutoLogTheme.colors
    val recording = state.phase == HomePhase.RECORDING_PARKED || state.phase == HomePhase.AUTO_ENDING
    if (recording) {
        StatusLine(
            text = stringResource(R.string.status_recording),
            dot = c.success,
            detail = stringResource(R.string.home_status_elapsed, state.elapsedMin ?: 0),
        )
    } else {
        StatusLine(text = stringResource(R.string.status_ready), dot = c.textTertiary)
    }
}

/** 화면 맨 위 줄(높이 76 dp). 오른쪽은 운전자 배지 자리로 비운다([LocalTopEndReserved]). */
@Composable
private fun TopRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Size.touchMin).padding(end = LocalTopEndReserved.current),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** 위쪽 정렬로 쌓고, 넘치면 스크롤 + 더 있는 쪽 가장자리를 흐린다. */
@Composable
private fun ScrollRegion(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Column(modifier.verticalScrollFade(scroll, Space.s7).verticalScroll(scroll), content = content)
}
