package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.ConfirmResult
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.data.interim.containsEpoch
import kr.autolog.vehicle.fake.FakeReceiptApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class ScreenRepositoriesTest {
    private val now = LocalDateTime.of(2026, 11, 20, 10, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
    private val month = YearMonth.of(2026, 11)

    @Test
    fun `이력은 운전자 이름을 붙이고 운전자 미확정을 표시한다`() = runTest {
        val g = TestAppGraph(backgroundScope, AppClock { now })

        val items = g.trips.history(month).first()

        assertEquals("김영업", items.first { it.tripId == "t-s1" }.driverName)
        assertTrue(items.first { it.tripId == "t-s4" }.driverUnset)
        assertEquals(UploadMark.FAILED, items.first { it.tripId == "t-s5" }.upload)
        assertTrue(items.all { month.containsEpoch(it.startTime) })
    }

    @Test
    fun `월 요약은 업무 거리만 따로 더한다`() = runTest {
        val g = TestAppGraph(backgroundScope, AppClock { now })

        val s = g.trips.summary(month).first()

        assertEquals(6, s.tripCount)
        assertEquals(41.2 + 8.6 + 40.9, s.businessKm, 0.001) // 지점 이동·거래처 방문·지점 이동 (외근은 거리 미확정)
        assertEquals(3, s.reviewCount)
    }

    @Test
    fun `목적 확인은 추천을 함께 주고 확정하면 운행 기록에 목적이 들어간다`() = runTest {
        val g = TestAppGraph(backgroundScope, AppClock { now })

        val review = g.review.review("t-s1").first()!!
        g.review.confirm("t-s1", PurposeDetail.CLIENT_VISIT, null, byVoice = false)

        assertEquals(PurposeDetail.CLIENT_VISIT, review.suggestion?.detail)
        assertEquals(PurposeDetail.CLIENT_VISIT, g.trips.recent(10).first().single { it.tripId == "t-s1" }.purpose)
    }

    @Test
    fun `영수증을 확정하면 inbox에서 빠진다`() = runTest {
        val g = TestAppGraph(backgroundScope, AppClock { now })
        val receipt = FakeReceiptApi.SAMPLE_RECEIPT

        val result = g.receipts.confirm(receipt, "t-1", receipt.fields)

        assertTrue(result is ConfirmResult.Saved)
        assertNull(g.receipts.receipt(receipt.receiptId).first())
    }

    @Test
    fun `잘못된 연결 코드는 이유와 함께 실패한다`() = runTest {
        val g = TestAppGraph(backgroundScope, AppClock { now })

        val result = g.settings.register("https://autolog.acme.co.kr", "12")

        assertEquals(Outcome.Failed("연결 코드는 숫자 6자리예요"), result)
    }
}
