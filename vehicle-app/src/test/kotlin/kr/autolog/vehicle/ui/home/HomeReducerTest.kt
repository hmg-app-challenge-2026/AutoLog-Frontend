package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.data.interim.LiveTrip
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeReducerTest {
    private val trip = LiveTrip("t-1", startTime = 0L, 18_420.5, "판교", "u-1")

    @Test
    fun `운행이 없으면 누적거리와 놓친 운행 배너`() {
        val s = reduceHome(SessionState.Idle, 18_420.5, MissedTrip(15.0, 0), destination = null, now = 0)

        assertEquals(HomePhase.READY, s.phase)
        assertEquals("18,420.5", s.odometerKm)
        assertEquals("15.0", s.missedTripKm)
    }

    @Test
    fun `기어 P 카운트다운은 남은 초와 진행률`() {
        val candidate = SessionState.StopCandidate(trip, 23.4, StopReason.GEAR_P, since = 0, deadlineAt = 5_000)

        val s = reduceHome(candidate, null, null, destination = "판교", now = 2_000)

        assertEquals(HomePhase.AUTO_ENDING, s.phase)
        assertEquals(3, s.autoEnd!!.remainingSec)
        assertEquals(0.6f, s.autoEnd!!.progress, 0.001f)
        assertEquals("23.4", s.tripKm)
    }

    @Test
    fun `자동 종료를 취소하고 주차 중이면 기록 중 화면과 경과 분`() {
        val s = reduceHome(SessionState.Recording(trip, 23.4, 0), null, null, destination = "판교", now = 35 * 60_000L)

        assertEquals(HomePhase.RECORDING_PARKED, s.phase)
        assertEquals(35, s.elapsedMin)
        assertNull(s.missedTripKm)
    }

    @Test
    fun `목적 확인이 남으면 확인 필요 수와 가장 최근 운행`() {
        val s =
            reduceHome(
                SessionState.PendingReview(trip, 23.4, null),
                18_443.9,
                null,
                destination = null,
                now = 0,
                pendingReviewIds = listOf("t-1", "t-0"),
                receiptIds = listOf("r-1"),
            )

        assertEquals(HomePhase.READY, s.phase)
        assertEquals(2, s.reviewCount)
        assertEquals("t-1", s.latestReviewId)
        assertEquals(1, s.receiptCount)
    }

    @Test
    fun `복구 상황이면 늘어난 거리를 보여 준다`() {
        val s = reduceHome(SessionState.NeedsRecovery(trip, 3.2), null, null, destination = null, now = 0)

        assertEquals(HomePhase.NEEDS_RECOVERY, s.phase)
        assertEquals("3.2", s.recoveryExtraKm)
    }
}
