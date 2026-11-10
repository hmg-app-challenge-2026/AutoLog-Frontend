package kr.autolog.vehicle.ui.trips

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.common.AppZone
import kr.autolog.vehicle.data.repository.HistoryItem
import kr.autolog.vehicle.data.repository.UploadMark
import kr.autolog.vehicle.ui.design.formatKm
import java.time.Instant
import java.time.format.DateTimeFormatter

/** 운행이 시작된 날이 오늘·어제·그 전인지 — "오늘 09:12" / "어제 17:40" / "11월 7일 19:34". */
enum class TripDay { TODAY, YESTERDAY, EARLIER }

/**
 * 운행 한 줄(Home "최근 운행 기록"·운행 기록 조회가 같이 쓴다). 값만 담고 문구는 화면이 strings.xml로 만든다.
 * [purpose]가 null이면 "목적 미입력"(회색, 재촉하지 않음 — D-26), [km]가 null이면 "거리 미확정".
 */
data class TripRowUi(
    val tripId: String,
    val origin: String?,
    val destination: String?,
    val km: String?,
    val day: TripDay,
    val month: Int,
    val dayOfMonth: Int,
    val time: String,
    val purpose: String?,
    val driverName: String? = null,
    /** 운전자 미확정(F-20) — 운행 기록 조회가 이 줄 안에서 바로 지정하게 한다. */
    val driverUnset: Boolean = false,
    /** 사내 서버 전송 상태. 서버를 연결하지 않았으면 null(표시 안 함). */
    val upload: UploadMark? = null,
)

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** 저장소 항목 → 한 줄. [now]로 오늘·어제를 [AppZone](Asia/Seoul) 기준으로 가른다. 순수 함수. */
fun HistoryItem.toTripRowUi(now: Long): TripRowUi {
    val start = Instant.ofEpochMilli(startTime).atZone(AppZone)
    val today = Instant.ofEpochMilli(now).atZone(AppZone).toLocalDate()
    val day =
        when (start.toLocalDate()) {
            today -> TripDay.TODAY
            today.minusDays(1) -> TripDay.YESTERDAY
            else -> TripDay.EARLIER
        }
    return TripRowUi(
        tripId = tripId,
        origin = origin,
        destination = destination,
        km = formatKm(distanceKm),
        day = day,
        month = start.monthValue,
        dayOfMonth = start.dayOfMonth,
        time = start.format(TIME),
        purpose = purpose?.labelKo,
        driverName = driverName,
        driverUnset = driverUnset,
        upload = upload,
    )
}

/** "본사 → 판교 테크노밸리 거래처". 모르는 쪽은 "위치 미상". */
@Composable
fun tripRouteText(row: TripRowUi): String {
    val unknown = stringResource(R.string.place_unknown)
    return stringResource(R.string.trip_route, row.origin ?: unknown, row.destination ?: unknown)
}

/**
 * 아래 줄 사실들을 " · "로 잇는다: 언제 · (운전자) · 거리 · 목적 · (전송 상태).
 * Home은 언제·거리·목적만, 운행 기록 조회는 [withDriver]·[withUpload]까지.
 */
@Composable
fun tripMetaText(row: TripRowUi, withDriver: Boolean = false, withUpload: Boolean = false): String {
    val parts = buildList {
        add(
            when (row.day) {
                TripDay.TODAY -> stringResource(R.string.trip_day_today, row.time)
                TripDay.YESTERDAY -> stringResource(R.string.trip_day_yesterday, row.time)
                TripDay.EARLIER -> stringResource(R.string.trip_day_date, row.month, row.dayOfMonth, row.time)
            },
        )
        if (withDriver) {
            add(row.driverName ?: stringResource(R.string.trip_driver_unset))
        }
        add(row.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown))
        add(row.purpose ?: stringResource(R.string.trip_no_purpose))
        if (withUpload) {
            row.upload?.let { add(stringResource(uploadLabel(it))) }
        }
    }
    return parts.joinToString(stringResource(R.string.meta_separator))
}

private fun uploadLabel(mark: UploadMark): Int = when (mark) {
    UploadMark.PENDING -> R.string.upload_pending
    UploadMark.DONE -> R.string.upload_done
    UploadMark.FAILED -> R.string.upload_failed
}
