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
 * 알림 목록(D-23): 처리할 일을 한 목록에. 제목 `알림 N` + 줄마다 1탭. 놓친 운행은 줄 안에서 [추가]·[무시]로 끝낸다.
 * 줄의 모양(아이콘·색·문장·동작)은 [HomeNotice] 종류마다 여기 한 곳에서 정한다.
 */
@Composable
internal fun HomeNotices(notices: List<HomeNotice>, onAction: (HomeAction) -> Unit) {
    val c = AutoLogTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        Text(
            stringResource(R.string.notices_title, notices.size),
            // 목록 머리는 작은 섹션 제목 — 줄 내용보다 한 단계 낮게
            style = AutoLogTheme.type.caption,
            color = c.textSecondary,
        )
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
