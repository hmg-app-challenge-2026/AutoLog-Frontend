package kr.autolog.vehicle.data.interim

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.DriverSource
import kr.autolog.vehicle.core.PurposeDetail

/** 기어 P 자동 종료 대기 (사용자 결정 2026-11-09, 스펙 D-18). 실제 값은 앱 A 소유. */
const val GEAR_P_WAIT_MS = 5_000L

/**
 * 앱 A [TripSessionApi]가 올 때까지 쓰는 가짜(스펙 6.6). 데모 패널과 테스트가 setMotion/addKm로 차량을 흉내 낸다.
 * 규칙: 운행 중 PARKED → GEAR_P 카운트다운 / 카운트다운 중 MOVING → 계속 / 운행 없을 때 MOVING → 자동 시작(F-02).
 */
class FakeTripSession(
    private val scope: CoroutineScope,
    private val clock: AppClock,
    initialOdometerKm: Double? = 18_420.5,
    private val gearPWaitMs: Long = GEAR_P_WAIT_MS,
) : TripSessionApi {
    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    private val _motion = MutableStateFlow<VehicleMotion?>(VehicleMotion.PARKED)
    private val _odometer = MutableStateFlow(initialOdometerKm)
    private val _missed = MutableStateFlow<MissedTrip?>(null)
    private val _driver = MutableStateFlow<String?>(null)
    private var autoEndJob: Job? = null

    override val state: StateFlow<SessionState> = _state.asStateFlow()
    override val motion: StateFlow<VehicleMotion?> = _motion.asStateFlow()
    override val totalOdometerKm: StateFlow<Double?> = _odometer.asStateFlow()
    override val missedTrip: StateFlow<MissedTrip?> = _missed.asStateFlow()
    override val currentDriver: StateFlow<String?> = _driver.asStateFlow()

    override suspend fun selectDriver(userId: String?) {
        _driver.value = userId
    }

    override suspend fun startSession(): StartResult {
        if (_state.value !is SessionState.Idle) return StartResult.Failed("이미 기록 중입니다")
        val now = clock.nowMs()
        val trip = LiveTrip("trip-$now", now, _odometer.value, "판교 테크노밸리 거래처", _driver.value)
        _state.value = SessionState.Recording(trip, liveKm = if (_odometer.value == null) null else 0.0, lastSavedAt = now)
        return StartResult.Started(trip.tripId)
    }

    override suspend fun finalize() {
        autoEndJob?.cancel()
        finalizeNow()
    }

    override suspend fun resume() {
        val s = _state.value as? SessionState.StopCandidate ?: return
        autoEndJob?.cancel()
        _state.value = SessionState.Recording(s.trip, s.liveKm, clock.nowMs())
    }

    override suspend fun confirmPurpose(tripId: String, detail: PurposeDetail, note: String?, inputMode: InputMode) {
        val s = _state.value as? SessionState.PendingReview ?: return
        if (s.trip.tripId == tripId) _state.value = SessionState.Idle
    }

    override suspend fun assignDriver(tripId: String, userId: String, source: DriverSource) = Unit

    override suspend fun acceptMissedTrip() {
        _missed.value = null
    }

    override suspend fun ignoreMissedTrip() {
        _missed.value = null
    }

    override suspend fun resolveRecovery(choice: RecoveryChoice) {
        val s = _state.value as? SessionState.NeedsRecovery ?: return
        _state.value =
            when (choice) {
                RecoveryChoice.RECORD -> SessionState.PendingReview(s.trip, s.extraKm, null)
                RecoveryChoice.CONTINUE -> SessionState.Recording(s.trip, s.extraKm, clock.nowMs())
                RecoveryChoice.DELETE -> SessionState.Idle
            }
    }

    // ---- 데모·테스트 조작 ----

    fun setMotion(motion: VehicleMotion?) {
        _motion.value = motion
        val s = _state.value
        when {
            motion == VehicleMotion.PARKED && s is SessionState.Recording -> startAutoEnd(s, StopReason.GEAR_P)
            motion == VehicleMotion.MOVING && s is SessionState.StopCandidate -> {
                autoEndJob?.cancel()
                _state.value = SessionState.Recording(s.trip, s.liveKm, clock.nowMs())
            }
            motion == VehicleMotion.MOVING && s is SessionState.Idle -> scope.launch { startSession() }
            else -> Unit
        }
    }

    fun addKm(km: Double) {
        _odometer.value = _odometer.value?.plus(km)
        val s = _state.value
        if (s is SessionState.Recording) {
            _state.value = s.copy(liveKm = s.liveKm?.plus(km), lastSavedAt = clock.nowMs())
        }
    }

    fun setMissedTrip(missed: MissedTrip?) {
        _missed.value = missed
    }

    fun enterRecovery(extraKm: Double?) {
        val now = clock.nowMs()
        val trip = LiveTrip("trip-rec-$now", now - 3_600_000, _odometer.value, null, _driver.value)
        _state.value = SessionState.NeedsRecovery(trip, extraKm)
    }

    private fun startAutoEnd(s: SessionState.Recording, reason: StopReason) {
        val now = clock.nowMs()
        _state.value = SessionState.StopCandidate(s.trip, s.liveKm, reason, since = now, deadlineAt = now + gearPWaitMs)
        autoEndJob?.cancel()
        autoEndJob =
            scope.launch {
                delay(gearPWaitMs)
                if (_state.value is SessionState.StopCandidate) finalizeNow()
            }
    }

    private fun finalizeNow() {
        val (trip, km) =
            when (val s = _state.value) {
                is SessionState.Recording -> s.trip to s.liveKm
                is SessionState.StopCandidate -> s.trip to s.liveKm
                else -> return
            }
        _state.value = SessionState.PendingReview(trip, km, null)
    }
}
