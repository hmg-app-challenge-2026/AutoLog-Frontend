package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

/** WIDE에서 주인공이 차지하는 폭 비율(나머지는 알림). 알림이 없으면 주인공이 다 쓴다. */
private const val WIDE_HERO_WEIGHT = 0.55f

/**
 * 스펙 7.2 (D-23). 3단: 맨 위 상태 줄(오른쪽은 운전자 배지) / 가운데 주인공 하나 + 알림 / 맨 아래 독.
 * 주인공 = 대기면 이번 달 요약, 기록 중이면 이번 운행 거리 + 경로([HomeHero]). 처리할 일은 알림 목록 하나([HomeNotices]).
 * 동작은 독 한 줄([HomeDock]) — 상태가 바뀌어도 자리가 같다.
 */
@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    // 복구 중에는 팝업이 모든 정보를 말한다 — 뒤에 주인공·알림을 그리지 않는다
    val showBody = state.phase != HomePhase.NEEDS_RECOVERY
    Box(modifier.fillMaxSize()) {
        if (layout == WindowLayout.WIDE) {
            // 높이 640 dp 안에 상태 줄·알림 3줄·독이 다 들어가게 단 사이 간격은 16 dp
            Column(Modifier.fillMaxSize().padding(Space.s7), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                TopRow { HomeStatus(state) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                    if (showBody) {
                        val hasNotices = state.notices.isNotEmpty()
                        ScrollRegion(Modifier.weight(if (hasNotices) WIDE_HERO_WEIGHT else 1f).fillMaxHeight()) {
                            HomeHero(state)
                        }
                        if (hasNotices) {
                            ScrollRegion(Modifier.weight(1f - WIDE_HERO_WEIGHT).fillMaxHeight()) {
                                HomeNotices(state.notices, onAction)
                            }
                        }
                    }
                }
                HomeDock(state, onAction, wide = true)
            }
        } else {
            val margin = if (layout == WindowLayout.TALL) Space.tallMargin else Space.s6
            Column(Modifier.fillMaxSize().padding(margin), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                TopRow { HomeStatus(state) }
                // 가운데만 남는 높이를 쓴다 — 넘치면 이 영역이 스크롤되고 독·주 버튼은 늘 보인다
                ScrollRegion(Modifier.weight(1f).fillMaxWidth()) {
                    if (showBody) {
                        // 좁은 창은 한 칸에 하나씩 쌓인다. 대기 중이면 처리할 일(알림)이 요약보다 급하므로 먼저,
                        // 기록 중이면 지금 운행이 먼저 — 아래 것은 스크롤 + 가장자리 흐림으로 이어진다
                        val noticesFirst = state.phase == HomePhase.READY && state.notices.isNotEmpty()
                        if (noticesFirst) {
                            HomeNotices(state.notices, onAction)
                            Spacer(Modifier.height(Space.s6))
                            HomeHero(state)
                        } else {
                            HomeHero(state)
                            if (state.notices.isNotEmpty()) {
                                Spacer(Modifier.height(Space.s6))
                                HomeNotices(state.notices, onAction)
                            }
                        }
                    }
                }
                HomeDock(state, onAction, wide = false)
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

/** 공간이 남으면 세로 가운데, 모자라면 스크롤 + 더 있는 쪽 가장자리를 흐린다. */
@Composable
private fun ScrollRegion(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier.verticalScrollFade(scroll, Space.s7)) {
        Column(
            Modifier.verticalScroll(scroll).heightIn(min = maxHeight).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}
