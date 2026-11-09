package kr.autolog.vehicle.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.repository.ContextRepository
import kr.autolog.vehicle.data.repository.ProfileRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.usage.VehicleUsageRepository
import kr.autolog.vehicle.nav.Navigator

/** 수동 DI (스펙 D-05·6.7). flavor마다 createAppGraph()가 조립한다. */
interface AppGraph {
    val clock: AppClock
    val navigator: Navigator
    val session: SessionRepository
    val usage: VehicleUsageRepository
    val profile: ProfileRepository
    val context: ContextRepository

    /** demo flavor 데모 패널. vehicle flavor는 null. */
    val demoOverlay: (@Composable () -> Unit)?
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> { error("AppGraph가 제공되지 않았습니다") }
