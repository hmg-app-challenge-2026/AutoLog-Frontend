package kr.autolog.vehicle.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.VehicleMotion
import kr.autolog.vehicle.nav.Route
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.graph() = TestAppGraph(
        backgroundScope,
        clock = AppClock {
            testScheduler.currentTime
        },
        start = Route.Home,
    )

    @Test
    fun `계속 기록을 누르면 자동 종료가 취소된다`() = runTest {
        val g = graph()
        val vm = HomeViewModel(g.session, g.context, g.trips, g.receipts, g.navigator, g.clock)
        g.trip.startSession()
        g.trip.setMotion(VehicleMotion.PARKED)

        vm.onAction(HomeAction.KeepRecording)

        assertTrue(g.trip.state.value is SessionState.Recording)
    }

    @Test
    fun `기록 종료를 누르면 바로 끝난다`() = runTest {
        val g = graph()
        val vm = HomeViewModel(g.session, g.context, g.trips, g.receipts, g.navigator, g.clock)
        g.trip.startSession()
        g.trip.setMotion(VehicleMotion.PARKED)

        vm.onAction(HomeAction.EndNow)

        assertTrue(g.trip.state.value is SessionState.PendingReview)
    }

    @Test
    fun `운행이 끝나면 목적 확인 화면이 자동으로 열린다`() = runTest {
        val g = graph()
        HomeViewModel(g.session, g.context, g.trips, g.receipts, g.navigator, g.clock)
        g.trip.startSession()
        g.trip.addKm(2.0)

        g.trip.finalize()
        advanceTimeBy(1_100)

        val current = g.navigator.current
        assertTrue(current is Route.ReviewTrip)
        assertEquals(
            (g.trip.state.value as SessionState.PendingReview).trip.tripId,
            (current as Route.ReviewTrip).tripId,
        )
    }

    @Test
    fun `확인 필요 칩을 누르면 가장 최근 운행의 목적 확인이 열린다`() = runTest {
        val g = graph()
        val vm = HomeViewModel(g.session, g.context, g.trips, g.receipts, g.navigator, g.clock)

        vm.onAction(HomeAction.OpenReview)

        assertEquals(Route.ReviewTrip("t-s1"), g.navigator.current)
    }
}
