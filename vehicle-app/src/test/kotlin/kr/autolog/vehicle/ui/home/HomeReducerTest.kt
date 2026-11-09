package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.data.interim.LiveTrip
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.HistoryItem
import kr.autolog.vehicle.data.repository.MonthSummary
import kr.autolog.vehicle.data.repository.UploadInfo
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

    @Test
    fun `기록 자체의 문제(계기판·저장·전송)가 처리할 일보다 먼저`() {
        val noOdo = SessionState.Recording(trip, liveKm = null, lastSavedAt = 0)
        val upload = UploadInfo(enabled = true, pendingCount = 2, lastUploadAt = null, lastError = "timeout")

        val s = reduceHome(noOdo, null, "판교", now = 40_000, pendingReviewIds = listOf("t-0"), upload = upload)

        assertEquals(
            listOf(
                HomeNotice.OdometerUnavailable,
                HomeNotice.SaveDelayed,
                HomeNotice.UploadFailed(2),
                HomeNotice.Review(1),
            ),
            s.notices,
        )
    }

    @Test
    fun `저장이 30초 안이고 전송이 꺼져 있으면 문제 알림이 없다`() {
        val ok = SessionState.Recording(trip, 23.4, lastSavedAt = 20_000)
        val off = UploadInfo(enabled = false, pendingCount = 5, lastUploadAt = null, lastError = "x")

        assertTrue(reduceHome(ok, null, "판교", now = 40_000, upload = off).notices.isEmpty())
    }

    @Test
    fun `최근 운행은 최대 3건, 오늘·어제·그 전으로 나누고 목적이 없으면 확인 필요`() {
        val now = java.time.ZonedDateTime.of(
            2026,
            11,
            9,
            12,
            0,
            0,
            0,
            kr.autolog.vehicle.common.AppZone,
        ).toInstant().toEpochMilli()
        fun at(day: Int, h: Int) = java.time.ZonedDateTime.of(
            2026,
            11,
            day,
            h,
            12,
            0,
            0,
            kr.autolog.vehicle.common.AppZone,
        ).toInstant().toEpochMilli()
        fun item(id: String, start: Long, purpose: PurposeDetail?) =
            HistoryItem(id, start, start + 1, "김영업", false, "본사", "판교", 23.4, purpose, null)
        val recent =
            listOf(
                item("a", at(9, 9), null),
                item("b", at(8, 17), PurposeDetail.CLIENT_VISIT),
                item("c", at(6, 13), PurposeDetail.FIELD_WORK),
                item("d", at(5, 8), PurposeDetail.FIELD_WORK),
            )

        val s = reduceHome(SessionState.Idle, null, null, now = now, recent = recent)

        assertEquals(listOf("a", "b", "c"), s.recentTrips.map { it.tripId })
        assertEquals(listOf(TripDay.TODAY, TripDay.YESTERDAY, TripDay.EARLIER), s.recentTrips.map { it.day })
        assertEquals("09:12", s.recentTrips[0].time)
        assertTrue(s.recentTrips[0].needsReview)
        assertEquals("거래처 방문", s.recentTrips[1].purpose)
        assertEquals(6, s.recentTrips[2].dayOfMonth)
    }
}
