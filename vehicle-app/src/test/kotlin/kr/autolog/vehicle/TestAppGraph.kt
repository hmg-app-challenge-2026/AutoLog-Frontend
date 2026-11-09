package kr.autolog.vehicle

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.interim.FakeTripSession
import kr.autolog.vehicle.data.repository.DefaultContextRepository
import kr.autolog.vehicle.data.repository.DefaultProfileRepository
import kr.autolog.vehicle.data.repository.DefaultSessionRepository
import kr.autolog.vehicle.data.usage.DefaultVehicleUsageRepository
import kr.autolog.vehicle.data.usage.StaticCarUxSource
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.fake.FakeContext
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeSettingsApi
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

/** UI 테스트용 그래프. 데모 패널이 없다. */
class TestAppGraph(
    scope: CoroutineScope,
    override val clock: AppClock = AppClock.System,
    start: Route = Route.ProfilePicker,
) : AppGraph {
    val trip = FakeTripSession(scope, clock)
    val carUx = StaticCarUxSource()
    override val navigator = Navigator(start)
    override val session = DefaultSessionRepository(trip)
    override val usage = DefaultVehicleUsageRepository(trip.motion, carUx.state, scope)
    override val profile =
        DefaultProfileRepository(FakePurposeEngine(), FakeSettingsApi(), trip.currentDriver, { "veh-test" }, clock)
    override val context = DefaultContextRepository(FakeContext())
    override val demoOverlay: (@Composable () -> Unit)? = null
}
