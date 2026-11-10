package kr.autolog.vehicle.ui.result

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.nav.TripResultMode
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
import kr.autolog.vehicle.ui.design.twoColumns
import kr.autolog.vehicle.ui.trips.tripWhenText

@Composable
fun TripResultRoute(tripId: String, mode: TripResultMode = TripResultMode.AFTER_TRIP) {
    val graph = LocalAppGraph.current
    val vm =
        viewModel {
            TripResultViewModel(tripId, graph.tripResults, graph.session, graph.navigator, graph.clock, mode)
        }
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
 * 넓은 창과 Pleos 1:1 분할 한 칸(≈1280×1268)은 같은 두 칸 배치·같은 글자. 세로로 긴 창(TALL)만 세로 한 칸(넘치면 그 칸만 스크롤) + 분류 문구 + 버튼 두 줄.
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
    val twoColumns = layout.twoColumns
    val margin = screenMargin()
    // 두 칸 배치(넓은 창·분할 화면)는 위아래 여백을 16 dp로 줄여 두 칸에 높이를 준다(에뮬레이터에서 맞춤, PR #4)
    val vertical = if (twoColumns) Space.s4 else margin
    Column(
        Modifier.fillMaxSize().padding(start = margin, top = vertical, end = margin, bottom = vertical),
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        TopLine {
            Text(
                resultTitle(state),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = AutoLogTheme.windowType.resultTitle,
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
            twoColumns -> {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    val availableHeight = maxHeight
                    val spacious = availableHeight >= Size.spaciousHeight
                    val summaryGap = if (spacious) SUMMARY_GAP_SPACIOUS else Space.s7
                    TwoColumns(
                        Modifier
                            .heightIn(max = availableHeight)
                            .then(if (spacious) Modifier.offset(y = -COLUMNS_LIFT_SPACIOUS) else Modifier),
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
                // 세로로 긴 창: 넓은 창과 같은 순서 — 운행 거리 · 시간 → 운행 경로 → 운행 목적(세로 선택형 행).
                // 다 들어가지 않으면 이 칸만 스크롤되고 분류 문구·버튼은 늘 보인다
                val scroll = rememberScrollState()
                // [수정]을 누르면 아래에 있는 운행 목적이 보이게 끝까지 내린다
                LaunchedEffect(state.editing) { if (state.editing) scroll.animateScrollTo(scroll.maxValue) }
                ScrollColumn(Modifier.weight(1f).fillMaxWidth(), scroll) {
                    TripSummary(state)
                    Spacer(Modifier.height(Space.s6))
                    Purpose(state, onAction)
                }
                ClassificationSummary(state)
                CompactActions(state, onAction)
            }
        }
    }
}

/** 콘텐츠 폭을 좌우 반반으로 — 위 섹션과 아래 행동 줄이 같은 두 칸을 쓴다(공통 그리드). */
@Composable
private fun TwoColumns(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s7), content = content)
}

/** 칸 높이가 넉넉한 차량 화면에서 거리·시간과 경로 사이 간격(112 dp). 작은 창은 48 dp. */
private val SUMMARY_GAP_SPACIOUS = Space.s7 * 2 + Space.s4

/** 칸 높이가 넉넉한 차량 화면에서 두 칸을 세로 가운데보다 이만큼 위에 둔다. */
private val COLUMNS_LIFT_SPACIOUS = Space.s6

/**
 * 운행 결과의 섹션 하나. 두 칸 배치는 소제목 아래를 16 dp 더 띄운다(Pleos 에뮬레이터 2560×1440에서 맞춤, PR #4) —
 * 운행 요약과 운행 목적이 같은 규칙을 쓰도록 여기 한 곳에서.
 */
@Composable
private fun ResultSection(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    val twoColumns = LocalWindowLayout.current.twoColumns
    Section(stringResource(title)) {
        if (twoColumns) Spacer(Modifier.height(Space.s4))
        content()
    }
}

/** 운행 요약: "운행 거리 · 시간" → [gap] → "운행 경로", 위아래로. */
@Composable
private fun TripSummary(state: TripResultUiState, gap: Dp = Space.s7) {
    Column {
        ResultSection(R.string.section_distance_time) { DistanceTime(state) }
        Spacer(Modifier.height(gap))
        ResultSection(R.string.section_route) { Route(state) }
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
private fun Purpose(state: TripResultUiState, onAction: (TripResultAction) -> Unit) {
    ResultSection(R.string.section_purpose) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            PURPOSE_CHOICES.forEach { category ->
                ChoiceRow(
                    stringResource(choiceLabel(category)),
                    selected = category == state.selected,
                    onClick = if (state.editing) ({ onAction(TripResultAction.Select(category)) }) else null,
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
        style = AutoLogTheme.windowType.resultClassification,
        color = c.textPrimary,
    )
}

/**
 * 두 칸 배치(넓은 창·분할 화면) 행동 버튼(같은 폭, 이 순서): [계속 기록하기](종료 대기 중에만) · [수정하기]/[저장] · [메인 화면으로 · n초].
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
        when {
            state.review -> stringResource(R.string.result_close)
            countdownInLabel && sec != null -> stringResource(R.string.result_home_in, durationText(sec))
            else -> stringResource(R.string.result_home)
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
            labelStyle = AutoLogTheme.windowType.resultHomeButton,
        )
    }
}

/** "운행 기록을 완료했어요", 사후 확인이면 "11월 7일 19:34 운행"(D-29). */
@Composable
private fun resultTitle(state: TripResultUiState): String =
    state.startedAt?.let { stringResource(R.string.result_review_title, tripWhenText(it)) }
        ?: stringResource(R.string.result_title)

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
