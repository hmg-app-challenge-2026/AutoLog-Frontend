package kr.autolog.vehicle.ui.result

import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.data.repository.TripResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripResultReducerTest {
    private val trip =
        TripResult("t-1", 0L, 52 * 60_000L, 48.2, "판교 테크노밸리", "서울 강남구 역삼동", PurposeCategory.BUSINESS, false)

    @Test
    fun `거리·시간·경로와 기본 분류(일반 업무)`() {
        val s = reduceTripResult(trip, loaded = true, TripResultInteraction(autoCloseSec = 5))

        assertEquals("48.2", s.km)
        assertEquals(52, s.minutes)
        assertEquals(PurposeCategory.BUSINESS, s.selected)
        assertEquals(1f, s.autoCloseProgress)
    }

    @Test
    fun `사용자가 고른 분류가 저장값보다 먼저, 끝 시각이 없으면 시간 미상`() {
        val s =
            reduceTripResult(
                trip.copy(endTime = null),
                loaded = true,
                TripResultInteraction(selected = PurposeCategory.COMMUTE),
            )

        assertEquals(PurposeCategory.COMMUTE, s.selected)
        assertNull(s.minutes)
    }

    @Test
    fun `읽기 전 loading, 운행이 없으면 notFound`() {
        assertTrue(reduceTripResult(null, loaded = false, TripResultInteraction()).loading)
        assertTrue(reduceTripResult(null, loaded = true, TripResultInteraction()).notFound)
    }
}
