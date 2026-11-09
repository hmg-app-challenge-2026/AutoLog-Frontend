package kr.autolog.vehicle.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.IconButtonXL
import kr.autolog.vehicle.ui.design.components.OptionButton
import kr.autolog.vehicle.ui.design.components.OptionGroup
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.RouteFlow
import kr.autolog.vehicle.ui.design.components.ScrollColumn
import kr.autolog.vehicle.ui.design.components.Section
import kr.autolog.vehicle.ui.design.components.TertiaryButton
import kr.autolog.vehicle.ui.design.components.TopLine
import kr.autolog.vehicle.ui.design.screenMargin
import kr.autolog.vehicle.ui.design.withRo

@Composable
fun ReviewRoute(tripId: String) {
    val graph = LocalAppGraph.current
    val vm = viewModel { ReviewViewModel(tripId, graph.review, graph.navigator) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    ReviewScreen(state, vm::onAction)
}

/**
 * 스펙 7.3 목적 확인. 위에서 아래로(좁은 창) / 좌 요약 · 우 결정(넓은 창, 디자인 언어 Decision 패턴):
 * 운행 요약(시각·거리 + 경로선, 도착지를 모르면 최근 목적지) → 추천(확신일 때만 큰 버튼 1개) → 말로 입력 → 선택지 → 나중에.
 * 추천·선택지는 1탭으로 바로 확정한다(조작 예산 1탭). 확정 뒤 거래처 등록 제안은 [PlaceProposalDialog].
 */
@Composable
fun ReviewScreen(state: ReviewUiState, onAction: (ReviewAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val layout = LocalWindowLayout.current
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(screenMargin()), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            TopLine { Text(stringResource(R.string.review_title), style = t.title, color = c.textPrimary) }
            when {
                state.loading -> Unit
                state.notFound -> {
                    Text(stringResource(R.string.review_not_found), style = t.body, color = c.textSecondary)
                    TertiaryButton(stringResource(R.string.review_later), onClick = { onAction(ReviewAction.Later) })
                }
                layout == WindowLayout.WIDE ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(Space.s7)) {
                        ScrollColumn(Modifier.weight(0.4f).fillMaxHeight()) { TripSummary(state, onAction) }
                        ScrollColumn(Modifier.weight(0.6f).fillMaxHeight()) { Decision(state, onAction) }
                    }
                else ->
                    ScrollColumn(Modifier.weight(1f).fillMaxWidth()) {
                        TripSummary(state, onAction)
                        Spacer(Modifier.height(Space.s6))
                        Decision(state, onAction)
                    }
            }
        }
        state.place?.let { PlaceProposalDialog(it, onAction) }
    }
}

/** 운행 요약: `09:00 → 09:35 · 23.4 km` + 경로선(끝난 운행이라 흐름 모션 없음). 도착지를 모르면 최근 목적지 칩. */
@Composable
private fun TripSummary(state: ReviewUiState, onAction: (ReviewAction) -> Unit) {
    val summary = state.summary ?: return
    val c = AutoLogTheme.colors
    Section(stringResource(R.string.section_trip_summary)) {
        Text(summaryLine(summary), style = AutoLogTheme.type.label, color = c.textPrimary)
        Spacer(Modifier.height(Space.s4))
        val destination = state.chosenDestination ?: summary.destination
        RouteFlow(
            origin = summary.origin,
            destination = destination ?: stringResource(R.string.review_choose_destination),
            destinationColor = if (destination != null) c.textPrimary else c.textSecondary,
            live = false,
        )
        if (state.destinationCandidates.isNotEmpty()) {
            Spacer(Modifier.height(Space.s5))
            OptionGroup(stringResource(R.string.section_recent_destinations)) {
                state.destinationCandidates.forEach { label ->
                    OptionButton(
                        label,
                        selected = label == state.chosenDestination,
                        onClick = { onAction(ReviewAction.ChooseDestination(label)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun summaryLine(s: ReviewSummaryUi): String {
    val time = s.end?.let { stringResource(R.string.review_time_range, s.start, it) } ?: s.start
    val km = s.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown)
    return stringResource(R.string.review_time_meta, time, km)
}

/** 결정 칸: 추천 → 말로 입력 → 선택지(또는 [다른 목적]) → 나중에. */
@Composable
private fun Decision(state: ReviewUiState, onAction: (ReviewAction) -> Unit) {
    val heard = state.voice as? VoiceUi.Heard
    Column(verticalArrangement = Arrangement.spacedBy(Space.s6)) {
        val suggestion = state.suggestion
        if (suggestion != null && heard == null) {
            Section(stringResource(R.string.section_suggested_purpose)) {
                PrimaryButton(
                    stringResource(R.string.review_suggest, withRo(suggestion.labelKo)),
                    onClick = { onAction(ReviewAction.Confirm(suggestion)) },
                    modifier = Modifier.fillMaxWidth(),
                    height = Size.buttonXl,
                    loading = state.saving,
                )
            }
        }
        if (state.voiceAvailable) VoiceInput(state, onAction)
        val optionsOpen = state.showOptions || heard != null
        if (optionsOpen) PurposeOptions(state, selected = heard?.detail, onAction)
        // 보조 동작은 한 줄에 — 640 dp 창에서 [나중에]가 스크롤 아래로 숨지 않게
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s5)) {
            if (!optionsOpen) {
                TertiaryButton(
                    stringResource(R.string.review_other_purpose),
                    onClick = { onAction(ReviewAction.ShowOptions) },
                )
            }
            TertiaryButton(stringResource(R.string.review_later), onClick = { onAction(ReviewAction.Later) })
        }
    }
}

/**
 * 말로 입력: 마이크(96 dp) 옆에 지금 상태 한 줄. 듣는 중에는 파형 대신 정적 "듣고 있어요…"(디자인 언어 8장 — 주의를 끄는 반복 모션 금지).
 * 알아들으면 원문 + 아래 선택지에 고른 목적 표시 + [저장]. 마이크를 다시 누르면 다시 듣는다.
 */
@Composable
private fun VoiceInput(state: ReviewUiState, onAction: (ReviewAction) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val voice = state.voice
    Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s5)) {
            IconButtonXL(
                Icons.Rounded.Mic,
                label =
                stringResource(if (voice == VoiceUi.Idle) R.string.review_voice else R.string.review_voice_retry),
                onClick = { onAction(ReviewAction.Listen) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                when (voice) {
                    VoiceUi.Idle -> Text(
                        stringResource(R.string.review_voice),
                        style = t.label,
                        color = c.textSecondary,
                    )
                    VoiceUi.Listening -> Text(
                        stringResource(R.string.review_listening),
                        style = t.title,
                        color = c.accent,
                    )
                    is VoiceUi.Heard ->
                        Text(stringResource(R.string.review_heard, voice.text), style = t.title, color = c.textPrimary)
                    is VoiceUi.NotUnderstood -> {
                        Text(stringResource(R.string.review_heard, voice.text), style = t.title, color = c.textPrimary)
                        Text(stringResource(R.string.review_not_understood), style = t.caption, color = c.textSecondary)
                    }
                    is VoiceUi.Failed ->
                        Text(
                            voice.reason ?: stringResource(R.string.review_voice_timeout),
                            style = t.body,
                            color = c.textSecondary,
                        )
                }
            }
        }
        if (voice is VoiceUi.Heard) {
            PrimaryButton(
                stringResource(R.string.review_voice_save),
                onClick = { onAction(ReviewAction.SaveVoice) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.saving,
            )
        }
    }
}

/**
 * 선택지 3묶음. 1탭 = 바로 확정. [selected]는 말로 고른 목적(링 + 체크).
 * 묶음 이름(일반 업무용 · 출퇴근용 · 비업무용)이 곧 소제목이라 그 위에 소제목을 하나 더 두지 않는다.
 */
@Composable
private fun PurposeOptions(state: ReviewUiState, selected: PurposeDetail?, onAction: (ReviewAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.s5)) {
        state.groups.forEach { group ->
            OptionGroup(group.category.labelKo) {
                group.options.forEach { detail ->
                    OptionButton(
                        detail.labelKo,
                        selected = detail == selected,
                        onClick = { onAction(ReviewAction.Confirm(detail)) },
                    )
                }
            }
        }
    }
}
