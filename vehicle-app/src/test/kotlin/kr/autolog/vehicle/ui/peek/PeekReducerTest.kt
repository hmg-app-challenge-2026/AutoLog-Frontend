package kr.autolog.vehicle.ui.peek

import kr.autolog.vehicle.data.repository.ActiveTrip
import kr.autolog.vehicle.data.repository.AutoEndReason
import kr.autolog.vehicle.data.repository.TripSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeekReducerTest {
    private val trip = ActiveTrip("t-1", 0L, origin = "본사")

    @Test
    fun `기록 중이면 거리와 출발지 → 목적지 경로를 보여 준다`() {
        val state =
            reducePeek(TripSession.Recording(trip, 23.4), 18_443.9, "판교", now = 13_000)

        val r = state as PeekUiState.Recording
        assertEquals("23.4", r.tripKm)
        assertEquals("본사", r.origin)
        assertEquals("판교", r.destination)
        assertNull(r.autoEndInSec)
    }

    @Test
    fun `움직임 없음 자동 종료 대기면 남은 시간을 보여 준다`() {
        val candidate = TripSession.AutoEnding(trip, 23.4, AutoEndReason.NO_MOVEMENT, since = 0, deadlineAt = 180_000)

        val r = reducePeek(candidate, null, null, now = 19_000) as PeekUiState.Recording

        assertEquals(161, r.autoEndInSec)
    }

    @Test
    fun `계기판을 못 읽으면 거리를 비운다`() {
        val r = reducePeek(TripSession.Recording(trip, null), null, null, now = 0) as PeekUiState.Recording

        assertNull(r.tripKm)
    }

    @Test
    fun `운행이 없으면 누적거리만`() {
        val r = reducePeek(TripSession.Idle, 18_420.5, null, now = 0)

        assertEquals(PeekUiState.NoTrip("18,420.5"), r)
    }
}
