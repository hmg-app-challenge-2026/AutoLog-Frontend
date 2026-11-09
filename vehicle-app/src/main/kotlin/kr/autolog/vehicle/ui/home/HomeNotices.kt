package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.NoticeRow
import kr.autolog.vehicle.ui.design.components.TertiaryButton

/**
 * 알림 목록(D-23·D-24, 오른쪽 섹션 "알림"): 처리할 일을 한 목록에. 줄마다 1탭. 놓친 운행은 줄 안에서 [추가]·[무시]로 끝낸다.
 * 없으면 "새 알림이 없어요".
 * 줄의 모양(아이콘·색·문장·동작)은 [HomeNotice] 종류마다 여기 한 곳에서 정한다.
 */
@Composable
internal fun HomeNotices(notices: List<HomeNotice>, onAction: (HomeAction) -> Unit) {
    val c = AutoLogTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        // 없을 때도 칸을 지킨다 — 좌우 반반이 무너지지 않게(D-24)
        if (notices.isEmpty()) {
            Text(stringResource(R.string.notices_empty), style = AutoLogTheme.type.body, color = c.textTertiary)
        }
        notices.forEach { notice ->
            when (notice) {
                is HomeNotice.Review ->
                    NoticeRow(
                        Icons.Rounded.ErrorOutline,
                        c.warning,
                        stringResource(R.string.notice_review, notice.count),
                        onClick = { onAction(HomeAction.OpenReview) },
                    )
                is HomeNotice.Receipt ->
                    NoticeRow(
                        Icons.AutoMirrored.Rounded.ReceiptLong,
                        c.accent,
                        stringResource(R.string.notice_receipt, notice.count),
                        onClick = { onAction(HomeAction.OpenReceipt) },
                    )
                is HomeNotice.MissedTrip ->
                    NoticeRow(
                        Icons.Rounded.Info,
                        c.accent,
                        stringResource(R.string.notice_missed, notice.km),
                        actions = {
                            TertiaryButton(stringResource(R.string.home_missed_add), onClick = {
                                onAction(HomeAction.AcceptMissedTrip)
                            })
                            TertiaryButton(stringResource(R.string.home_missed_ignore), onClick = {
                                onAction(HomeAction.IgnoreMissedTrip)
                            })
                        },
                    )
            }
        }
    }
}
