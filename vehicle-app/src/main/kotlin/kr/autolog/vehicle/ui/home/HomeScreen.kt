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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.LocalTopEndReserved
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.ChipTone
import kr.autolog.vehicle.ui.design.components.SectionHeader
import kr.autolog.vehicle.ui.design.components.StatusChip
import kr.autolog.vehicle.ui.design.verticalScrollFade

@Composable
fun HomeRoute() {
    val graph = LocalAppGraph.current
    val vm =
        viewModel {
            HomeViewModel(graph.session, graph.context, graph.trips, graph.receipts, graph.navigator, graph.clock)
        }
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, vm::onAction)
}

/**
 * 스펙 7.2 (D-23·D-24). 3단: 맨 위 상태 줄(오른쪽은 운전자 배지) / 가운데 두 섹션 / 맨 아래 같은 모양 타일 4개([HomeDock]).
 * 두 섹션 = 운행 정보([HomeHero] — 대기 "최근 운행 정보", 기록 중 "현재 운행 정보") · 알림([HomeNotices]).
 * 소제목은 같은 [SectionHeader], 내용은 위쪽 정렬로 시작선을 맞춘다.
 */
@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    // 복구 중에는 팝업이 모든 정보를 말한다 — 뒤에 섹션을 그리지 않는다
    val showBody = state.phase != HomePhase.NEEDS_RECOVERY
    val trip = @Composable { HomeSection(tripSectionTitle(state.phase)) { HomeHero(state) } }
    val notices = @Composable {
        HomeSection(stringResource(R.string.section_notices)) { HomeNotices(state.notices, onAction) }
    }
    Box(modifier.fillMaxSize()) {
        if (layout == WindowLayout.WIDE) {
            // 높이 640 dp 안에 상태 줄·두 섹션·타일이 다 들어가게 단 사이 간격은 16 dp
            Column(Modifier.fillMaxSize().padding(Space.s7), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                TopRow { HomeStatus(state) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                    if (showBody) {
                        // 좌우 반반. 섹션마다 넘치면 그 섹션만 스크롤
                        ScrollRegion(Modifier.weight(1f).fillMaxHeight()) { trip() }
                        ScrollRegion(Modifier.weight(1f).fillMaxHeight()) { notices() }
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
                        // 한 칸에 위아래로 쌓는다. 대기 중 알림이 있으면 처리할 일이 요약보다 급하므로 알림이 먼저,
                        // 기록 중이면 지금 운행이 먼저
                        val noticesFirst = state.phase == HomePhase.READY && state.notices.isNotEmpty()
                        if (noticesFirst) notices() else trip()
                        Spacer(Modifier.height(Space.s6))
                        if (noticesFirst) trip() else notices()
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
    }
}

/** 왼쪽 섹션 소제목: 대기 "최근 운행 정보", 기록 중 "현재 운행 정보". */
@Composable
private fun tripSectionTitle(phase: HomePhase): String = stringResource(
    if (phase == HomePhase.READY) R.string.section_recent_trip else R.string.section_current_trip,
)

/** 소제목 + 내용. 모든 섹션이 같은 모양. */
@Composable
private fun HomeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        content()
    }
}

/** 상태 줄: `● 대기 중` / `● 기록 중 · 35분째`. 누를 수 없는 표시. */
@Composable
private fun HomeStatus(state: HomeUiState) {
    val recording = state.phase == HomePhase.RECORDING_PARKED || state.phase == HomePhase.AUTO_ENDING
    StatusChip(
        text =
        if (recording) {
            stringResource(R.string.home_status_recording, state.elapsedMin ?: 0)
        } else {
            stringResource(R.string.home_status_ready)
        },
        tone = if (recording) ChipTone.SUCCESS else ChipTone.MUTED,
        icon = Icons.Rounded.FiberManualRecord,
    )
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
