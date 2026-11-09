package kr.autolog.vehicle.ui.profile

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.nav.Route
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilePickerViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `타일 1탭으로 운전자를 정하고 Home으로 간다`() =
        runTest {
            val graph = TestAppGraph(backgroundScope)
            val vm = ProfilePickerViewModel(graph.session, graph.profile, graph.navigator)

            vm.onAction(ProfilePickerAction.Select("u-2"))

            assertEquals("u-2", graph.trip.currentDriver.value)
            assertEquals(listOf(Route.Home), graph.navigator.backStack.value)
        }

    @Test
    fun `나중에 선택하면 운전자 없이 Home으로 간다`() =
        runTest {
            val graph = TestAppGraph(backgroundScope)
            val vm = ProfilePickerViewModel(graph.session, graph.profile, graph.navigator)

            vm.onAction(ProfilePickerAction.Later)

            assertNull(graph.trip.currentDriver.value)
            assertEquals(Route.Home, graph.navigator.current)
        }

    @Test
    fun `배차 운전자가 첫 자리이고 미확정 운행 카드가 뜬다`() =
        runTest {
            val graph = TestAppGraph(backgroundScope)
            val vm = ProfilePickerViewModel(graph.session, graph.profile, graph.navigator)

            vm.uiState.test {
                val loaded = expectMostRecentItem().takeIf { it.loaded } ?: awaitItem()
                assertEquals("u-1", loaded.visible.first().userId)
                assertEquals("t-9", loaded.unassigned?.tripId)
            }
        }

    @Test
    fun `이전 운행 운전자를 고르면 카드가 사라진다`() =
        runTest {
            val graph = TestAppGraph(backgroundScope)
            val vm = ProfilePickerViewModel(graph.session, graph.profile, graph.navigator)

            vm.uiState.test {
                skipItems(1)
                vm.onAction(ProfilePickerAction.AssignPrevious("t-9", "u-1"))
                assertNull(expectMostRecentItem().unassigned)
            }
        }
}
