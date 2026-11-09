package kr.autolog.vehicle.ui.root

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kr.autolog.vehicle.TestAppGraph
import kr.autolog.vehicle.data.interim.VehicleMotion
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
    fun `정차면 누를 수 있는 것이 하나도 없다`() {
        val graph = TestAppGraph(scope, start = Route.Home)
        graph.trip.setMotion(VehicleMotion.STOPPED)

        compose.setContent { AutoLogRoot(graph, onRequestBackground = {}) }
        compose.waitForIdle()

        assertEquals(0, compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
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
