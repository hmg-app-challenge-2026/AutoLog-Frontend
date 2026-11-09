package kr.autolog.vehicle.ui.result

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.CountdownBar
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.RadioOption
import kr.autolog.vehicle.ui.design.components.RouteFlow
import kr.autolog.vehicle.ui.design.components.ScrollColumn
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.components.Section
import kr.autolog.vehicle.ui.design.components.TopLine
import kr.autolog.vehicle.ui.design.durationText
import kr.autolog.vehicle.ui.design.screenMargin

@Composable
fun TripResultRoute(tripId: String) {
    val graph = LocalAppGraph.current
    val vm = viewModel { TripResultViewModel(tripId, graph.tripResults, graph.navigator) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    TripResultScreen(state, vm::onAction)
}

/**
 * 스펙 7.3 운행 결과(D-27) — 게임 결과 화면처럼 운행이 끝나면 한 번 보여 주고 5초 뒤 메인 화면으로.
 * 맨 위 줄 가운데 제목 "운행 기록을 완료했어요"(운전자 배지 없음) → 주행 거리 · 운행 시간 · 경로선 → 이번 운행의 목적(라디오 3개)
 * → 분류 한 줄 → 남은 시간 + [수정하기]/[저장] · [메인 화면으로](하단 고정).
 * WIDE는 좌 결과 / 우 목적·버튼, NARROW·TALL은 위아래 한 칸(버튼은 늘 보이게 하단 고정).
 */
@Composable
fun TripResultScreen(state: TripResultUiState, onAction: (TripResultAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    Column(
        modifier.fillMaxSize().padding(screenMargin()),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        // 제목은 맨 위 줄 가운데(이 화면엔 운전자 배지가 없다 — FullApp.showsDriverBadge). 그 아래 좌우 칸이 같은 높이의 소제목으로 시작
        TopLine { Title(Modifier.weight(1f)) }
        when {
            state.loading -> Unit
            state.notFound -> {
                Text(stringResource(R.string.result_not_found), style = AutoLogTheme.type.body, color = c.textSecondary)
                Spacer(Modifier.weight(1f))
                PrimaryButton(
                    stringResource(R.string.result_home),
                    onClick = { onAction(TripResultAction.GoHome) },
                    modifier = Modifier.fillMaxWidth(),
                    height = Size.buttonMd,
                )
            }
            LocalWindowLayout.current == WindowLayout.WIDE -> {
                // 위: 좌 결과 | 우 목적(소제목 높이가 같다). 아래: 분류 한 줄 · 남은 시간 · 버튼을 화면 가운데에 모은다
                // 왼쪽 여백만큼 두 칸을 안쪽으로 들여 가운데 쪽에 모은다(왼쪽 쏠림 방지). 좌우 반반
                Row(
                    Modifier.weight(1f).padding(start = Space.s8),
                    horizontalArrangement = Arrangement.spacedBy(Space.s7),
                ) {
                    ScrollColumn(Modifier.weight(1f).fillMaxHeight()) { Summary(state, fixedMetricGap = true) }
                    ScrollColumn(Modifier.weight(1f).fillMaxHeight()) {
                        Purpose(state, onAction, withClassification = false)
                    }
                }
                // 640 dp 높이에 위 두 칸(라디오 3개)이 스크롤 없이 들어가게 아래 묶음 간격은 촘촘히(8 dp)
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s2),
                ) {
                    Classification(state)
                    Footer(state, onAction, Modifier.widthIn(max = Size.dialogMaxWidth), stackedCountdown = true)
                }
            }
            LocalWindowLayout.current == WindowLayout.NARROW -> {
                // 분할 화면 한 칸(504×640): 목적이 첫 화면에 보이게 거리·시간을 한 줄로 줄이고 경로는 목적 아래로
                ScrollColumn(Modifier.weight(1f).fillMaxWidth()) {
                    CompactMetrics(state)
                    Spacer(Modifier.height(Space.s5))
                    Purpose(state, onAction)
                    Spacer(Modifier.height(Space.s5))
                    Route(state)
                }
                Footer(state, onAction, compact = true)
            }
            else -> {
                ScrollColumn(Modifier.weight(1f).fillMaxWidth()) {
                    Summary(state)
                    Spacer(Modifier.height(Space.s6))
                    Purpose(state, onAction)
                }
                Footer(state, onAction)
            }
        }
    }
}

/** 주행 거리 | 운행 시간 · 경로선(끝난 운행이라 흐름 모션 없음). WIDE에선 오른쪽 "이번 운행의 목적"과 소제목 높이가 같다. */
@Composable
private fun Summary(state: TripResultUiState, fixedMetricGap: Boolean = false) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    // WIDE: 두 값 사이를 칸 폭과 상관없이 일정하게(글자 폭 + 64 dp). 그 밖: 반반
    val metric = if (fixedMetricGap) Modifier.width(IntrinsicSize.Max) else Modifier
    Row(horizontalArrangement = Arrangement.spacedBy(if (fixedMetricGap) Space.s8 else Space.s5)) {
        Section(stringResource(R.string.section_distance), if (fixedMetricGap) metric else Modifier.weight(1f)) {
            Text(
                state.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown),
                style = t.title,
                color = if (state.km != null) c.textPrimary else c.textSecondary,
            )
        }
        Section(stringResource(R.string.section_trip_time), if (fixedMetricGap) metric else Modifier.weight(1f)) {
            Text(state.minutes?.let { minutesText(it) } ?: "—", style = t.title, color = c.textPrimary)
        }
    }
    Spacer(Modifier.height(Space.s5))
    Route(state)
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.result_title),
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = AutoLogTheme.type.title,
        color = AutoLogTheme.colors.textPrimary,
    )
}

/** 좁은 창: `48.2 km · 52분` 한 줄. */
@Composable
private fun CompactMetrics(state: TripResultUiState) {
    val km = state.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown)
    val time = state.minutes?.let { minutesText(it) }
    Text(
        if (time != null) stringResource(R.string.trip_meta_pair, km, time) else km,
        style = AutoLogTheme.type.label,
        color = AutoLogTheme.colors.textPrimary,
    )
}

/** 경로선 — 끝난 운행이라 흐름 모션 없음. 도착지를 모르면 "위치 미상". */
@Composable
private fun Route(state: TripResultUiState) {
    val c = AutoLogTheme.colors
    RouteFlow(
        origin = state.origin,
        destination = state.destination ?: stringResource(R.string.place_unknown),
        destinationColor = if (state.destination != null) c.textPrimary else c.textSecondary,
        live = false,
    )
}

/** 이번 운행의 목적: 라디오 3개(보기 모드에선 누를 수 없음) + "일반 업무용 거리 48.2 km로 분류돼요". */
@Composable
private fun Purpose(
    state: TripResultUiState,
    onAction: (TripResultAction) -> Unit,
    withClassification: Boolean = true,
) {
    Section(stringResource(R.string.section_purpose)) {
        PURPOSE_CHOICES.forEach { category ->
            RadioOption(
                stringResource(choiceLabel(category)),
                selected = category == state.selected,
                onClick = if (state.editing) ({ onAction(TripResultAction.Select(category)) }) else null,
            )
        }
        if (withClassification) {
            Spacer(Modifier.height(Space.s3))
            Classification(state)
        }
    }
}

/** "✓ 일반 업무용 거리 48.2 km로 분류돼요" — 지금 고른 목적이 운행기록부에 어떻게 들어가는지. */
@Composable
private fun Classification(state: TripResultUiState) {
    val c = AutoLogTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = c.success,
            modifier = Modifier.size(Size.iconSm),
        )
        val kind = stringResource(classLabel(state.selected))
        Text(
            state.km?.let { stringResource(R.string.result_classified, kind, stringResource(R.string.trip_km, it)) }
                ?: stringResource(R.string.result_classified_no_km, kind),
            style = AutoLogTheme.type.caption,
            color = c.textSecondary,
        )
    }
}

/**
 * 하단 고정: 남은 시간 한 줄(글자 + 줄어드는 막대) + 버튼 두 개.
 * 보기: [수정하기](보조) · [메인 화면으로](주). 수정: [저장](주) · [메인 화면으로](보조) — 자리는 그대로, 강조만 옮긴다.
 */
@Composable
private fun Footer(
    state: TripResultUiState,
    onAction: (TripResultAction) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    stackedCountdown: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (stackedCountdown) Space.s2 else Space.s3)) {
        state.autoCloseSec?.let { sec ->
            val text: @Composable () -> Unit = {
                Text(
                    stringResource(R.string.result_auto_close, durationText(sec)),
                    style = AutoLogTheme.type.caption,
                    color = AutoLogTheme.colors.textSecondary,
                )
            }
            if (stackedCountdown) {
                // WIDE: 글자(가운데) 아래에 막대 — 가운데 모은 아래 묶음과 같은 축
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s2),
                ) {
                    text()
                    CountdownBar(state.autoCloseProgress)
                }
            } else {
                // 좁은 창: 높이를 아끼려 한 줄에 글자 + 막대
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s4),
                ) {
                    text()
                    CountdownBar(state.autoCloseProgress, Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
            // 좁은 창은 왼쪽 버튼을 글자 폭에 맞추고 [메인 화면으로]가 나머지 — 반반이면 "메인 화면으로"가 두 줄로 꺾인다
            val left = if (compact) Modifier else Modifier.weight(1f)
            val home = stringResource(R.string.result_home)
            if (state.editing) {
                PrimaryButton(
                    stringResource(R.string.result_save),
                    onClick = { onAction(TripResultAction.Save) },
                    modifier = left,
                    height = Size.buttonMd,
                    loading = state.saving,
                )
                SecondaryButton(home, onClick = { onAction(TripResultAction.GoHome) }, modifier = Modifier.weight(1f))
            } else {
                SecondaryButton(
                    stringResource(R.string.result_edit),
                    onClick = { onAction(TripResultAction.Edit) },
                    modifier = left,
                )
                PrimaryButton(
                    home,
                    onClick = { onAction(TripResultAction.GoHome) },
                    modifier = Modifier.weight(1f),
                    height = Size.buttonMd,
                    loading = state.saving,
                )
            }
        }
    }
}

@Composable
private fun minutesText(minutes: Int): String = if (minutes < MINUTES_PER_HOUR) {
    stringResource(R.string.trip_minutes, minutes)
} else {
    stringResource(R.string.trip_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

private const val MINUTES_PER_HOUR = 60

/** 라디오 이름(일반 업무 · 출퇴근 · 개인 용무). */
@StringRes
private fun choiceLabel(category: PurposeCategory): Int = when (category) {
    PurposeCategory.COMMUTE -> R.string.purpose_commute
    PurposeCategory.NON_BUSINESS -> R.string.purpose_personal
    PurposeCategory.BUSINESS, PurposeCategory.UNDETERMINED -> R.string.purpose_business
}

/** 분류 이름(운행기록부 기준: 일반 업무용 · 출퇴근용 · 비업무용). */
@StringRes
private fun classLabel(category: PurposeCategory): Int = when (category) {
    PurposeCategory.COMMUTE -> R.string.purpose_class_commute
    PurposeCategory.NON_BUSINESS -> R.string.purpose_class_personal
    PurposeCategory.BUSINESS, PurposeCategory.UNDETERMINED -> R.string.purpose_class_business
}
