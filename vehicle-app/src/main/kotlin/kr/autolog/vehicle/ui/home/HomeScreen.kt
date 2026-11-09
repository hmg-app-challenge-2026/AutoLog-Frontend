package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.DriverSide
import kr.autolog.vehicle.ui.design.LocalDriverSide
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.Avatar
import kr.autolog.vehicle.ui.design.components.Banner
import kr.autolog.vehicle.ui.design.components.BannerAction
import kr.autolog.vehicle.ui.design.components.ChipTone
import kr.autolog.vehicle.ui.design.components.HeroMetric
import kr.autolog.vehicle.ui.design.components.MetricSize
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.StatusChip
import kr.autolog.vehicle.ui.design.sharedAvatar

@Composable
fun HomeRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel {
        HomeViewModel(
            graph.session,
            graph.profile,
            graph.context,
            graph.trips,
            graph.receipts,
            graph.navigator,
            graph.clock,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, vm::onAction)
}

/** 스펙 7.2. WIDE = 동작 컬럼 40%(운전자 쪽) + hero 60%, NARROW·TALL = 세로 스택 + 하단 고정 주 버튼. */
@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    Box(modifier.fillMaxSize()) {
        if (layout == WindowLayout.WIDE) {
            Row(Modifier.fillMaxSize().padding(Space.s7), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                val actions =
                    @Composable {
                        Column(Modifier.weight(0.4f).fillMaxHeight()) {
                            HomeChips(state, onAction)
                            Spacer(Modifier.weight(1f))
                            HomePrimary(state, onAction)
                        }
                    }
                val hero = @Composable {
                    HeroRegion(Modifier.weight(0.6f).fillMaxHeight()) { HomeHero(state, onAction) }
                }
                if (LocalDriverSide.current == DriverSide.START) {
                    actions()
                    hero()
                } else {
                    hero()
                    actions()
                }
            }
        } else {
            val margin = if (layout == WindowLayout.TALL) Space.tallMargin else Space.s6
            Column(Modifier.fillMaxSize().padding(margin), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                HomeChips(state, onAction)
                // 가운데 영역만 남는 높이를 쓴다 — 넘치면 이 영역이 스크롤되고 주 버튼은 절대 눌리지 않는다
                HeroRegion(Modifier.weight(1f).fillMaxWidth()) { HomeHero(state, onAction) }
                HomePrimary(state, onAction)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeChips(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        val d = state.driver
        StatusChip(
            text = d?.name ?: stringResource(R.string.home_choose_driver),
            tone = if (d == null) ChipTone.ACCENT else ChipTone.NEUTRAL,
            leading =
            if (d != null) {
                { Avatar(d.userId, d.name, Size.avatarChip, Modifier.sharedAvatar(d.userId)) }
            } else {
                null
            },
            onClick = { onAction(HomeAction.OpenProfilePicker) },
        )
        if (state.reviewCount > 0) {
            StatusChip(
                stringResource(R.string.home_review_chip, state.reviewCount),
                ChipTone.WARNING,
                icon = Icons.Rounded.ErrorOutline,
            )
        }
    }
}

/** 공간이 남으면 세로 가운데, 모자라면 스크롤. */
@Composable
private fun HeroRegion(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

@Composable
private fun ColumnScope.HomeHero(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val layout = LocalWindowLayout.current
    // 복구 중에는 시트가 모든 정보를 말한다 — 뒤에 거리 경고를 그리지 않는다
    if (state.phase == HomePhase.NEEDS_RECOVERY) return
    if (state.phase == HomePhase.READY) {
        state.missedTripKm?.let {
            Banner(
                stringResource(R.string.home_missed, it),
                listOf(
                    BannerAction(stringResource(R.string.home_missed_add)) { onAction(HomeAction.AcceptMissedTrip) },
                    BannerAction(stringResource(R.string.home_missed_ignore)) { onAction(HomeAction.IgnoreMissedTrip) },
                ),
            )
            Spacer(Modifier.height(Space.s5))
        }
        HeroMetric(
            value = state.odometerKm,
            unit = stringResource(R.string.unit_km),
            label = stringResource(R.string.odometer_label),
            unavailableText = stringResource(R.string.distance_unavailable),
            // 누적거리는 자릿수가 많다(18,420.5) — 좁은 창에선 72 sp
            size = if (layout == WindowLayout.NARROW) MetricSize.COMPACT else MetricSize.MEDIUM,
        )
        // 배너가 있으면 상황 설명은 배너가 한다 — 좁은 창 높이를 아끼려 안내 문장을 생략
        if (state.missedTripKm == null) {
            Spacer(Modifier.height(Space.s3))
            Text(stringResource(R.string.home_ready_hint), style = t.title, color = c.textSecondary)
        }
    } else {
        HeroMetric(
            value = state.tripKm,
            unit = stringResource(R.string.unit_km),
            label = stringResource(R.string.home_trip_label),
            unavailableText = stringResource(R.string.distance_unavailable),
            size = if (layout == WindowLayout.NARROW) MetricSize.MEDIUM else MetricSize.HERO,
        )
        Spacer(Modifier.height(Space.s3))
        state.destination?.let { Text(it, style = t.title, color = c.textPrimary) }
        state.elapsedMin?.let {
            Text(stringResource(R.string.home_elapsed, it), style = t.body, color = c.textSecondary)
        }
    }
}

@Composable
private fun HomePrimary(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    // 좁은 창(분할 화면 한 칸)은 높이가 모자라 96 dp, 그 외 120 dp
    val height = if (LocalWindowLayout.current == WindowLayout.NARROW) Size.buttonLg else Size.buttonXl
    when (state.phase) {
        HomePhase.READY ->
            PrimaryButton(
                text = stringResource(R.string.home_start),
                onClick = { onAction(HomeAction.StartTrip) },
                modifier = Modifier.fillMaxWidth(),
                height = height,
            )
        HomePhase.RECORDING_PARKED, HomePhase.AUTO_ENDING ->
            PrimaryButton(
                text = stringResource(R.string.home_end),
                onClick = { onAction(HomeAction.EndNow) },
                modifier = Modifier.fillMaxWidth(),
                height = height,
            )
        HomePhase.NEEDS_RECOVERY -> Unit
    }
}
