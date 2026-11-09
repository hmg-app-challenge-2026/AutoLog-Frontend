package kr.autolog.vehicle.ui.peek

import kr.autolog.vehicle.data.interim.LiveTrip
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeekReducerTest {
    private val trip = LiveTrip("t-1", 0L, 18_420.5, "판교", "u-1")

    @Test
    fun `기록 중이면 거리·목적지·저장 시각을 보여 준다`() {
        val state = reducePeek(SessionState.Recording(trip, 23.4, lastSavedAt = 10_000), 18_443.9, "김영업", "판교", now = 13_000)

        val r = state as PeekUiState.Recording
        assertEquals("23.4", r.tripKm)
        assertEquals("판교", r.destination)
        assertEquals(3, r.savedSecondsAgo)
        assertNull(r.autoEndInSec)
    }

    @Test
    fun `움직임 없음 자동 종료 대기면 남은 시간을 보여 준다`() {
        val candidate = SessionState.StopCandidate(trip, 23.4, StopReason.NO_MOVEMENT, since = 0, deadlineAt = 180_000)

        val r = reducePeek(candidate, null, null, null, now = 19_000) as PeekUiState.Recording

        assertEquals(161, r.autoEndInSec)
    }

    @Test
    fun `계기판을 못 읽으면 거리를 비운다`() {
        val r = reducePeek(SessionState.Recording(trip, null, 0), null, null, null, now = 0) as PeekUiState.Recording

        assertNull(r.tripKm)
    }

    @Test
    fun `운행이 없으면 누적거리만`() {
        val r = reducePeek(SessionState.Idle, 18_420.5, null, null, now = 0)

        assertEquals(PeekUiState.NoTrip("18,420.5"), r)
    }
}
