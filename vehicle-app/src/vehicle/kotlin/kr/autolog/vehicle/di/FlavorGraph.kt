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
import kr.autolog.vehicle.data.usage.AndroidCarUxSource
import kr.autolog.vehicle.data.usage.DefaultVehicleUsageRepository
import kr.autolog.vehicle.fake.FakeContext
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeSettingsApi
import kr.autolog.vehicle.nav.Navigator

/**
 * 실차·Pleos 에뮬레이터용. 지금은 CarUxRestrictions만 실제이고 나머지는 Fake다.
 * 스펙 R-1(`AutoLog.create(context)`)·R-2(앱 A 계약)가 머지되면 여기서 실구현으로 바꾼다.
 */
fun createAppGraph(app: Application): AppGraph {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val carUx = AndroidCarUxSource(app).also { it.start() }
    val appClock = AppClock.System
    val trip = FakeTripSession(scope, appClock)
    return object : AppGraph {
        override val clock = appClock
        override val navigator = Navigator()
        override val session = DefaultSessionRepository(trip)
        override val usage = DefaultVehicleUsageRepository(trip.motion, carUx.state, scope)
        override val profile = DefaultProfileRepository(FakePurposeEngine(), FakeSettingsApi(), trip.currentDriver, { "veh-1" }, appClock)
        override val context = DefaultContextRepository(FakeContext())
        override val demoOverlay: (@Composable () -> Unit)? = null
    }
}
