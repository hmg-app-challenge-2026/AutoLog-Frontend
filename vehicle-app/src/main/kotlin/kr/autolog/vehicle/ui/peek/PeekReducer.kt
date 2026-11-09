package kr.autolog.vehicle.ui.peek

import kr.autolog.vehicle.data.repository.AutoEndReason
import kr.autolog.vehicle.data.repository.TripSession
import kr.autolog.vehicle.ui.design.formatKm
import kr.autolog.vehicle.ui.design.remainingSeconds

/** 스펙 7.0 내용 ①~⑤. 순수 함수(테스트 대상). */
internal fun reducePeek(session: TripSession, odometerKm: Double?, destination: String?, now: Long): PeekUiState =
    when (session) {
        is TripSession.Recording ->
            PeekUiState.Recording(
                tripKm = formatKm(session.liveKm),
                origin = session.trip.origin,
                destination = destination,
                autoEndInSec = null,
            )
        is TripSession.AutoEnding ->
            PeekUiState.Recording(
                tripKm = formatKm(session.liveKm),
                origin = session.trip.origin,
                destination = destination,
                // 기어 P는 Peek가 아니라 Home 카드·시스템 알림이 카운트다운을 보여 준다(스펙 7.2.1)
                autoEndInSec =
                if (session.reason == AutoEndReason.GEAR_P) null else remainingSeconds(session.deadlineAt, now),
            )
        else -> PeekUiState.NoTrip(formatKm(odometerKm))
    }
