package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
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

/**
 * Home 왼쪽 섹션 내용(D-23·D-24).
 * - 대기("이번 달 운행 정보"): 이번 달 요약 — `412.6 km` / `23회 운행 · 업무 388.1 km`.
 * - 기록 중("현재 운행 정보"): 이번 운행 거리 + 경로(Peek과 같은 [RouteFlow]). 경과 시간은 상태 줄이 말한다.
 */
@Composable
internal fun ColumnScope.HomeHero(state: HomeUiState) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val layout = LocalWindowLayout.current
    val narrow = layout == WindowLayout.NARROW
    when (state.phase) {
        HomePhase.READY -> {
            // 위계: 소제목(22 굵게) → 값(숫자) → 보조 사실 한 줄(22 회색). 소제목보다 큰 회색 설명은 두지 않는다
            val month = state.month
            HeroMetric(
                value = month?.totalKm,
                unit = stringResource(R.string.unit_km),
                label = null,
                unavailableText = stringResource(R.string.home_month_loading),
                size = if (narrow) MetricSize.COMPACT else MetricSize.MEDIUM,
            )
            month?.let {
                Spacer(Modifier.height(Space.s2))
                Text(
                    stringResource(R.string.home_month_meta, it.tripCount, it.businessKm),
                    style = t.caption,
                    color = c.textSecondary,
                )
            }
        }
        HomePhase.RECORDING_PARKED, HomePhase.AUTO_ENDING -> {
            HeroMetric(
                value = state.tripKm,
                unit = stringResource(R.string.unit_km),
                label = null, // 섹션 소제목 "현재 운행 정보"가 이미 말한다
                unavailableText = stringResource(R.string.distance_unavailable),
                // 가장 큰 숫자는 세로가 넉넉한 TALL에서만 — 높이 640 dp 창에선 경로가 밀려 내려갔다
                size = when (layout) {
                    WindowLayout.TALL -> MetricSize.HERO
                    WindowLayout.WIDE -> MetricSize.MEDIUM
                    WindowLayout.NARROW -> MetricSize.COMPACT
                },
            )
            Spacer(Modifier.height(Space.s4))
            RouteFlow(
                origin = state.origin,
                destination = state.destination ?: stringResource(R.string.peek_no_destination),
                destinationColor = if (state.destination != null) c.textPrimary else c.textSecondary,
            )
        }
        HomePhase.NEEDS_RECOVERY -> Unit
    }
}
