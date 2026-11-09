package kr.autolog.vehicle.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.DriverSide
import kr.autolog.vehicle.ui.design.LocalDriverSide
import kr.autolog.vehicle.ui.design.LocalTopEndReserved
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.ActionCard
import kr.autolog.vehicle.ui.design.components.Banner
import kr.autolog.vehicle.ui.design.components.BannerAction
import kr.autolog.vehicle.ui.design.components.ChipTone
import kr.autolog.vehicle.ui.design.components.HeroMetric
import kr.autolog.vehicle.ui.design.components.MetricSize
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.QuickAction
import kr.autolog.vehicle.ui.design.components.StatusChip
import kr.autolog.vehicle.ui.design.verticalScrollFade

@Composable
fun HomeRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel {
        HomeViewModel(
            graph.session,
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

/**
 * 스펙 7.2. 위쪽 줄 오른쪽은 운전자 배지 자리(루트가 그린다 — 디자인 언어 11장 DriverBadge).
 * WIDE = 동작 컬럼 40%(운전자 쪽) + hero 60%, 상태 칩은 위쪽 줄 왼쪽.
 * NARROW·TALL = 세로 스택(상태 칩은 가운데 영역 위) + 보조 동작 한 줄 + 하단 고정 주 버튼.
 */
/** WIDE에서 동작 열이 차지하는 폭 비율. 동작 타일 이름(최장 "영수증 저장")이 한 줄에 들어가고 hero 숫자가 HERO 크기를 지키는 값. */
private const val WIDE_ACTIONS_WEIGHT = 0.45f

/** WIDE 보조 동작 격자의 열 수. */
private const val WIDE_ACTION_COLUMNS = 2

@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    Box(modifier.fillMaxSize()) {
        if (layout == WindowLayout.WIDE) {
            Column(Modifier.fillMaxSize().padding(Space.s7), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                TopRow { StatusChips(state, onAction, compact = false) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                    val actions =
                        @Composable {
                            Column(
                                Modifier.weight(WIDE_ACTIONS_WEIGHT).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(Space.s4),
                            ) {
                                QuickActions(state, onAction, wide = true)
                                Spacer(Modifier.weight(1f))
                                HomePrimary(state, onAction)
                            }
                        }
                    val hero = @Composable {
                        HeroRegion(Modifier.weight(1f - WIDE_ACTIONS_WEIGHT).fillMaxHeight()) {
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
            }
        } else {
            val margin = if (layout == WindowLayout.TALL) Space.tallMargin else Space.s6
            Column(Modifier.fillMaxSize().padding(margin), verticalArrangement = Arrangement.spacedBy(Space.s5)) {
                val narrow = layout == WindowLayout.NARROW
                // NARROW는 세로가 모자라다 — 칩을 축약형(아이콘+숫자)으로 배지 줄에 올려 가운데 영역을 넓힌다.
                // TALL은 세로가 넉넉하다 — 배지 줄은 비우고 칩은 전체 문구로 본문 위에 둔다.
                TopRow { if (narrow) StatusChips(state, onAction, compact = true) }
                // 가운데 영역만 남는 높이를 쓴다 — 넘치면 이 영역이 스크롤되고 주 버튼은 절대 눌리지 않는다
                HeroRegion(Modifier.weight(1f).fillMaxWidth()) {
                    if (!narrow && (state.reviewCount > 0 || state.receiptCount > 0)) {
                        StatusChips(state, onAction, compact = false)
                        Spacer(Modifier.height(Space.s5))
                    }
                    HomeHero(state, onAction)
                }
                QuickActions(state, onAction, wide = false)
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

/** 화면 맨 위 줄(높이 76 dp). 오른쪽은 운전자 배지 자리로 비운다([LocalTopEndReserved]). */
@Composable
private fun TopRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Size.touchMin).padding(end = LocalTopEndReserved.current),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 확인 필요·영수증 도착 칩 — 누르면 해당 화면(1탭). [compact]면 아이콘+숫자만 보이고 전체 문구는 접근성 설명으로 남긴다(NARROW 배지 줄용).
 * 색·아이콘은 두 형태가 같아서 창 크기를 바꿔도 같은 칩으로 읽힌다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChips(state: HomeUiState, onAction: (HomeAction) -> Unit, compact: Boolean) {
    val chips =
        listOfNotNull(
            if (state.reviewCount > 0) {
                HomeChip(
                    stringResource(R.string.home_review_chip, state.reviewCount),
                    state.reviewCount,
                    ChipTone.WARNING,
                    Icons.Rounded.ErrorOutline,
                    HomeAction.OpenReview,
                )
            } else {
                null
            },
            if (state.receiptCount > 0) {
                HomeChip(
                    stringResource(R.string.home_receipt_chip, state.receiptCount),
                    state.receiptCount,
                    ChipTone.ACCENT,
                    Icons.AutoMirrored.Rounded.ReceiptLong,
                    HomeAction.OpenReceipt,
                )
            } else {
                null
            },
        )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(if (compact) Space.s3 else Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        chips.forEach { chip ->
            StatusChip(
                if (compact) chip.count.toString() else chip.label,
                chip.tone,
                modifier = if (compact) Modifier.semantics { contentDescription = chip.label } else Modifier,
                icon = chip.icon,
                onClick = { onAction(chip.action) },
            )
        }
    }
}

private data class HomeChip(
    val label: String,
    val count: Int,
    val tone: ChipTone,
    val icon: ImageVector,
    val action: HomeAction,
)

/** 보조 동작 한 칸. */
private data class QuickActionItem(val icon: ImageVector, @StringRes val label: Int, val action: HomeAction)

/**
 * 보조 동작. 대기 중 4개, 운행 중(주차) 3개 — 운행기록부는 운행이 끝난 뒤의 일이라 뺀다.
 * "주유 기록 추가"는 영수증 QR(7.4)로 들어가고, 직접 입력은 그 화면에서 같은 입력 창(7.5)을 빈 칸으로 여는 보조 경로다(D-20).
 * 같은 뜻의 버튼을 둘 두지 않는다.
 */
@Composable
private fun QuickActions(state: HomeUiState, onAction: (HomeAction) -> Unit, wide: Boolean) {
    if (state.phase == HomePhase.NEEDS_RECOVERY) return
    val recording = state.phase != HomePhase.READY
    val items =
        listOfNotNull(
            QuickActionItem(Icons.Rounded.LocalGasStation, R.string.action_fuel_add, HomeAction.AddFuelRecord),
            if (!recording) {
                QuickActionItem(
                    Icons.Rounded.Description,
                    R.string.action_report,
                    HomeAction.OpenReport,
                )
            } else {
                null
            },
            QuickActionItem(Icons.Rounded.History, R.string.action_history, HomeAction.OpenHistory),
            QuickActionItem(Icons.Rounded.Settings, R.string.action_settings, HomeAction.OpenSettings),
        )
    if (wide) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            items.chunked(WIDE_ACTION_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                    row.forEach { (icon, label, action) ->
                        ActionCard(icon, stringResource(label), onClick = {
                            onAction(action)
                        }, modifier = Modifier.weight(1f))
                    }
                    // 마지막 줄이 덜 차도 카드 폭은 격자와 같게 — 빈 칸으로 채운다
                    repeat(WIDE_ACTION_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            items.forEach { (icon, label, action) ->
                QuickAction(icon, stringResource(label), onClick = { onAction(action) })
            }
        }
    }
}

/** 공간이 남으면 세로 가운데, 모자라면 스크롤 + 더 있는 쪽 가장자리를 흐린다. */
@Composable
private fun HeroRegion(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    BoxWithConstraints(modifier.verticalScrollFade(scroll, Space.s7)) {
        Column(
            Modifier.verticalScroll(scroll).heightIn(min = maxHeight),
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
