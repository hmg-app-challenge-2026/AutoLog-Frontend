package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.CloseButton
import kr.autolog.vehicle.ui.design.components.ModalCard
import kr.autolog.vehicle.ui.design.components.NoticePill
import kr.autolog.vehicle.ui.design.components.NoticeRow
import kr.autolog.vehicle.ui.design.components.OverflowRow
import kr.autolog.vehicle.ui.design.components.TertiaryButton

/**
 * 알림 한 건의 모양(아이콘·색·문장·동작). [HomeNotice] 종류마다 여기 한 곳에서 정하고,
 * 넓은 창의 알림 띠([HomeNoticeStrip])와 좁은 창·넘침 목록([HomeNoticeList])이 같은 정의를 쓴다.
 * [onClick] = 줄을 누르면 처리 화면으로. [onAccept]/[onDismiss] = 줄 안에서 끝내는 일(놓친 운행).
 */
private data class NoticeSpec(
    val icon: ImageVector,
    val tint: Color,
    val text: String,
    val onClick: (() -> Unit)? = null,
    val onAccept: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null,
)

@Composable
private fun noticeSpec(notice: HomeNotice, onAction: (HomeAction) -> Unit): NoticeSpec {
    val c = AutoLogTheme.colors
    return when (notice) {
        HomeNotice.OdometerUnavailable -> NoticeSpec(
            Icons.Rounded.Speed,
            c.warning,
            stringResource(R.string.notice_odometer),
        )
        HomeNotice.SaveDelayed -> NoticeSpec(
            Icons.Rounded.Save,
            c.warning,
            stringResource(R.string.notice_save_delayed),
        )
        is HomeNotice.UploadFailed ->
            NoticeSpec(
                Icons.Rounded.CloudOff,
                c.warning,
                stringResource(R.string.notice_upload_failed, notice.pending),
                onClick = { onAction(HomeAction.OpenSettings) },
            )
        is HomeNotice.Review ->
            NoticeSpec(
                Icons.Rounded.ErrorOutline,
                c.warning,
                stringResource(R.string.notice_review, notice.count),
                onClick = { onAction(HomeAction.OpenReview) },
            )
        is HomeNotice.Receipt ->
            NoticeSpec(
                Icons.AutoMirrored.Rounded.ReceiptLong,
                c.accent,
                stringResource(R.string.notice_receipt, notice.count),
                onClick = { onAction(HomeAction.OpenReceipt) },
            )
        is HomeNotice.MissedTrip ->
            NoticeSpec(
                Icons.Rounded.Info,
                c.accent,
                stringResource(R.string.notice_missed, notice.km),
                onAccept = { onAction(HomeAction.AcceptMissedTrip) },
                onDismiss = { onAction(HomeAction.IgnoreMissedTrip) },
            )
    }
}

/**
 * 넓은 창 알림 띠(D-25): 상태 줄 바로 아래 한 줄, **알림이 있을 때만**. 알약을 가로로 놓고,
 * 한 줄에 다 안 들어가면 끝에 "외 N건 ›" — 누르면 전체 목록 팝업(오른쪽 위 X로 닫기).
 */
@Composable
internal fun HomeNoticeStrip(notices: List<HomeNotice>, onAction: (HomeAction) -> Unit) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val specs = notices.map { noticeSpec(it, onAction) }
    val c = AutoLogTheme.colors
    OverflowRow(
        count = specs.size,
        spacing = Space.s3,
        item = { i ->
            val spec = specs[i]
            NoticePill(
                spec.icon,
                spec.tint,
                spec.text,
                onClick = spec.onClick,
                trailing =
                if (spec.onAccept != null) {
                    {
                        TertiaryButton(stringResource(R.string.home_missed_add), onClick = spec.onAccept)
                        spec.onDismiss?.let { CloseButton(it, label = stringResource(R.string.home_missed_ignore)) }
                    }
                } else {
                    null
                },
            )
        },
        more = { hidden ->
            NoticePill(Icons.Rounded.Info, c.textSecondary, stringResource(R.string.notices_more, hidden), onClick = {
                showAll =
                    true
            })
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (showAll) {
        ModalCard(maxWidth = Size.dialogMaxWidth, title = stringResource(R.string.section_notices), onClose = {
            showAll =
                false
        }) {
            HomeNoticeList(notices, onAction)
        }
    }
}

/** 좁은 창(그리고 넘침 팝업)의 알림 목록: 한 건 한 줄([NoticeRow]). 놓친 운행은 줄 안에서 [추가] · [무시]. */
@Composable
internal fun HomeNoticeList(notices: List<HomeNotice>, onAction: (HomeAction) -> Unit) {
    val specs = notices.map { noticeSpec(it, onAction) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        specs.forEach { spec ->
            NoticeRow(
                spec.icon,
                spec.tint,
                spec.text,
                onClick = spec.onClick,
                actions =
                if (spec.onAccept != null) {
                    {
                        TertiaryButton(stringResource(R.string.home_missed_add), onClick = spec.onAccept)
                        spec.onDismiss?.let {
                            TertiaryButton(stringResource(R.string.home_missed_ignore), onClick = it)
                        }
                    }
                } else {
                    null
                },
            )
        }
    }
}
