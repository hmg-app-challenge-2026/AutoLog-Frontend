package kr.autolog.vehicle.di

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.data.usage.AndroidCarUxSource

/**
 * 실차·Pleos 에뮬레이터용. 지금은 CarUxRestrictions만 실제이고 나머지는 Fake다.
 * 스펙 R-1(`AutoLog.create(context)`)·R-2(앱 A 계약)가 머지되면 여기서 실구현으로 바꾼다.
 */
fun createAppGraph(app: Application): AppGraph {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val carUx = AndroidCarUxSource(app).also { it.start() }
    return FakeAppGraph(scope, AppClock.System, carUx, vehicleId = "veh-1")
}
