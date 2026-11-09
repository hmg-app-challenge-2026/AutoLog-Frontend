package kr.autolog.vehicle.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.data.repository.AddDriverResult
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.InputField
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.ScrimSheet
import kr.autolog.vehicle.ui.design.components.TertiaryButton

/**
 * 스펙 7.1 [+ 새 운전자 추가] 시트(F-24, origin=LOCAL). 이름만 있으면 [추가하고 시작] 한 번으로 그 사람으로 시작한다.
 * 부서는 선택. 키보드가 올라오면 시트가 같이 올라간다(imePadding).
 */
@Composable
fun AddDriverSheet(form: AddDriverForm, onAction: (ProfilePickerAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    ScrimSheet(modifier.imePadding()) {
        Text(stringResource(R.string.picker_add), style = t.headline, color = c.textPrimary)
        Spacer(Modifier.height(Space.s5))
        InputField(
            label = stringResource(R.string.picker_add_name),
            value = form.name,
            onValueChange = { onAction(ProfilePickerAction.AddNameChanged(it)) },
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.s4))
        InputField(
            label = stringResource(R.string.picker_add_dept),
            value = form.dept,
            onValueChange = { onAction(ProfilePickerAction.AddDeptChanged(it)) },
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth(),
        )
        form.error?.let {
            Spacer(Modifier.height(Space.s3))
            Text(errorText(it), style = t.caption, color = c.danger)
        }
        Spacer(Modifier.height(Space.s6))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s5),
        ) {
            TertiaryButton(stringResource(R.string.action_cancel), onClick = {
                onAction(ProfilePickerAction.CancelAdd)
            })
            PrimaryButton(
                stringResource(R.string.picker_add_confirm),
                onClick = { onAction(ProfilePickerAction.SubmitAdd) },
                modifier = Modifier.weight(1f),
                enabled = form.canSubmit,
                loading = form.saving,
            )
        }
    }
}

@Composable
private fun errorText(failure: AddDriverResult.Failure): String = when (failure) {
    AddDriverResult.EmptyName -> stringResource(R.string.picker_add_name_required)
    is AddDriverResult.Rejected -> failure.reason
}
