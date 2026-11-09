package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.HistoryItem
import kr.autolog.vehicle.data.repository.MonthSummary
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.progressOf
import kr.autolog.vehicle.ui.design.remainingSeconds
import java.time.Instant
import java.time.format.DateTimeFormatter

/** 스펙 7.2 상태 4가지. */
enum class HomePhase { READY, RECORDING_PARKED, AUTO_ENDING, NEEDS_RECOVERY }

data class AutoEndUi(val remainingSec: Int, val progress: Float, val reason: StopReason)

/** 대기 상태의 주인공 — 이번 달 요약(스펙 7.2, D-23). */
data class MonthUi(val tripCount: Int, val totalKm: String, val businessKm: String)

/** 최근 운행 한 줄의 날짜 구분 — 화면이 "오늘/어제/11월 7일"로 말한다. */
enum class TripDay { TODAY, YESTERDAY, EARLIER }

/** 대기 오른쪽 "최근 운행 기록"의 한 줄(D-25). [purpose]가 null이면 화면이 "목적 미입력"(회색)으로 말한다 — 고치는 곳은 운행 기록 조회(D-26). */
data class RecentTripUi(
    val tripId: String,
    val origin: String?,
    val destination: String?,
    val km: String?,
    val day: TripDay,
    val month: Int,
    val dayOfMonth: Int,
    val time: String,
    val purpose: String?,
)

data class HomeUiState(
    val phase: HomePhase = HomePhase.READY,
    val month: MonthUi? = null,
    /** 기록 중 거리. null = 계기판 미확정(추정 금지). */
    val tripKm: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val elapsedMin: Int? = null,
    val autoEnd: AutoEndUi? = null,
    /** 대기 오른쪽 "최근 운행 기록", 최신 순 최대 [RECENT_TRIPS]건. */
    val recentTrips: List<RecentTripUi> = emptyList(),
    val recoveryExtraKm: String? = null,
    /** 앱이 꺼진 동안 달린 거리(F-10). 대기 상태에서만 채워지고, 있으면 Home이 [추가하기]·X 팝업으로 한 번 묻는다(D-26). */
    val missedKm: String? = null,
)

sealed interface HomeAction {
    /** 수동 시작 — 보통은 출발하면 자동으로 시작된다(독 끝의 작은 "직접 시작"). */
    data object StartTrip : HomeAction

    data object EndNow : HomeAction

    data object KeepRecording : HomeAction

    data object AcceptMissedTrip : HomeAction

    data object IgnoreMissedTrip : HomeAction

    data class Recover(val choice: RecoveryChoice) : HomeAction

    /** 주유 기록 추가 — 영수증 QR로 들어간다(스펙 7.4). */
    data object AddFuelRecord : HomeAction

    /** 운행 기록 조회(이력 + 운행기록부 만들기·보내기, D-23). 최근 운행 줄을 눌러도 여기로 — 목적 수정도 여기서(D-26). */
    data object OpenRecords : HomeAction

    data object OpenSettings : HomeAction
}

/** "최근 운행 기록"에 보이는 최대 건수(D-25). */
const val RECENT_TRIPS = 3

internal fun reduceHome(
    state: SessionState,
    missed: MissedTrip?,
    destination: String?,
    now: Long,
    month: MonthSummary? = null,
    recent: List<HistoryItem> = emptyList(),
): HomeUiState {
    val idle = state is SessionState.Idle || state is SessionState.PendingReview
    // 놓친 운행은 운행이 없을 때만 묻는다 — 기록 중엔 지금 운행이 먼저
    val missedKm = if (idle) formatKm(missed?.gapKm) else null
    val base =
        HomeUiState(
            month = month?.let {
                MonthUi(it.tripCount, formatKm(it.totalKm).orEmpty(), formatKm(it.businessKm).orEmpty())
            },
            destination = destination,
            missedKm = missedKm,
            recentTrips = recent.take(RECENT_TRIPS).map { it.toRecentUi(now) },
        )
    return when (state) {
        SessionState.Idle, is SessionState.PendingReview -> base
        is SessionState.Recording ->
            base.copy(
                phase = HomePhase.RECORDING_PARKED,
                tripKm = formatKm(state.liveKm),
                origin = state.trip.originLabel,
                elapsedMin = ((now - state.trip.startTime) / 60_000).toInt().coerceAtLeast(0),
            )
        is SessionState.StopCandidate ->
            base.copy(
                phase = HomePhase.AUTO_ENDING,
                tripKm = formatKm(state.liveKm),
                origin = state.trip.originLabel,
                elapsedMin = ((now - state.trip.startTime) / 60_000).toInt().coerceAtLeast(0),
                autoEnd =
                AutoEndUi(
                    remainingSec = remainingSeconds(state.deadlineAt, now),
                    progress = progressOf(state.since, state.deadlineAt, now),
                    reason = state.reason,
                ),
            )
        is SessionState.NeedsRecovery ->
            base.copy(phase = HomePhase.NEEDS_RECOVERY, recoveryExtraKm = formatKm(state.extraKm))
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** [AppZone] 기준으로 오늘·어제·그 전을 가른다. 문구는 화면이 strings.xml로 만든다. */
private fun HistoryItem.toRecentUi(now: Long): RecentTripUi {
    val start = Instant.ofEpochMilli(startTime).atZone(AppZone)
    val today = Instant.ofEpochMilli(now).atZone(AppZone).toLocalDate()
    val day =
        when (start.toLocalDate()) {
            today -> TripDay.TODAY
            today.minusDays(1) -> TripDay.YESTERDAY
            else -> TripDay.EARLIER
        }
    return RecentTripUi(
        tripId = tripId,
        origin = origin,
        destination = destination,
        km = formatKm(distanceKm),
        day = day,
        month = start.monthValue,
        dayOfMonth = start.dayOfMonth,
        time = start.format(TIME),
        purpose = purpose?.labelKo,
    )
}
