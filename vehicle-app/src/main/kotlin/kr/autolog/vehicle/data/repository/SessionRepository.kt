package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kr.autolog.vehicle.common.throttleLatest
import kr.autolog.vehicle.core.DriverSource
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StartResult
import kr.autolog.vehicle.data.interim.TripSessionApi

/** 운행 세션 (스펙 6.5). 앱 A 실계약이 오면 구현만 바꾼다. */
interface SessionRepository {
    /** 1초 스로틀(스펙 6.11). */
    val state: Flow<SessionState>
    val totalOdometerKm: Flow<Double?>
    val missedTrip: Flow<MissedTrip?>
    val currentDriverId: StateFlow<String?>

    suspend fun selectDriver(userId: String?)

    suspend fun startTrip(): StartResult

    suspend fun endNow()

    suspend fun keepRecording()

    suspend fun assignDriver(tripId: String, userId: String)

    suspend fun acceptMissedTrip()

    suspend fun ignoreMissedTrip()

    suspend fun resolveRecovery(choice: RecoveryChoice)
}

class DefaultSessionRepository(private val api: TripSessionApi) : SessionRepository {
    override val state: Flow<SessionState> = api.state.throttleLatest(1_000)
    override val totalOdometerKm: Flow<Double?> = api.totalOdometerKm.throttleLatest(1_000)
    override val missedTrip: Flow<MissedTrip?> = api.missedTrip
    override val currentDriverId: StateFlow<String?> = api.currentDriver

    override suspend fun selectDriver(userId: String?) = api.selectDriver(userId)

    override suspend fun startTrip(): StartResult = api.startSession()

    override suspend fun endNow() = api.finalize()

    override suspend fun keepRecording() = api.resume()

    override suspend fun assignDriver(tripId: String, userId: String) =
        api.assignDriver(tripId, userId, DriverSource.DRIVER_SELECTED)

    override suspend fun acceptMissedTrip() = api.acceptMissedTrip()

    override suspend fun ignoreMissedTrip() = api.ignoreMissedTrip()

    override suspend fun resolveRecovery(choice: RecoveryChoice) = api.resolveRecovery(choice)
}
