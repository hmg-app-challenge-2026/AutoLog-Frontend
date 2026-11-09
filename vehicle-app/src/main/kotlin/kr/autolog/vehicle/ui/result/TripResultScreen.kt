package kr.autolog.vehicle.ui.result

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import kr.autolog.vehicle.ui.design.components.ChoiceRow
import kr.autolog.vehicle.ui.design.components.PrimaryButton
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
 * 스펙 7.3 운행 결과(D-27). **정렬축은 하나**: 화면 여백 안의 콘텐츠 폭을 좌우 반반으로 나누고, 아래 행동 줄도 같은 두 칸을 쓴다.
 * - 맨 위 줄 가운데: 제목 "운행 기록을 완료했어요"(운전자 배지 없음).
 * - 왼쪽 칸 = 운행 요약: "운행 거리 · 시간"(큰 거리 옆에 작은 시간) → "운행 경로"(○—● 경로선).
 * - 오른쪽 칸 = 운행 목적: 같은 폭 선택형 행 3개(8 dp 간격) → 바로 아래 "✓ 일반 업무용 거리 48.2 km로 분류돼요"(선택의 결과).
 * - 아래 행동 줄: 왼쪽 칸에 "3초 후 메인 화면으로 돌아가요", 오른쪽 칸(=목적 칸과 같은 폭)에 [수정하기]/[저장] · [메인 화면으로].
 * NARROW·TALL은 한 칸(목적이 먼저 보이게) + 하단 고정 행동.
 */
@Composable
fun TripResultScreen(state: TripResultUiState, onAction: (TripResultAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val layout = LocalWindowLayout.current
    Column(
        modifier.fillMaxSize().padding(screenMargin()),
        verticalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        TopLine {
            Text(
                stringResource(R.string.result_title),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = AutoLogTheme.type.title,
                color = c.textPrimary,
            )
        }
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
            layout == WindowLayout.WIDE -> {
                TwoColumns(Modifier.weight(1f)) {
                    ScrollColumn(Modifier.weight(1f).fillMaxHeight()) { TripSummary(state) }
                    ScrollColumn(Modifier.weight(1f).fillMaxHeight()) { Purpose(state, onAction) }
                }
                Spacer(Modifier.height(Space.s2)) // 섹션 묶음과 행동 줄 사이를 그룹 안 간격보다 넓게
                TwoColumns(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Countdown(state) }
                    Actions(state, onAction, Modifier.weight(1f))
                }
            }
            else -> {
                ScrollColumn(Modifier.weight(1f).fillMaxWidth()) {
                    // 한 칸: 목적이 첫 화면에 보이게 요약은 한 줄(48.2 km · 52분), 경로는 목적 아래로
                    CompactMetrics(state)
                    Spacer(Modifier.height(Space.s6))
                    Purpose(state, onAction)
                    Spacer(Modifier.height(Space.s6))
                    Section(stringResource(R.string.section_route)) { Route(state) }
                }
                Countdown(state)
                Actions(state, onAction, Modifier.fillMaxWidth())
            }
        }
    }
}

/** 콘텐츠 폭을 좌우 반반으로 — 위 섹션과 아래 행동 줄이 같은 두 칸을 쓴다(공통 그리드). */
@Composable
private fun TwoColumns(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s7),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}

/** 왼쪽 칸: "운행 거리 · 시간"(큰 거리 + 작은 시간, 같은 기준선) → "운행 경로". 섹션 사이 32 dp. */
@Composable
private fun TripSummary(state: TripResultUiState) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Section(stringResource(R.string.section_distance_time)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
            Text(
                state.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown),
                modifier = Modifier.alignByBaseline(),
                style = t.headline,
                color = if (state.km != null) c.textPrimary else c.textSecondary,
            )
            state.minutes?.let {
                Text(minutesText(it), modifier = Modifier.alignByBaseline(), style = t.title, color = c.textSecondary)
            }
        }
    }
    Spacer(Modifier.height(Space.s6))
    Section(stringResource(R.string.section_route)) { Route(state) }
}

/** 한 칸 창: `48.2 km · 52분` 한 줄. */
@Composable
private fun CompactMetrics(state: TripResultUiState) {
    val km = state.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown)
    val time = state.minutes?.let { minutesText(it) }
    Text(
        if (time != null) stringResource(R.string.trip_meta_pair, km, time) else km,
        style = AutoLogTheme.type.title,
        color = AutoLogTheme.colors.textPrimary,
    )
}

/** 경로선 — 끝난 운행이라 ○—● 가는 선(흐름 모션 없음). 도착지를 모르면 "위치 미상". */
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

/** 오른쪽 칸: 운행 목적 — 같은 폭 선택형 행 3개(8 dp 간격, 보기 모드에선 누를 수 없음) + 바로 아래 분류 한 줄. */
@Composable
private fun Purpose(state: TripResultUiState, onAction: (TripResultAction) -> Unit) {
    val c = AutoLogTheme.colors
    Section(stringResource(R.string.section_purpose)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            PURPOSE_CHOICES.forEach { category ->
                ChoiceRow(
                    stringResource(choiceLabel(category)),
                    selected = category == state.selected,
                    onClick = if (state.editing) ({ onAction(TripResultAction.Select(category)) }) else null,
                )
            }
        }
        Spacer(Modifier.height(Space.s3))
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
}

/** "3초 후 메인 화면으로 돌아가요" — 보조 문구 한 줄(막대 없음). 수정 중엔 없다. */
@Composable
private fun Countdown(state: TripResultUiState) {
    state.autoCloseSec?.let { sec ->
        Text(
            stringResource(R.string.result_auto_close, durationText(sec)),
            style = AutoLogTheme.type.caption,
            color = AutoLogTheme.colors.textSecondary,
        )
    }
}

/**
 * 행동 버튼 두 개. 보기: [수정하기](보조) · [메인 화면으로](주). 수정: [저장](주) · [메인 화면으로](보조) — 자리는 그대로, 강조만 옮긴다.
 * 왼쪽 버튼은 글자 폭, [메인 화면으로]가 나머지 — 반반이면 "메인 화면으로"가 두 줄로 꺾인다.
 */
@Composable
private fun Actions(state: TripResultUiState, onAction: (TripResultAction) -> Unit, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
        val home = stringResource(R.string.result_home)
        if (state.editing) {
            PrimaryButton(
                stringResource(R.string.result_save),
                onClick = { onAction(TripResultAction.Save) },
                height = Size.buttonMd,
                loading = state.saving,
            )
            SecondaryButton(home, onClick = { onAction(TripResultAction.GoHome) }, modifier = Modifier.weight(1f))
        } else {
            SecondaryButton(stringResource(R.string.result_edit), onClick = { onAction(TripResultAction.Edit) })
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

@Composable
private fun minutesText(minutes: Int): String = if (minutes < MINUTES_PER_HOUR) {
    stringResource(R.string.trip_minutes, minutes)
} else {
    stringResource(R.string.trip_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

private const val MINUTES_PER_HOUR = 60

/** 선택지 이름(일반 업무 · 출퇴근 · 개인 용무). */
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
