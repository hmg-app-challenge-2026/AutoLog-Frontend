package kr.autolog.vehicle.ui.result

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kr.autolog.vehicle.ui.design.components.EdgeCountdownBar
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
    val vm = viewModel { TripResultViewModel(tripId, graph.tripResults, graph.session, graph.navigator, graph.clock) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    TripResultScreen(state, vm::onAction)
}

/**
 * 스펙 7.3 운행 결과(D-27). **정렬축은 하나**: 화면 여백 안의 콘텐츠 폭을 좌우 반반으로 나누고, 아래 행동 줄도 같은 두 칸을 쓴다.
 * - 맨 위 줄 가운데: 제목 "운행 기록을 완료했어요"(운전자 배지 없음).
 * - 왼쪽 칸 = 운행 요약: "운행 거리 · 시간"(큰 거리 옆에 작은 시간) → "운행 경로"(○—● 경로선).
 * - 오른쪽 칸 = 운행 목적: 같은 폭 선택형 행 3개(8 dp 간격).
 * - 두 칸 아래 가운데 = "일반 업무용 거리 48.2 km로 분류돼요"(선택의 결과), 행동 버튼 바로 위.
 * - 아래 행동 줄: [계속 기록하기](종료 대기 중에만) · [수정하기]/[저장] · [메인 화면으로 · 3초]를
 *   콘텐츠 폭 전체에 같은 폭으로(위 두 칸과 같은 좌우 끝선). 남은 시간은 자동으로 눌릴 주 버튼 글자 + 맨 위 가장자리 막대.
 * NARROW·TALL: 넓은 창 순서를 세로 한 칸에(넘치면 그 칸만 스크롤) + 하단 고정 행동 — NARROW는 짧은 이름 한 줄, TALL은 두 줄.
 */
@Composable
fun TripResultScreen(state: TripResultUiState, onAction: (TripResultAction) -> Unit, modifier: Modifier = Modifier) {
    val layout = LocalWindowLayout.current
    Box(modifier.fillMaxSize()) {
        ResultBody(state, onAction, layout)
        // 맨 위 가장자리에 붙는 남은 시간 막대 — 오른쪽 끝이 왼쪽으로 줄어든다(수정 중엔 없다)
        if (state.autoCloseSec != null) EdgeCountdownBar(state.autoCloseProgress, Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun ResultBody(state: TripResultUiState, onAction: (TripResultAction) -> Unit, layout: WindowLayout) {
    val c = AutoLogTheme.colors
    // 좁은 창(분할 화면)은 한 단계 작은 글자·얇은 면(dense)으로 — 같은 내용을 덜 꽉 차게.
    // 버튼은 화면 바닥 가까이(아래 여백 8 dp — 버튼 면 아래 투명 터치 영역 10 dp와 합쳐 바닥에서 18 dp), 그만큼 위 내용이 더 보인다
    val dense = layout == WindowLayout.NARROW
    val margin = screenMargin()
    Column(
        Modifier.fillMaxSize().padding(
            start = margin,
            top = if (layout == WindowLayout.WIDE) Space.s4 else margin,
            end = margin,
            bottom = when (layout) {
                WindowLayout.NARROW -> Space.s2
                WindowLayout.WIDE -> Space.s4
                WindowLayout.TALL -> margin
            },
        ),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        TopLine {
            Text(
                stringResource(R.string.result_title),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = if (dense) AutoLogTheme.type.label else AutoLogTheme.type.title,
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
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    val availableHeight = maxHeight
                    val spacious = availableHeight >= 800.dp
                    val summaryGap = if (spacious) Space.s7 * 2 + Space.s4 else Space.s7
                    TwoColumns(
                        Modifier
                            .heightIn(max = availableHeight)
                            .offset(y = if (spacious) -Space.s6 else 0.dp),
                    ) {
                        ScrollColumn(Modifier.weight(1f).heightIn(max = availableHeight)) {
                            TripSummary(state, gap = summaryGap)
                        }
                        ScrollColumn(Modifier.weight(1f).heightIn(max = availableHeight)) {
                            Purpose(state, onAction)
                        }
                    }
                }
                ClassificationSummary(state)
                // 행동 줄: 버튼 3개를 콘텐츠 폭 전체에 같은 폭으로(위 두 칸과 같은 좌우 끝선). 남은 시간은 주 버튼 안에
                WideActions(state, onAction)
            }
            else -> {
                // 한 칸: 넓은 창과 같은 순서 — 운행 거리 · 시간 → 운행 경로 → 운행 목적(세로 선택형 행).
                // 분할 화면(504×640)에선 다 들어가지 않아 이 칸만 스크롤되고 버튼은 늘 보인다
                val scroll = rememberScrollState()
                // [수정]을 누르면 아래에 있는 운행 목적이 보이게 끝까지 내린다(좁은 창에선 첫 화면 밖)
                LaunchedEffect(state.editing) { if (state.editing) scroll.animateScrollTo(scroll.maxValue) }
                ScrollColumn(Modifier.weight(1f).fillMaxWidth(), scroll) {
                    TripSummary(state, dense)
                    Spacer(Modifier.height(if (dense) Space.s4 else Space.s6))
                    Purpose(state, onAction, dense)
                }
                ClassificationSummary(state)
                if (layout == WindowLayout.NARROW) NarrowActions(state, onAction) else CompactActions(state, onAction)
            }
        }
    }
}

/** 콘텐츠 폭을 좌우 반반으로 — 위 섹션과 아래 행동 줄이 같은 두 칸을 쓴다(공통 그리드). */
@Composable
private fun TwoColumns(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s7), content = content)
}

/**
 * 운행 요약: "운행 거리 · 시간" → "운행 경로". 일반 창은 48 dp, 높이가 넉넉한 차량 화면은 112 dp.
 * 좁은 창에서는 거리·시간과 경로를 전폭으로 쌓아 커진 값이 잘리지 않게 한다. 아래 목적 선택은 같은 칸에서 스크롤한다.
 */
@Composable
private fun TripSummary(state: TripResultUiState, dense: Boolean = false, gap: Dp = Space.s7) {
    val contentGap = if (LocalWindowLayout.current == WindowLayout.WIDE) Space.s4 else 0.dp
    if (dense) {
        Column {
            Section(stringResource(R.string.section_distance_time), dense = true) {
                Spacer(Modifier.height(contentGap))
                DistanceTime(state)
            }
            Spacer(Modifier.height(Space.s7))
            Section(stringResource(R.string.section_route), dense = true) {
                Spacer(Modifier.height(contentGap))
                Route(state)
            }
        }
        return
    }
    Section(stringResource(R.string.section_distance_time)) {
        Spacer(Modifier.height(contentGap))
        DistanceTime(state)
    }
    Spacer(Modifier.height(gap))
    Section(stringResource(R.string.section_route)) {
        Spacer(Modifier.height(contentGap))
        Route(state)
    }
}

/**
 * 큰 거리 + 작은 시간(회색 — 보조 지표)을 같은 기준선에 놓는다(`resultDistance` · `resultTime`).
 */
@Composable
private fun DistanceTime(state: TripResultUiState) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val km = state.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown)
    val kmColor = if (state.km != null) c.textPrimary else c.textSecondary
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
        Text(km, modifier = Modifier.alignByBaseline(), style = t.resultDistance, color = kmColor)
        state.minutes?.let {
            Text(minutesText(it), modifier = Modifier.alignByBaseline(), style = t.resultTime, color = c.textSecondary)
        }
    }
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

/** 운행 목적 — 같은 폭 선택형 행 3개(8 dp 간격, 보기 모드에선 누를 수 없음). */
@Composable
private fun Purpose(state: TripResultUiState, onAction: (TripResultAction) -> Unit, dense: Boolean = false) {
    Section(stringResource(R.string.section_purpose), dense = dense) {
        if (LocalWindowLayout.current == WindowLayout.WIDE) Spacer(Modifier.height(Space.s4))
        // dense 행은 면 위아래가 이미 비어 있어 간격 없이 쌓는다(면 사이 틈 20 dp)
        Column(verticalArrangement = if (dense) Arrangement.Top else Arrangement.spacedBy(Space.s2)) {
            PURPOSE_CHOICES.forEach { category ->
                ChoiceRow(
                    stringResource(choiceLabel(category)),
                    selected = category == state.selected,
                    onClick = if (state.editing) ({ onAction(TripResultAction.Select(category)) }) else null,
                    dense = dense,
                )
            }
        }
    }
}

/** 선택한 운행 목적의 분류 결과를 하단 행동 버튼 바로 위, 화면 가운데에 표시한다. */
@Composable
private fun ClassificationSummary(state: TripResultUiState) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val kind = stringResource(classLabel(state.selected))
    Text(
        state.km?.let { stringResource(R.string.result_classified, kind, stringResource(R.string.trip_km, it)) }
            ?: stringResource(R.string.result_classified_no_km, kind),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        style = if (LocalWindowLayout.current == WindowLayout.WIDE) t.title else t.label,
        color = c.textPrimary,
    )
}

/**
 * 넓은 창 행동 버튼(같은 폭, 이 순서): [계속 기록하기](종료 대기 중에만) · [수정하기]/[저장] · [메인 화면으로 · n초].
 * 보기: [메인 화면으로]가 주 버튼(시간이 다 되면 저절로 눌릴 버튼, 남은 시간을 글자에). 수정: [저장]이 주 버튼 — 자리는 그대로, 강조만 옮긴다(D-28).
 */
@Composable
private fun WideActions(state: TripResultUiState, onAction: (TripResultAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
        if (state.canContinue) {
            SecondaryButton(
                stringResource(R.string.result_continue),
                onClick = { onAction(TripResultAction.Continue) },
                modifier = Modifier.weight(1f),
            )
        }
        EditOrSave(state, onAction, Modifier.weight(1f))
        Home(state, onAction, Modifier.weight(1f), countdownInLabel = true)
    }
}

/**
 * 세로로 긴 창 행동 버튼(같은 순서, 두 줄): [계속 기록하기 | 수정하기](종료 대기 중이 아니면 [수정하기]만) / [메인 화면으로 · 3초](전체 폭).
 * 한 줄 셋이면 "메인 화면으로 · 3초"가 두 줄로 꺾인다.
 */
@Composable
private fun CompactActions(state: TripResultUiState, onAction: (TripResultAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (state.canContinue) {
                SecondaryButton(
                    stringResource(R.string.result_continue),
                    onClick = { onAction(TripResultAction.Continue) },
                    modifier = Modifier.weight(1f),
                )
                EditOrSave(state, onAction, Modifier)
            } else {
                EditOrSave(state, onAction, Modifier.weight(1f))
            }
        }
        Home(state, onAction, Modifier.fillMaxWidth(), countdownInLabel = true)
    }
}

/**
 * 좁은 창(분할 화면) 행동 버튼 — 한 줄, 같은 순서·같은 이름: [계속 기록하기](종료 대기 중에만) · [수정하기]/[저장] · [메인 화면으로 · 3초].
 * 폭 440 dp에 셋을 두려고 dense 버튼(글자 fine 20 sp — 글자 하한, 좌우 여백 12 dp). 앞 둘은 글자 폭, [메인 화면으로]가 남는 폭을 쓴다.
 */
@Composable
private fun NarrowActions(state: TripResultUiState, onAction: (TripResultAction) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s3)) {
        if (state.canContinue) {
            SecondaryButton(
                stringResource(R.string.result_continue),
                onClick = { onAction(TripResultAction.Continue) },
                dense = true,
            )
        }
        if (state.editing) {
            PrimaryButton(
                stringResource(R.string.result_save),
                onClick = { onAction(TripResultAction.Save) },
                height = Size.buttonMd,
                loading = state.saving,
                dense = true,
            )
            SecondaryButton(
                stringResource(R.string.result_home),
                onClick = { onAction(TripResultAction.GoHome) },
                modifier = Modifier.weight(1f),
                dense = true,
            )
        } else {
            SecondaryButton(stringResource(R.string.result_edit), onClick = {
                onAction(TripResultAction.Edit)
            }, dense = true)
            val sec = state.autoCloseSec
            PrimaryButton(
                if (sec != null) {
                    stringResource(R.string.result_home_in, durationText(sec))
                } else {
                    stringResource(R.string.result_home)
                },
                onClick = { onAction(TripResultAction.GoHome) },
                modifier = Modifier.weight(1f),
                height = Size.buttonMd,
                loading = state.saving,
                dense = true,
            )
        }
    }
}

@Composable
private fun EditOrSave(state: TripResultUiState, onAction: (TripResultAction) -> Unit, modifier: Modifier) {
    if (state.editing) {
        PrimaryButton(
            stringResource(R.string.result_save),
            onClick = { onAction(TripResultAction.Save) },
            modifier = modifier,
            height = Size.buttonMd,
            loading = state.saving,
        )
    } else {
        SecondaryButton(stringResource(R.string.result_edit), onClick = {
            onAction(TripResultAction.Edit)
        }, modifier = modifier)
    }
}

/** [메인 화면으로]. [countdownInLabel]이면 남은 시간을 버튼 글자에("메인 화면으로 · 3초") — 이 버튼이 자동으로 눌린다는 뜻. */
@Composable
private fun Home(
    state: TripResultUiState,
    onAction: (TripResultAction) -> Unit,
    modifier: Modifier,
    countdownInLabel: Boolean = false,
) {
    val sec = state.autoCloseSec
    val home =
        if (countdownInLabel && sec != null) {
            stringResource(R.string.result_home_in, durationText(sec))
        } else {
            stringResource(R.string.result_home)
        }
    if (state.editing) {
        SecondaryButton(home, onClick = { onAction(TripResultAction.GoHome) }, modifier = modifier)
    } else {
        PrimaryButton(
            home,
            onClick = { onAction(TripResultAction.GoHome) },
            modifier = modifier,
            height = Size.buttonMd,
            loading = state.saving,
            labelStyle = if (LocalWindowLayout.current ==
                WindowLayout.WIDE
            ) {
                AutoLogTheme.type.caption
            } else {
                AutoLogTheme.type.label
            },
        )
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
