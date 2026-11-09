package kr.autolog.vehicle.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R

/** "5초" / "2분 41초" */
@Composable
fun durationText(sec: Int): String = when {
    sec < 60 -> stringResource(R.string.duration_sec, sec)
    else -> stringResource(R.string.duration_min_sec, sec / 60, sec % 60)
}
