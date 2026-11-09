package kr.autolog.vehicle.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.DangerButton
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.ScrimSheet
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.components.TertiaryButton

/** Backend F-10 복구. 되돌릴 수 없는 [삭제]만 한 번 더 묻는다(디자인 언어 2.6). */
@Composable
fun RecoveryDialog(extraKm: String?, onChoice: (RecoveryChoice) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    ScrimSheet {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (!confirmingDelete) {
                Text(stringResource(R.string.recovery_title), style = t.headline, color = c.textPrimary)
                extraKm?.let {
                    Text(stringResource(R.string.recovery_body, it), style = t.title, color = c.textSecondary)
                }
                PrimaryButton(
                    text = stringResource(R.string.recovery_record),
                    onClick = { onChoice(RecoveryChoice.RECORD) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.recovery_continue),
                    onClick = { onChoice(RecoveryChoice.CONTINUE) },
                    modifier = Modifier.fillMaxWidth(),
                )
                TertiaryButton(
                    text = stringResource(R.string.recovery_delete),
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(stringResource(R.string.recovery_delete_confirm), style = t.title, color = c.textPrimary)
                DangerButton(
                    text = stringResource(R.string.recovery_delete_yes),
                    onClick = { onChoice(RecoveryChoice.DELETE) },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { confirmingDelete = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
