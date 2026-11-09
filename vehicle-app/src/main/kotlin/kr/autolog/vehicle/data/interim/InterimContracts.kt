package kr.autolog.vehicle.data.interim

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kr.autolog.shared.ReceiptPayload
import kr.autolog.vehicle.core.DriverSource
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.core.ReportFile
import kr.autolog.vehicle.core.Suggestion
import java.time.YearMonth

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
    /** 출발지 이름(등록 장소 이름, 없으면 동 단위 주소). 모르면 null — 앱 A 요청 R-1. */
    val originLabel: String? = null,
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

    /**
     * Idle **또는 PendingReview**에서 시작할 수 있다. 목적 확인을 미룬("나중에") 운행은 저장소에 "목적 확인 필요"로 남고
     * 새 운행이 시작된다 — 목적을 안 골랐다고 다음 운행이 기록되지 않으면 안 된다(스펙 6.6, R-2).
     */
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

// ===== 앱 A 계약 초안 2 (스펙 6.6): 로컬 저장소·운행기록부·기기 등록·사내 서버 전송 =====

enum class UploadState { PENDING, DONE, FAILED }

/**
 * 운행 기록 한 줄. 비어 있는 값은 "미확정"이다 — [purpose] null = 목적 확인 필요,
 * [driverUserId] null = 운전자 미확정, [distanceKm] null = 거리 미확정(추정 금지).
 * [upload] null = 사내 서버 전송을 쓰지 않음.
 */
data class TripRow(
    val tripId: String,
    val startTime: Long,
    val endTime: Long?,
    val driverUserId: String?,
    val originLabel: String?,
    val destinationLabel: String?,
    val distanceKm: Double?,
    val purpose: PurposeDetail?,
    val note: String? = null,
    val upload: UploadState? = null,
)

interface LocalStore {
    /** [month](Asia/Seoul 기준)에 시작한 운행, 최신 순. */
    fun trips(month: YearMonth): Flow<List<TripRow>>

    /** 목적 확인이 필요한 운행(purpose == null), 최신 순. */
    val pendingReviews: Flow<List<TripRow>>

    /** 달과 상관없이 가장 최근 운행 [limit]건, 최신 순(Home "최근 운행 기록"). */
    fun recent(limit: Int): Flow<List<TripRow>>

    /** inbox로 받은 영수증 OCR 결과 중 아직 확정하지 않은 것. */
    val receiptInbox: Flow<List<ReceiptPayload>>

    suspend fun trip(tripId: String): TripRow?
}

sealed interface ReportBuildResult {
    data class Built(val file: ReportFile) : ReportBuildResult

    data class Failed(val reason: String) : ReportBuildResult
}

interface ReportApi {
    suspend fun build(month: YearMonth): ReportBuildResult
}

sealed interface ConnectionState {
    data object NotRegistered : ConnectionState

    data class Registered(val vehicleLabel: String, val serverUrl: String, val lastSyncAt: Long?) : ConnectionState

    data class Unreachable(val vehicleLabel: String, val serverUrl: String) : ConnectionState
}

sealed interface RegisterResult {
    data object Ok : RegisterResult

    data class Invalid(val reason: String) : RegisterResult
}

/** 사내 서버(자체 호스팅 relay) 등록. 서버 주소는 런타임 설정(스펙 8.3, R-4). */
interface DeviceApi {
    val connection: StateFlow<ConnectionState>

    suspend fun register(serverUrl: String, code: String): RegisterResult
}

data class UploadStatus(val enabled: Boolean, val pendingCount: Int, val lastUploadAt: Long?, val lastError: String?)

sealed interface UploadResult {
    data class Uploaded(val count: Int) : UploadResult

    data class Failed(val reason: String) : UploadResult
}

/** 사내 서버 자동 취합용 운행 사본 전송 (기능명세 ②, 스펙 8장, R-4). */
interface UploadApi {
    val status: StateFlow<UploadStatus>

    suspend fun uploadNow(): UploadResult
}
