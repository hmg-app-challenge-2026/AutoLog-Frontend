package kr.autolog.vehicle.ui.home

import kr.autolog.vehicle.data.interim.MissedTrip
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.SessionState
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.MonthSummary
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.progressOf
import kr.autolog.vehicle.ui.design.remainingSeconds

/** 스펙 7.2 상태 4가지. */
enum class HomePhase { READY, RECORDING_PARKED, AUTO_ENDING, NEEDS_RECOVERY }

data class AutoEndUi(val remainingSec: Int, val progress: Float, val reason: StopReason)

/** 대기 상태의 주인공 — 이번 달 요약(스펙 7.2, D-23). */
data class MonthUi(val tripCount: Int, val totalKm: String, val businessKm: String)

/**
 * 알림 목록의 한 줄(D-23). 순서가 곧 화면 순서다 — 목적 확인 → 영수증 → 놓친 운행.
 * 새 종류의 처리할 일은 여기에 하나 추가하고 [HomeNotices]에서 그 모양만 정한다.
 */
sealed interface HomeNotice {
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

    data object OpenSettings : HomeAction
}

internal fun reduceHome(
    state: SessionState,
    missed: MissedTrip?,
    destination: String?,
    now: Long,
    pendingReviewIds: List<String> = emptyList(),
    receiptIds: List<String> = emptyList(),
    month: MonthSummary? = null,
): HomeUiState {
    val idle = state is SessionState.Idle || state is SessionState.PendingReview
    val notices =
        buildList {
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
