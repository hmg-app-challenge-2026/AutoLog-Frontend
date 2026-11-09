package kr.autolog.vehicle.ui.review

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.data.interim.TripRow
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.nav.Route
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /** Home → 목적 확인(t-s1: 목적 없는 운행)으로 들어온 상태. */
    private fun TestScope.open(tripId: String = "t-s1"): Pair<TestAppGraph, ReviewViewModel> {
        val g = TestAppGraph(backgroundScope, AppClock { testScheduler.currentTime }, start = Route.Home)
        g.navigator.push(Route.ReviewTrip(tripId))
        return g to ReviewViewModel(tripId, g.review, g.navigator)
    }

    private suspend fun ReviewViewModel.loaded() = uiState.first { !it.loading }

    @Test
    fun `확신 추천 1탭이면 그 목적으로 저장하고 들어온 화면으로 돌아간다`() = runTest {
        val (g, vm) = open()
        g.sources.purpose.proposePlace = false
        assertEquals(PurposeDetail.CLIENT_VISIT, vm.loaded().suggestion)

        vm.onAction(ReviewAction.Confirm(PurposeDetail.CLIENT_VISIT))

        assertEquals(PurposeDetail.CLIENT_VISIT, g.store.trip("t-s1")!!.purpose)
        assertEquals(Route.Home, g.navigator.current)
    }

    @Test
    fun `확신이 낮으면 추천 없이 선택지`() = runTest {
        val (g, vm) = open()
        g.sources.purpose.suggestion = FakePurposeEngine.UNSURE

        val s = vm.loaded()

        assertNull(s.suggestion)
        assertTrue(s.showOptions)
    }

    @Test
    fun `말로 입력 — 듣는 중 → 알아들은 목적을 고른 상태 → 저장하면 원문이 메모로`() = runTest {
        val (g, vm) = open()
        g.sources.purpose.proposePlace = false

        vm.onAction(ReviewAction.Listen)
        assertEquals(VoiceUi.Listening, vm.loaded().voice)
        advanceTimeBy(2_000)
        val heard = vm.loaded().voice as VoiceUi.Heard
        vm.onAction(ReviewAction.SaveVoice)

        assertEquals(PurposeDetail.CLIENT_VISIT, heard.detail)
        val row = g.store.trip("t-s1")!!
        assertEquals(PurposeDetail.CLIENT_VISIT, row.purpose)
        assertEquals("거래처 미팅", row.note)
    }

    @Test
    fun `엔진이 제안하면 확정 뒤 거래처 등록 팝업(도착지 이름 프리셋), 등록하면 돌아간다`() = runTest {
        val (g, vm) = open()

        vm.onAction(ReviewAction.Confirm(PurposeDetail.CLIENT_VISIT))
        assertEquals(PlaceProposalUi("판교 테크노밸리 거래처"), vm.loaded().place)
        assertEquals(Route.ReviewTrip("t-s1"), g.navigator.current)

        vm.onAction(ReviewAction.RegisterPlace)

        assertEquals(Route.Home, g.navigator.current)
    }

    @Test
    fun `등록 이름을 비우면 등록할 수 없고, X(아니요)면 등록 없이 돌아간다`() = runTest {
        val (g, vm) = open()
        vm.onAction(ReviewAction.Confirm(PurposeDetail.CLIENT_VISIT))

        vm.onAction(ReviewAction.PlaceLabelChanged(" "))
        assertTrue(!vm.loaded().place!!.canRegister)
        vm.onAction(ReviewAction.SkipPlace)

        assertEquals(Route.Home, g.navigator.current)
    }

    @Test
    fun `나중에 — 목적은 그대로 두고 돌아간다`() = runTest {
        val (g, vm) = open()
        vm.loaded()

        vm.onAction(ReviewAction.Later)

        assertEquals(Route.Home, g.navigator.current)
        assertNull(g.store.trip("t-s1")!!.purpose)
    }

    @Test
    fun `도착지를 모르던 운행은 고른 최근 목적지가 함께 저장된다`() = runTest {
        val (g, vm) = open("t-x")
        g.sources.purpose.proposePlace = false
        g.store.upsert(TripRow("t-x", 0L, 60_000L, "u-1", "본사", null, 5.0, null))
        val candidates = vm.loaded().destinationCandidates
        assertTrue("본사" in candidates)

        vm.onAction(ReviewAction.ChooseDestination("수원 지점"))
        vm.onAction(ReviewAction.Confirm(PurposeDetail.BRANCH_MOVE))

        val row = g.store.trip("t-x")!!
        assertEquals("수원 지점", row.destinationLabel)
        assertEquals(PurposeDetail.BRANCH_MOVE, row.purpose)
    }
}
