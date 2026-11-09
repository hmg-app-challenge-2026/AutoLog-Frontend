package kr.autolog.vehicle.di

import android.app.Application
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.interim.FakeTripSession
import kr.autolog.vehicle.data.repository.DefaultContextRepository
import kr.autolog.vehicle.data.repository.DefaultProfileRepository
import kr.autolog.vehicle.data.repository.DefaultSessionRepository
import kr.autolog.vehicle.data.usage.DefaultVehicleUsageRepository
import kr.autolog.vehicle.data.usage.StaticCarUxSource
import kr.autolog.vehicle.demo.DemoPanelOverlay
import kr.autolog.vehicle.demo.DemoScenario
import kr.autolog.vehicle.fake.FakeContext
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeSettingsApi
import kr.autolog.vehicle.nav.Navigator

fun createAppGraph(app: Application): AppGraph = DemoAppGraph(
    CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate,
    ),
)

/** 전부 Fake (스펙 6.7). 일반 에뮬레이터·시연 영상 백업용. 오른쪽 위를 길게 누르면 데모 패널. */
class DemoAppGraph(private val scope: CoroutineScope) : AppGraph {
    override val clock: AppClock = AppClock.System
    val trip = FakeTripSession(scope, clock)
    override val navigator = Navigator()
    override val session = DefaultSessionRepository(trip)
    override val usage = DefaultVehicleUsageRepository(trip.motion, StaticCarUxSource().state, scope)
    override val profile =
        DefaultProfileRepository(FakePurposeEngine(), FakeSettingsApi(), trip.currentDriver, { "veh-demo" }, clock)
    override val context = DefaultContextRepository(FakeContext())
    private val scenario = DemoScenario(trip, scope, clock)
    override val demoOverlay: (@Composable () -> Unit)? = { DemoPanelOverlay(scenario) }
}
