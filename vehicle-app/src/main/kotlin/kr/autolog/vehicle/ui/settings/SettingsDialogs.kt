package kr.autolog.vehicle.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.DangerButton
import kr.autolog.vehicle.ui.design.components.InputField
import kr.autolog.vehicle.ui.design.components.ModalCard
import kr.autolog.vehicle.ui.design.components.ModalText
import kr.autolog.vehicle.ui.design.components.NumericKeypad
import kr.autolog.vehicle.ui.design.components.PrimaryButton

/**
 * 설정 팝업(모두 `ModalCard` — 오른쪽 위 X로 닫는다, D-30). 결정 버튼은 하단 고정(`footer`).
 * 실패하면 팝업 안에 이유 한 줄, 성공하면 ViewModel이 팝업을 닫고 섹션 아래에 결과 한 줄.
 */
@Composable
internal fun SettingsDialogHost(state: SettingsUiState, onAction: (SettingsAction) -> Unit) {
    val dismiss = { onAction(SettingsAction.DismissDialog) }
    val submit = { onAction(SettingsAction.SubmitDialog) }
    when (val d = state.dialog ?: return) {
        is SettingsDialog.Register -> ModalCard(
            title = stringResource(R.string.settings_register_title),
            onClose = dismiss,
            footer = {
                PrimaryButton(
                    stringResource(R.string.settings_register_submit),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = d.code.length == REGISTER_CODE_LENGTH && d.serverUrl.isNotBlank(),
                    loading = state.running == SettingsTask.REGISTER,
                )
            },
        ) {
            InputField(
                stringResource(R.string.settings_server_url),
                d.serverUrl,
                { onAction(SettingsAction.EditDialog(DialogField.SERVER_URL, it)) },
                keyboardType = KeyboardType.Uri,
            )
            Text(
                stringResource(R.string.settings_register_code),
                style = AutoLogTheme.type.caption,
                color = AutoLogTheme.colors.textSecondary,
            )
            CodeBoxes(d.code)
            NumericKeypad(
                onDigit = { onAction(SettingsAction.RegisterKey(it)) },
                onDelete = { onAction(SettingsAction.RegisterDelete) },
                allowDecimal = false,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            DialogError(state, SettingsTask.REGISTER)
        }
        is SettingsDialog.AddRecipient -> ModalCard(
            title = stringResource(R.string.settings_add_recipient),
            onClose = dismiss,
            footer = {
                PrimaryButton(
                    stringResource(R.string.action_add),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = d.email.isNotBlank(),
                    loading = state.running == SettingsTask.ADD_RECIPIENT,
                )
            },
        ) {
            InputField(
                stringResource(R.string.settings_email),
                d.email,
                { onAction(SettingsAction.EditDialog(DialogField.EMAIL, it)) },
                keyboardType = KeyboardType.Email,
                placeholder = stringResource(R.string.settings_email_hint),
            )
            InputField(
                stringResource(R.string.settings_name_optional),
                d.name,
                { onAction(SettingsAction.EditDialog(DialogField.NAME, it)) },
                placeholder = stringResource(R.string.settings_recipient_name_hint),
            )
            DialogError(state, SettingsTask.ADD_RECIPIENT)
        }
        is SettingsDialog.AddStaff -> ModalCard(
            title = stringResource(R.string.settings_add_staff),
            onClose = dismiss,
            footer = {
                PrimaryButton(
                    stringResource(R.string.action_add),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = d.name.isNotBlank(),
                    loading = state.running == SettingsTask.ADD_STAFF,
                )
            },
        ) {
            InputField(
                stringResource(R.string.settings_name),
                d.name,
                { onAction(SettingsAction.EditDialog(DialogField.NAME, it)) },
                placeholder = stringResource(R.string.picker_add_name_hint),
            )
            InputField(
                stringResource(R.string.settings_dept_optional),
                d.dept,
                { onAction(SettingsAction.EditDialog(DialogField.DEPT, it)) },
                placeholder = stringResource(R.string.picker_add_dept_hint),
            )
            InputField(
                stringResource(R.string.settings_email_optional),
                d.email,
                { onAction(SettingsAction.EditDialog(DialogField.EMAIL, it)) },
                keyboardType = KeyboardType.Email,
                placeholder = stringResource(R.string.settings_email_hint),
            )
            DialogError(state, SettingsTask.ADD_STAFF)
        }
        is SettingsDialog.RemoveRecipient -> ModalCard(
            title = stringResource(R.string.settings_remove_recipient_title),
            onClose = dismiss,
            footer = {
                DangerButton(
                    stringResource(R.string.action_remove),
                    onClick = submit,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        ) {
            ModalText(
                stringResource(
                    R.string.settings_remove_recipient_body,
                    d.name?.let { stringResource(R.string.settings_name_with_email, it, d.email) } ?: d.email,
                ),
            )
            DialogError(state, SettingsTask.REMOVE_RECIPIENT)
        }
    }
}

/** 연결 코드 6칸 — 입력한 숫자는 칸에, 다음 칸은 accent 테두리 없이 비어 있다. */
@Composable
private fun CodeBoxes(code: String) {
    val c = AutoLogTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.s2, Alignment.CenterHorizontally),
    ) {
        repeat(REGISTER_CODE_LENGTH) { i ->
            Box(
                Modifier.size(Size.denseFace).clip(RoundedCornerShape(Radius.sm)).background(c.surface3),
                contentAlignment = Alignment.Center,
            ) {
                Text(code.getOrNull(i)?.toString().orEmpty(), style = AutoLogTheme.type.title, color = c.textPrimary)
            }
        }
    }
}

/** 팝업 안 실패 한 줄(성공이면 팝업이 닫히므로 실패만). */
@Composable
private fun DialogError(state: SettingsUiState, task: SettingsTask) {
    val failed = state.results[task] as? TaskResult.Failed ?: return
    Text(failed.reason, style = AutoLogTheme.type.body, color = AutoLogTheme.colors.danger)
}
