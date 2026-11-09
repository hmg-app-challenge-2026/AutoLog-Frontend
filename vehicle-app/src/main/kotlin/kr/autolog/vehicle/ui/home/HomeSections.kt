package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.HeroMetric
import kr.autolog.vehicle.ui.design.components.MetricSize
import kr.autolog.vehicle.ui.design.components.RouteFlow
import kr.autolog.vehicle.ui.design.components.SectionHeader
import kr.autolog.vehicle.ui.design.components.TripListRow

/**
 * Home 가운데 두 섹션(D-25). 상태마다 "사용자가 지금 알고 싶은 것"을 왼쪽(값)·오른쪽(맥락)에 나눈다.
 * - 대기: 왼쪽 "이번 달 운행 정보"(누적 km + 보조 사실) · 오른쪽 "최근 운행 기록"(최신 3건 목록).
 * - 기록 중: 왼쪽 "이번 운행"(거리) · 오른쪽 "운행 경로"(출발지 → 흐르는 화살표 → 목적지).
 * 모든 소제목은 같은 [SectionHeader], 거리 숫자는 72 sp(TALL만 96) — 계기판처럼 계속 보는 앱이 아니므로.
 */
@Composable
internal fun HomePrimarySection(state: HomeUiState) {
    when (state.phase) {
        HomePhase.READY -> HomeSection(stringResource(R.string.section_month_trip)) { MonthSummary(state.month) }
        HomePhase.RECORDING_PARKED, HomePhase.AUTO_ENDING ->
            HomeSection(stringResource(R.string.section_this_trip)) { TripDistance(state.tripKm) }
        HomePhase.NEEDS_RECOVERY -> Unit
    }
}

@Composable
internal fun HomeSecondarySection(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    when (state.phase) {
        HomePhase.READY ->
            HomeSection(stringResource(R.string.section_recent_trips)) { RecentTrips(state.recentTrips, onAction) }
        HomePhase.RECORDING_PARKED, HomePhase.AUTO_ENDING ->
            HomeSection(stringResource(R.string.section_route)) {
                val c = AutoLogTheme.colors
                RouteFlow(
                    origin = state.origin,
                    destination = state.destination ?: stringResource(R.string.peek_no_destination),
                    destinationColor = if (state.destination != null) c.textPrimary else c.textSecondary,
                )
            }
        HomePhase.NEEDS_RECOVERY -> Unit
    }
}

/** 소제목 + 내용. 모든 섹션이 같은 모양. */
@Composable
private fun HomeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title)
        content()
    }
}

/** 큰 숫자 크기 — 높이 640 dp 창은 72 sp, 세로가 넉넉한 TALL은 96 sp. */
@Composable
private fun metricSize(): MetricSize = if (LocalWindowLayout.current ==
    WindowLayout.TALL
) {
    MetricSize.MEDIUM
} else {
    MetricSize.COMPACT
}

@Composable
private fun MonthSummary(month: MonthUi?) {
    val c = AutoLogTheme.colors
    HeroMetric(
        value = month?.totalKm,
        unit = stringResource(R.string.unit_km),
        label = null,
        unavailableText = stringResource(R.string.home_month_loading),
        size = metricSize(),
    )
    month?.let {
        Spacer(Modifier.height(Space.s2))
        Text(
            stringResource(R.string.home_month_meta, it.tripCount, it.businessKm),
            style = AutoLogTheme.type.caption,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun TripDistance(tripKm: String?) {
    if (tripKm == null) {
        // 이유는 알림 띠("계기판 거리를 읽지 못하고 있어요")가 말한다 — 여기선 조용히 미확정만, 추정값은 그리지 않는다
        Text(
            stringResource(R.string.trip_km_unknown),
            style = AutoLogTheme.type.label,
            color = AutoLogTheme.colors.textSecondary,
        )
        return
    }
    HeroMetric(
        value = tripKm,
        unit = stringResource(R.string.unit_km),
        label = null, // 소제목 "이번 운행"이 이미 말한다
        unavailableText = stringResource(R.string.distance_unavailable),
        size = metricSize(),
    )
}

/** 최근 운행 최대 3건, 위에서 아래로. 목적이 없는 운행은 아래 줄이 경고색 "목적 확인 필요" — 누르면 바로 목적 확인. */
@Composable
private fun RecentTrips(trips: List<RecentTripUi>, onAction: (HomeAction) -> Unit) {
    if (trips.isEmpty()) {
        Text(
            stringResource(R.string.recent_empty),
            style = AutoLogTheme.type.caption,
            color = AutoLogTheme.colors.textTertiary,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        trips.forEach { trip ->
            TripListRow(
                title = routeText(trip.origin, trip.destination),
                subtitle = tripMetaText(trip),
                onClick = { onAction(HomeAction.OpenTrip(trip.tripId, trip.needsReview)) },
                needsAttention = trip.needsReview,
            )
        }
    }
}

@Composable
private fun routeText(origin: String?, destination: String?): String {
    val unknown = stringResource(R.string.place_unknown)
    return stringResource(R.string.trip_route, origin ?: unknown, destination ?: unknown)
}

/** "오늘 09:12 · 23.4 km · 거래처 방문" / 목적이 없으면 끝이 "목적 확인 필요". */
@Composable
private fun tripMetaText(trip: RecentTripUi): String {
    val whenText =
        when (trip.day) {
            TripDay.TODAY -> stringResource(R.string.trip_day_today, trip.time)
            TripDay.YESTERDAY -> stringResource(R.string.trip_day_yesterday, trip.time)
            TripDay.EARLIER -> stringResource(R.string.trip_day_date, trip.month, trip.dayOfMonth, trip.time)
        }
    val km = trip.km?.let { stringResource(R.string.trip_km, it) } ?: stringResource(R.string.trip_km_unknown)
    val purpose = trip.purpose ?: stringResource(R.string.trip_needs_review)
    return stringResource(R.string.trip_meta, whenText, km, purpose)
}
