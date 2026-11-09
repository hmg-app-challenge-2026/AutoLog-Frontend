package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.data.repository.ActiveTrip
import kr.autolog.vehicle.data.repository.HistoryItem
import kr.autolog.vehicle.data.repository.MonthSummary
import kr.autolog.vehicle.data.repository.TripSession
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.progressOf
import kr.autolog.vehicle.ui.design.remainingSeconds
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Home 상태 계산(순수 함수, HomeReducerTest 대상). 입력은 Repository 값 타입만 — 앱 A 계약이 바뀌어도 이 파일은 그대로다.
 * [destination]은 내비 목적지 이름, [now]는 경과 분·남은 초 계산용.
 */
internal fun reduceHome(
    session: TripSession,
    missedKm: Double?,
    destination: String?,
    now: Long,
    month: MonthSummary? = null,
    recent: List<HistoryItem> = emptyList(),
): HomeUiState {
    val idle = session is TripSession.Idle || session is TripSession.PendingReview
    // 놓친 운행은 운행이 없을 때만 묻는다 — 기록 중엔 지금 운행이 먼저
    val base =
        HomeUiState(
            month = month?.let {
                MonthUi(it.tripCount, formatKm(it.totalKm).orEmpty(), formatKm(it.businessKm).orEmpty())
            },
            destination = destination,
            missedKm = if (idle) formatKm(missedKm) else null,
            recentTrips = recent.take(RECENT_TRIPS).map { it.toRecentUi(now) },
        )
    return when (session) {
        TripSession.Idle, is TripSession.PendingReview -> base
        is TripSession.Recording -> base.withTrip(HomePhase.RECORDING_PARKED, session.trip, session.liveKm, now)
        is TripSession.AutoEnding ->
            base.withTrip(HomePhase.AUTO_ENDING, session.trip, session.liveKm, now).copy(
                autoEnd =
                AutoEndUi(
                    remainingSec = remainingSeconds(session.deadlineAt, now),
                    progress = progressOf(session.since, session.deadlineAt, now),
                ),
            )
        is TripSession.NeedsRecovery ->
            base.copy(phase = HomePhase.NEEDS_RECOVERY, recoveryExtraKm = formatKm(session.extraKm))
    }
}

/** 기록 중·자동 종료 대기가 같이 쓰는 값: 거리 · 출발지 · 경과 분. */
private fun HomeUiState.withTrip(phase: HomePhase, trip: ActiveTrip, liveKm: Double?, now: Long) = copy(
    phase = phase,
    tripKm = formatKm(liveKm),
    origin = trip.origin,
    elapsedMin = ((now - trip.startTime) / 60_000).toInt().coerceAtLeast(0),
)

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
