package kr.autolog.vehicle.data.interim

import kotlinx.coroutines.flow.StateFlow
import kr.autolog.vehicle.core.DriverSource
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.core.Suggestion

// 앱 A 계약 초안 (스펙 6.6, R-2·R-7). 앱 A의 TripSessionApi가 core에 머지되면 이 파일을 지우고
// Repository 구현만 고친다. ui/ 패키지는 이 파일을 import하지 않는다.

/** 기어·움직임 (스펙 1.1.1 판정 결과). null = 판정 불가 → BACKGROUND. */
enum class VehicleMotion { MOVING, STOPPED, PARKED }

enum class StopReason { GEAR_P, NO_MOVEMENT, NAVI_ARRIVED, USER_REQUEST }

enum class InputMode { TAP, VOICE }

enum class RecoveryChoice { RECORD, CONTINUE, DELETE }

data class LiveTrip(
    val tripId: String,
    val startTime: Long,
    val startOdometerKm: Double?,
    val destinationLabel: String?,
    val driverUserId: String?,
)

sealed interface SessionState {
    data object Idle : SessionState

    /** [liveKm] null = 계기판을 못 읽음(거리 미확정, 추정 금지). */
    data class Recording(val trip: LiveTrip, val liveKm: Double?, val lastSavedAt: Long) : SessionState

    /** 자동 종료 대기. [deadlineAt]이 유일한 시계다 — 화면은 남은 시간만 계산한다. */
    data class StopCandidate(
        val trip: LiveTrip,
        val liveKm: Double?,
        val reason: StopReason,
        val since: Long,
        val deadlineAt: Long,
    ) : SessionState

    data class PendingReview(val trip: LiveTrip, val distanceKm: Double?, val suggestion: Suggestion?) : SessionState

    data class NeedsRecovery(val trip: LiveTrip, val extraKm: Double?) : SessionState
}

data class MissedTrip(val gapKm: Double, val lastKnownAt: Long)

sealed interface StartResult {
    data class Started(val tripId: String) : StartResult

    data class Failed(val reason: String) : StartResult
}

interface TripSessionApi {
    val state: StateFlow<SessionState>
    val motion: StateFlow<VehicleMotion?>
    val totalOdometerKm: StateFlow<Double?>
    val missedTrip: StateFlow<MissedTrip?>
    val currentDriver: StateFlow<String?>

    suspend fun selectDriver(userId: String?)

    suspend fun startSession(): StartResult

    /** [기록 종료] — StopCandidate에서도 즉시 종료. */
    suspend fun finalize()

    /** [계속 기록] — StopCandidate → Recording. */
    suspend fun resume()

    suspend fun confirmPurpose(tripId: String, detail: PurposeDetail, note: String?, inputMode: InputMode)

    suspend fun assignDriver(tripId: String, userId: String, source: DriverSource)

    suspend fun acceptMissedTrip()

    suspend fun ignoreMissedTrip()

    suspend fun resolveRecovery(choice: RecoveryChoice)
}
