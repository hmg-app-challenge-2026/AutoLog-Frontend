package kr.autolog.vehicle.ui.review

import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.core.VoicePurpose
import kr.autolog.vehicle.core.VoiceResult
import kr.autolog.vehicle.data.repository.TripReview
import kr.autolog.vehicle.ui.design.formatKm
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 목적 확인 상태 계산(순수 함수, ReviewReducerTest 대상). [review] null + [loaded] = 운행을 찾지 못함.
 * 추천은 확신(`isConfident`)이고 "확인 필요"가 아닐 때만 큰 버튼으로 — 아니면 선택지를 먼저 보여 준다.
 */
internal fun reduceReview(
    review: TripReview?,
    loaded: Boolean,
    candidates: List<String>,
    voiceAvailable: Boolean,
    interaction: ReviewInteraction,
): ReviewUiState {
    if (!loaded) return ReviewUiState(loading = true)
    if (review == null) return ReviewUiState(loading = false, notFound = true)
    val suggestion =
        review.suggestion?.takeIf { it.isConfident && it.category != PurposeCategory.UNDETERMINED }?.detail
    val voice = interaction.voice
    // 말로 고르다 실패하면 바로 탭할 수 있게 선택지를 편다
    val voiceFellBack = voice is VoiceUi.NotUnderstood || voice is VoiceUi.Failed
    return ReviewUiState(
        loading = false,
        summary = review.toSummaryUi(),
        suggestion = suggestion,
        showOptions = suggestion == null || interaction.showOptions || voiceFellBack,
        destinationCandidates = if (review.destination == null) candidates else emptyList(),
        chosenDestination = interaction.chosenDestination,
        voiceAvailable = voiceAvailable,
        voice = voice,
        saving = interaction.saving,
        place = interaction.place,
    )
}

/** 음성 결과 → 화면 상태. null = 시간 초과. 목적이 "확인 필요"면 알아듣지 못한 것과 같다. */
internal fun VoiceResult<VoicePurpose>?.toVoiceUi(): VoiceUi = when (this) {
    null -> VoiceUi.Failed(null)
    is VoiceResult.Recognized ->
        if (value.detail.category == PurposeCategory.UNDETERMINED) {
            VoiceUi.NotUnderstood(text)
        } else {
            VoiceUi.Heard(text, value.detail, value.note ?: text)
        }
    is VoiceResult.NotUnderstood -> VoiceUi.NotUnderstood(text)
    is VoiceResult.Failed -> VoiceUi.Failed(reason)
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

private fun epochTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(AppZone).format(TIME)

private fun TripReview.toSummaryUi() = ReviewSummaryUi(
    start = epochTime(startTime),
    end = endTime?.let(::epochTime),
    km = formatKm(distanceKm),
    origin = origin,
    destination = destination,
)
