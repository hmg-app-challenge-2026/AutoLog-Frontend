package kr.autolog.vehicle.ui.review

import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.core.PurposeRuleId
import kr.autolog.vehicle.core.Suggestion
import kr.autolog.vehicle.core.VoicePurpose
import kr.autolog.vehicle.core.VoiceResult
import kr.autolog.vehicle.data.repository.TripReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class ReviewReducerTest {
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 11, 9, h, m, 0, 0, AppZone).toInstant().toEpochMilli()

    private val confident = Suggestion(PurposeDetail.CLIENT_VISIT, 0.97, PurposeRuleId.SCHEDULE)
    private val trip = TripReview("t-1", at(9, 0), at(9, 35), 23.4, "본사", "판교", confident)

    private fun reduce(review: TripReview? = trip, interaction: ReviewInteraction = ReviewInteraction()) =
        reduceReview(review, loaded = true, candidates = listOf("본사", "수원 지점"), voiceAvailable = true, interaction)

    @Test
    fun `요약은 시각 두 개와 거리, 확신 추천이면 큰 버튼 하나고 선택지는 접혀 있다`() {
        val s = reduce()

        assertEquals(ReviewSummaryUi("09:00", "09:35", "23.4", "본사", "판교"), s.summary)
        assertEquals(PurposeDetail.CLIENT_VISIT, s.suggestion)
        assertFalse(s.showOptions)
        assertTrue(s.destinationCandidates.isEmpty())
    }

    @Test
    fun `확신이 낮거나 확인 필요 추천이면 추천 없이 선택지를 먼저 보여 준다`() {
        val low = reduce(trip.copy(suggestion = confident.copy(confidence = 0.6)))
        val undetermined =
            reduce(trip.copy(suggestion = Suggestion(PurposeDetail.NEEDS_CHECK, 0.9, PurposeRuleId.FALLBACK)))

        assertNull(low.suggestion)
        assertTrue(low.showOptions)
        assertNull(undetermined.suggestion)
    }

    @Test
    fun `도착지를 모르면 최근 목적지를 후보로 준다`() {
        assertEquals(listOf("본사", "수원 지점"), reduce(trip.copy(destination = null)).destinationCandidates)
    }

    @Test
    fun `말로 고르다 실패하면 선택지를 편다`() {
        assertTrue(reduce(interaction = ReviewInteraction(voice = VoiceUi.Failed(null))).showOptions)
    }

    @Test
    fun `운행을 찾지 못하면 notFound, 읽기 전이면 loading`() {
        assertTrue(reduce(review = null).notFound)
        assertTrue(reduceReview(null, loaded = false, emptyList(), false, ReviewInteraction()).loading)
    }

    @Test
    fun `선택지는 업무·출퇴근·비업무 세 묶음이고 확인 필요는 없다`() {
        assertEquals(
            listOf(PurposeCategory.BUSINESS, PurposeCategory.COMMUTE, PurposeCategory.NON_BUSINESS),
            PURPOSE_GROUPS.map { it.category },
        )
        assertEquals(8, PURPOSE_GROUPS.sumOf { it.options.size })
    }

    @Test
    fun `음성 결과 — 알아들으면 원문을 메모로, 확인 필요면 못 알아들은 것, 시간 초과는 이유 없는 실패`() {
        val heard: VoiceResult<VoicePurpose> = VoiceResult.Recognized(
            "판교 A고객사 미팅",
            VoicePurpose(PurposeDetail.CLIENT_VISIT),
        )
        val vague: VoiceResult<VoicePurpose> = VoiceResult.Recognized("음", VoicePurpose(PurposeDetail.NEEDS_CHECK))

        assertEquals(VoiceUi.Heard("판교 A고객사 미팅", PurposeDetail.CLIENT_VISIT, "판교 A고객사 미팅"), heard.toVoiceUi())
        assertEquals(VoiceUi.NotUnderstood("음"), vague.toVoiceUi())
        assertEquals(VoiceUi.Failed(null), null.toVoiceUi())
    }

    @Test
    fun `이미 목적이 있는 운행(고치러 옴)은 추천 없이 지금 목적이 체크된 선택지`() {
        val s = reduce(trip.copy(purpose = PurposeDetail.FIELD_WORK))

        assertNull(s.suggestion)
        assertEquals(PurposeDetail.FIELD_WORK, s.currentPurpose)
        assertTrue(s.showOptions)
    }
}
