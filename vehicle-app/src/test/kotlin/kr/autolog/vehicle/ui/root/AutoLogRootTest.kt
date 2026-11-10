package kr.autolog.vehicle.ui.root

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.core.VehicleMotion
import kr.autolog.vehicle.nav.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AutoLogRootTest {
    @get:Rule val compose = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Test
    fun `정차면 누를 수 있는 것이 하나도 없다 — 운전자 배지도 없다`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        runBlocking { graph.session.selectDriver("u-1") }
        graph.trip.setMotion(VehicleMotion.STOPPED)

        compose.setContent { AutoLogRoot(graph, onRequestBackground = {}) }
        compose.waitForIdle()

        compose.onNodeWithText("김영업님").assertDoesNotExist() // 배지는 Home "대기 중"에만(D-30)
        assertEquals(0, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w1024dp-h640dp")
    fun `기록 중 Home엔 운전자 배지가 없다 — 대기 중에만`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        runBlocking {
            graph.session.selectDriver("u-1")
            graph.trip.startSession()
        }

        compose.setContent { AutoLogRoot(graph, onRequestBackground = {}) }
        compose.mainClock.advanceTimeBy(FULL_ENTRY_GUARD_MS + 1_100) // 세션 값은 1초 스로틀

        compose.onNodeWithText("기록 중").assertIsDisplayed()
        compose.onNodeWithText("김영업님").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1024dp-h640dp")
    fun `주차 중 오른쪽 위 운전자 배지를 누르면 운전자 선택으로 간다`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        runBlocking { graph.session.selectDriver("u-1") }
        compose.setContent { AutoLogRoot(graph, onRequestBackground = {}) }
        compose.mainClock.advanceTimeBy(FULL_ENTRY_GUARD_MS + 100) // 진입 직후 오조작 방지 시간이 지난 뒤

        compose.onNodeWithText("김영업님").performClick()
        compose.waitForIdle()

        assertEquals(Route.ProfilePicker, graph.navigator.current)
    }

    @Test
    fun `주행이 시작되면 앱이 스스로 뒤로 물러난다`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        var backgrounded = false
        compose.setContent { AutoLogRoot(graph, onRequestBackground = { backgrounded = true }) }

        graph.trip.setMotion(VehicleMotion.MOVING)
        compose.waitForIdle()

        assertTrue(backgrounded)
    }

    @Test
    fun `주차에서 내려가면 열린 화면이 닫히고 Home만 남는다`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        graph.navigator.push(Route.ProfilePicker)
        compose.setContent { AutoLogRoot(graph, onRequestBackground = {}) }

        graph.trip.setMotion(VehicleMotion.STOPPED)
        compose.waitForIdle()

        assertEquals(listOf(Route.Home), graph.navigator.backStack.value)
    }
}
