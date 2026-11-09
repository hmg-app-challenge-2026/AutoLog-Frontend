package kr.autolog.vehicle.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.InputField
import kr.autolog.vehicle.ui.design.components.ModalCard
import kr.autolog.vehicle.ui.design.components.ModalText
import kr.autolog.vehicle.ui.design.components.PrimaryButton

/**
 * F-17 거래처 등록 제안 — 목적을 확정한 뒤 엔진이 제안할 때만. 이름 칸은 도착지 이름이 미리 채워져 있어 대부분 [등록] 1탭.
 * 오른쪽 위 X = 아니요(접근성 이름도 "아니요"). 어느 쪽이든 목적은 이미 저장돼 있다.
 */
@Composable
fun PlaceProposalDialog(place: PlaceProposalUi, onAction: (ReviewAction) -> Unit) {
    ModalCard(
        title = stringResource(R.string.place_title),
        onClose = { onAction(ReviewAction.SkipPlace) },
        closeLabel = stringResource(R.string.place_skip),
        footer = {
            PrimaryButton(
                stringResource(R.string.place_register),
                onClick = { onAction(ReviewAction.RegisterPlace) },
                modifier = Modifier.fillMaxWidth(),
                loading = place.saving,
                enabled = place.canRegister,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            // 머리 줄 제목은 짧게(X와 같은 줄에서 한 줄로) — 질문은 본문 첫 줄
            ModalText(
                stringResource(R.string.place_question),
                style = AutoLogTheme.type.title,
                color = AutoLogTheme.colors.textPrimary,
            )
            InputField(
                label = stringResource(R.string.place_label),
                value = place.label,
                onValueChange = { onAction(ReviewAction.PlaceLabelChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
            )
            place.error?.let { ModalText(it, color = AutoLogTheme.colors.danger) }
        }
    }
}
