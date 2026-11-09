package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
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
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.StatusChip
import kr.autolog.vehicle.ui.design.sharedAvatar

@Composable
fun HomeRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { HomeViewModel(graph.session, graph.profile, graph.context, graph.navigator, graph.clock) }
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
                val hero =
                    @Composable {
                        Column(Modifier.weight(0.6f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            HomeHero(state, onAction)
                        }
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
            Column(Modifier.fillMaxSize().padding(margin)) {
                HomeChips(state, onAction)
                Spacer(Modifier.weight(if (layout == WindowLayout.TALL) 0.5f else 1f))
                HomeHero(state, onAction)
                Spacer(Modifier.weight(1f))
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.s4), verticalArrangement = Arrangement.spacedBy(Space.s3)) {
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
            StatusChip(stringResource(R.string.home_review_chip, state.reviewCount), ChipTone.WARNING, icon = Icons.Rounded.ErrorOutline)
        }
    }
}

@Composable
private fun ColumnScope.HomeHero(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
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
            Spacer(Modifier.height(Space.s6))
        }
        HeroMetric(
            value = state.odometerKm,
            unit = stringResource(R.string.unit_km),
            label = stringResource(R.string.odometer_label),
            unavailableText = stringResource(R.string.distance_unavailable),
            large = false,
        )
        Spacer(Modifier.height(Space.s3))
        Text(stringResource(R.string.home_ready_hint), style = t.title, color = c.textSecondary)
    } else {
        HeroMetric(
            value = state.tripKm,
            unit = stringResource(R.string.unit_km),
            label = stringResource(R.string.home_trip_label),
            unavailableText = stringResource(R.string.distance_unavailable),
        )
        Spacer(Modifier.height(Space.s3))
        state.destination?.let { Text(it, style = t.title, color = c.textPrimary) }
        state.elapsedMin?.let { Text(stringResource(R.string.home_elapsed, it), style = t.body, color = c.textSecondary) }
    }
}

@Composable
private fun HomePrimary(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    when (state.phase) {
        HomePhase.READY ->
            PrimaryButton(stringResource(R.string.home_start), { onAction(HomeAction.StartTrip) }, Modifier.fillMaxWidth(), height = Size.buttonXl)
        HomePhase.RECORDING_PARKED, HomePhase.AUTO_ENDING ->
            PrimaryButton(stringResource(R.string.home_end), { onAction(HomeAction.EndNow) }, Modifier.fillMaxWidth(), height = Size.buttonXl)
        HomePhase.NEEDS_RECOVERY -> Unit
    }
}
