package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.HistoryItem
import kr.autolog.vehicle.data.repository.MonthSummary
import kr.autolog.vehicle.data.repository.UploadInfo
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

/** 대기 오른쪽 "최근 운행 기록"의 한 줄(D-25). [needsReview]면 목적이 아직 없다 — 누르면 목적 확인으로. */
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
    val needsReview: Boolean,
)

/**
 * 알림 한 건(D-23·D-25). **있을 때만** 상태 줄 아래 알림 띠에 나타난다. 순서가 곧 화면 순서다 —
 * 기록 자체의 문제(데이터를 잃을 수 있음) → 사용자가 처리할 일.
 * 새 종류는 여기에 하나 추가하고 [noticeSpec]에서 모양만 정한다.
 */
sealed interface HomeNotice {
    /** 기록 중인데 계기판 거리를 읽지 못한다(거리 미확정, 추정하지 않음). */
    data object OdometerUnavailable : HomeNotice

    /** 기록 중 마지막 저장이 [SAVE_DELAY_MS]보다 오래됐다. */
    data object SaveDelayed : HomeNotice

    /** 사내 서버 전송이 실패해 [pending]건이 남아 있다(전송이 켜진 경우만). */
    data class UploadFailed(val pending: Int) : HomeNotice

    data class Review(val count: Int) : HomeNotice

    data class Receipt(val count: Int) : HomeNotice

    data class MissedTrip(val km: String) : HomeNotice
}

data class HomeUiState(
    val phase: HomePhase = HomePhase.READY,
    val month: MonthUi? = null,
    /** 기록 중 거리. null = 계기판 미확정(추정 금지). */
    val tripKm: String? = null,
    val origin: String? = null,
    val destination: String? = null,
    val elapsedMin: Int? = null,
    val autoEnd: AutoEndUi? = null,
    val notices: List<HomeNotice> = emptyList(),
    /** 대기 오른쪽 "최근 운행 기록", 최신 순 최대 [RECENT_TRIPS]건. */
    val recentTrips: List<RecentTripUi> = emptyList(),
    val recoveryExtraKm: String? = null,
)

sealed interface HomeAction {
    /** 수동 시작 — 보통은 출발하면 자동으로 시작된다(독 끝의 작은 "직접 시작"). */
    data object StartTrip : HomeAction

    data object EndNow : HomeAction

    data object KeepRecording : HomeAction

    data object AcceptMissedTrip : HomeAction

    data object IgnoreMissedTrip : HomeAction

    data class Recover(val choice: RecoveryChoice) : HomeAction

    /** 알림 "운행 N건 목적 확인" — 가장 최근 운행. */
    data object OpenReview : HomeAction

    /** 알림 "영수증 N장 도착" — 가장 최근 영수증의 OCR 값이 채워진 주유 입력 창(스펙 7.5). */
    data object OpenReceipt : HomeAction

    /** 주유 기록 추가 — 영수증 QR로 들어간다(스펙 7.4). */
    data object AddFuelRecord : HomeAction

    /** 운행 기록(이력 + 운행기록부 만들기·보내기, D-23). */
    data object OpenRecords : HomeAction

    /** 최근 운행 한 줄 — 목적이 없으면 그 운행의 목적 확인, 있으면 운행 기록. */
    data class OpenTrip(val tripId: String, val needsReview: Boolean) : HomeAction

    data object OpenSettings : HomeAction
}

/** "최근 운행 기록"에 보이는 최대 건수(D-25). */
const val RECENT_TRIPS = 3

/** 기록 중 마지막 저장이 이보다 오래되면 "저장이 늦어지고 있어요"(앱 A는 10초마다 저장). */
const val SAVE_DELAY_MS = 30_000L

internal fun reduceHome(
    state: SessionState,
    missed: MissedTrip?,
    destination: String?,
    now: Long,
    pendingReviewIds: List<String> = emptyList(),
    receiptIds: List<String> = emptyList(),
    month: MonthSummary? = null,
    recent: List<HistoryItem> = emptyList(),
    upload: UploadInfo? = null,
): HomeUiState {
    val idle = state is SessionState.Idle || state is SessionState.PendingReview
    val liveKm =
        when (state) {
            is SessionState.Recording -> state.liveKm
            is SessionState.StopCandidate -> state.liveKm
            else -> null
        }
    val recording = state is SessionState.Recording || state is SessionState.StopCandidate
    val notices =
        buildList {
            if (recording && liveKm == null) add(HomeNotice.OdometerUnavailable)
            if (state is SessionState.Recording && now - state.lastSavedAt > SAVE_DELAY_MS) add(HomeNotice.SaveDelayed)
            if (upload != null &&
                upload.enabled &&
                upload.lastError != null
            ) {
                add(HomeNotice.UploadFailed(upload.pendingCount))
            }
            if (pendingReviewIds.isNotEmpty()) add(HomeNotice.Review(pendingReviewIds.size))
            if (receiptIds.isNotEmpty()) add(HomeNotice.Receipt(receiptIds.size))
            // 놓친 운행은 운행이 없을 때만 묻는다 — 기록 중엔 지금 운행이 먼저
            if (idle) formatKm(missed?.gapKm)?.let { add(HomeNotice.MissedTrip(it)) }
        }
    val base =
        HomeUiState(
            month = month?.let {
                MonthUi(it.tripCount, formatKm(it.totalKm).orEmpty(), formatKm(it.businessKm).orEmpty())
            },
            destination = destination,
            notices = notices,
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
        needsReview = purpose == null,
    )
}
