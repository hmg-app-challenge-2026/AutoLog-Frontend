package kr.autolog.vehicle.ui.result

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
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.nav.Route
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripResultViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    /** Home → 운행 결과(t-s1: 목적 없는 운행)로 들어온 상태. */
    private fun TestScope.open(tripId: String = "t-s1"): Pair<TestAppGraph, TripResultViewModel> {
        val g = TestAppGraph(backgroundScope, AppClock { testScheduler.currentTime }, start = Route.Home)
        g.navigator.push(Route.TripResult(tripId))
        return g to TripResultViewModel(tripId, g.tripResults, g.navigator)
    }

    private suspend fun TestScope.purposeOf(g: TestAppGraph, tripId: String = "t-s1") = g.store.trip(tripId)!!.purpose

    @Test
    fun `아무것도 안 누르면 5초 뒤 일반 업무로 저장하고 메인 화면으로`() = runTest {
        val (g, vm) = open()
        assertEquals(5, vm.uiState.first { !it.loading }.autoCloseSec)

        advanceTimeBy(4_900)
        assertEquals(Route.TripResult("t-s1"), g.navigator.current)
        assertNull(purposeOf(g))
        advanceTimeBy(200)

        assertEquals(Route.Home, g.navigator.current)
        assertEquals(PurposeCategory.BUSINESS, purposeOf(g)!!.category)
    }

    @Test
    fun `수정하기 — 카운트다운이 멈추고 목적을 바꿔 저장하면 다시 카운트다운`() = runTest {
        val (g, vm) = open()
        vm.uiState.first { !it.loading }

        vm.onAction(TripResultAction.Edit)
        advanceTimeBy(10_000)
        assertEquals(Route.TripResult("t-s1"), g.navigator.current) // 멈춰 있다
        vm.onAction(TripResultAction.Select(PurposeCategory.NON_BUSINESS))
        vm.onAction(TripResultAction.Save)

        assertEquals(PurposeCategory.NON_BUSINESS, purposeOf(g)!!.category)
        assertEquals(5, vm.uiState.first { !it.editing && !it.saving }.autoCloseSec)
        advanceTimeBy(5_100)
        assertEquals(Route.Home, g.navigator.current)
        assertEquals(PurposeCategory.NON_BUSINESS, purposeOf(g)!!.category)
    }

    @Test
    fun `보기 모드에선 목적을 못 바꾸고, 메인 화면으로는 바로 저장하고 돌아간다`() = runTest {
        val (g, vm) = open()
        vm.uiState.first { !it.loading }

        vm.onAction(TripResultAction.Select(PurposeCategory.COMMUTE))
        assertEquals(PurposeCategory.BUSINESS, vm.uiState.first().selected)
        vm.onAction(TripResultAction.GoHome)

        assertEquals(Route.Home, g.navigator.current)
        assertEquals(PurposeCategory.BUSINESS, purposeOf(g)!!.category)
    }

    @Test
    fun `수정 중 메인 화면으로 — 고친 목적으로 저장된다`() = runTest {
        val (g, vm) = open()
        vm.uiState.first { !it.loading }

        vm.onAction(TripResultAction.Edit)
        vm.onAction(TripResultAction.Select(PurposeCategory.COMMUTE))
        vm.onAction(TripResultAction.GoHome)

        assertEquals(PurposeCategory.COMMUTE, purposeOf(g)!!.category)
        assertTrue(g.navigator.current == Route.Home)
    }
}
