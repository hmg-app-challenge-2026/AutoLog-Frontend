package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import java.time.YearMonth

/**
 * 디자인 언어 11장 PeriodSwitcher — 달 이동 **◀ 2026년 11월 ▶**(운행 기록 조회·운행기록부 내보내기가 같이 쓴다).
 * 화살표는 `RoundIconButton`, 가운데 글자는 창 크기 역할 `periodTitle`. [canGoNext]가 false면 ▶가 흐려지고 안 눌린다(미래 달 없음).
 */
@Composable
fun PeriodSwitcher(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.month_prev), onPrev)
        Text(
            stringResource(R.string.period_month, month.year, month.monthValue),
            style = AutoLogTheme.windowType.periodTitle,
            color = AutoLogTheme.colors.textPrimary,
        )
        RoundIconButton(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            stringResource(R.string.month_next),
            onNext,
            enabled = canGoNext,
        )
    }
}
