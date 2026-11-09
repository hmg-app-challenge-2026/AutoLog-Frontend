package kr.autolog.vehicle.di

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.interim.FakeDeviceApi
import kr.autolog.vehicle.data.interim.FakeLocalStore
import kr.autolog.vehicle.data.interim.FakeReportApi
import kr.autolog.vehicle.data.interim.FakeTripSession
import kr.autolog.vehicle.data.interim.FakeUploadApi
import kr.autolog.vehicle.data.repository.DefaultContextRepository
import kr.autolog.vehicle.data.repository.DefaultProfileRepository
import kr.autolog.vehicle.data.repository.DefaultReceiptRepository
import kr.autolog.vehicle.data.repository.DefaultReportRepository
import kr.autolog.vehicle.data.repository.DefaultSessionRepository
import kr.autolog.vehicle.data.repository.DefaultSettingsRepository
import kr.autolog.vehicle.data.repository.DefaultTripRepository
import kr.autolog.vehicle.data.repository.DefaultTripResultRepository
import kr.autolog.vehicle.data.usage.CarUxSource
import kr.autolog.vehicle.data.usage.DefaultVehicleUsageRepository
import kr.autolog.vehicle.fake.FakeContext
import kr.autolog.vehicle.fake.FakeMailApi
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeReceiptApi
import kr.autolog.vehicle.fake.FakeSettingsApi
import kr.autolog.vehicle.fake.FakeVoiceApi
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

/** 그래프가 쓰는 가짜 원천들. 데모 패널·테스트가 직접 조작할 수 있게 노출한다. */
class FakeSources(scope: CoroutineScope, clock: AppClock) {
    val store = FakeLocalStore(clock)
    val trip = FakeTripSession(scope, clock, store = store)
    val purpose = FakePurposeEngine()
    val settingsApi = FakeSettingsApi()
    val context = FakeContext()
    val voice = FakeVoiceApi()
    val receiptApi = FakeReceiptApi()
    val mail = FakeMailApi()
    val report = FakeReportApi(store, clock)
    val device = FakeDeviceApi(clock)
    val upload = FakeUploadApi(clock)
}

/**
 * 앱 A 계약은 interim Fake, 앱 B 계약은 Backend `fake/`로 조립한 그래프(스펙 6.7).
 * demo flavor·vehicle flavor(R-1 전까지)·UI 테스트가 함께 쓴다. 다른 것은 [carUx]와 [overlay]뿐.
 */
open class FakeAppGraph(
    scope: CoroutineScope,
    override val clock: AppClock,
    carUx: CarUxSource,
    start: Route = Route.ProfilePicker,
    vehicleId: String = "veh-demo",
    overlay: ((FakeSources) -> @Composable () -> Unit)? = null,
) : AppGraph {
    val sources = FakeSources(scope, clock)
    override val navigator = Navigator(start)
    override val session = DefaultSessionRepository(sources.trip)
    override val usage = DefaultVehicleUsageRepository(sources.trip.motion, carUx.state, scope)
    override val profile =
        DefaultProfileRepository(sources.purpose, sources.settingsApi, sources.trip.currentDriver, { vehicleId }, clock)
    override val context = DefaultContextRepository(sources.context)
    override val trips = DefaultTripRepository(sources.store, sources.settingsApi)
    override val tripResults = DefaultTripResultRepository(sources.store, sources.trip, sources.purpose) { vehicleId }
    override val receipts =
        DefaultReceiptRepository(sources.receiptApi, sources.store, sources.voice, clock) {
            sources.store.ackReceipt(it)
        }
    override val reports = DefaultReportRepository(sources.report, sources.mail, sources.settingsApi)
    override val settings =
        DefaultSettingsRepository(sources.device, sources.upload, sources.settingsApi, sources.mail, scope)
    override val demoOverlay: (@Composable () -> Unit)? = overlay?.invoke(sources)
}
