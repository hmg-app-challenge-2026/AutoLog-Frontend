package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.data.interim.LiveTrip
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.MonthSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeReducerTest {
    private val trip = LiveTrip("t-1", startTime = 0L, 18_420.5, "판교", "u-1", originLabel = "본사")
    private val month = MonthSummary(tripCount = 23, totalKm = 412.6, businessKm = 388.1, reviewCount = 1)

    @Test
    fun `대기면 이번 달 요약이 주인공이고 놓친 운행은 알림 목록에 든다`() {
        val s = reduceHome(SessionState.Idle, MissedTrip(15.0, 0), destination = null, now = 0, month = month)

        assertEquals(HomePhase.READY, s.phase)
        assertEquals(MonthUi(23, "412.6", "388.1"), s.month)
        assertEquals(listOf<HomeNotice>(HomeNotice.MissedTrip("15.0")), s.notices)
    }

    @Test
    fun `알림은 목적 확인 → 영수증 → 놓친 운행 순서`() {
        val s =
            reduceHome(
                SessionState.PendingReview(trip, 23.4, null),
                MissedTrip(15.0, 0),
                destination = null,
                now = 0,
                pendingReviewIds = listOf("t-1", "t-0"),
                receiptIds = listOf("r-1"),
            )

        assertEquals(
            listOf(HomeNotice.Review(2), HomeNotice.Receipt(1), HomeNotice.MissedTrip("15.0")),
            s.notices,
        )
    }

    @Test
    fun `기록 중에는 놓친 운행을 묻지 않는다 — 지금 운행이 먼저`() {
        val s =
            reduceHome(
                SessionState.Recording(trip, 23.4, 0),
                MissedTrip(15.0, 0),
                "판교",
                now = 0,
                receiptIds = listOf("r-1"),
            )

        assertEquals(listOf<HomeNotice>(HomeNotice.Receipt(1)), s.notices)
    }

    @Test
    fun `기록 중이면 거리·경로·경과 분`() {
        val s = reduceHome(SessionState.Recording(trip, 23.4, 0), null, destination = "판교", now = 35 * 60_000L)

        assertEquals(HomePhase.RECORDING_PARKED, s.phase)
        assertEquals("23.4", s.tripKm)
        assertEquals("본사", s.origin)
        assertEquals("판교", s.destination)
        assertEquals(35, s.elapsedMin)
    }

    @Test
    fun `기어 P 카운트다운은 남은 초와 진행률`() {
        val candidate = SessionState.StopCandidate(trip, 23.4, StopReason.GEAR_P, since = 0, deadlineAt = 5_000)

        val s = reduceHome(candidate, null, destination = "판교", now = 2_000)

        assertEquals(HomePhase.AUTO_ENDING, s.phase)
        assertEquals(3, s.autoEnd!!.remainingSec)
        assertEquals(0.6f, s.autoEnd!!.progress, 0.001f)
        assertEquals("23.4", s.tripKm)
    }

    @Test
    fun `복구 상황이면 늘어난 거리를 보여 준다`() {
        val s = reduceHome(SessionState.NeedsRecovery(trip, 3.2), null, destination = null, now = 0)

        assertEquals(HomePhase.NEEDS_RECOVERY, s.phase)
        assertEquals("3.2", s.recoveryExtraKm)
    }

    @Test
    fun `처리할 일이 없으면 알림 목록이 비어 칸이 사라진다`() {
        assertTrue(reduceHome(SessionState.Idle, null, null, now = 0).notices.isEmpty())
    }
}
