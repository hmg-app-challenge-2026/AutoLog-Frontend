package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/**
 * 디자인 언어 11장 DockButton — 화면 맨 아래 독(넓은 창)의 동작. `surface1` 알약, 높이 76 dp, 아이콘(`accent`) + 이름(`label`).
 * [showLabel]이 false면 아이콘만 있는 정사각(76 dp) — 자주 쓰지 않는 동작(설정). 이름은 접근성 설명으로 남긴다.
 */
@Composable
fun DockButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Row(
        modifier
            .heightIn(min = Size.touchMin)
            .widthIn(min = Size.touchMin)
            .pressFeedback(interaction)
            .clip(RoundedCornerShape(Radius.lg))
            .background(c.surface1)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }.then(
                if (showLabel) {
                    Modifier.padding(horizontal = Space.s5)
                } else {
                    Modifier.semantics {
                        contentDescription =
                            label
                    }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(Size.icon))
        if (showLabel) Text(label, style = AutoLogTheme.type.label, color = c.textPrimary, maxLines = 1)
    }
}

/** 좁은 창 독의 동작: 원형 아이콘(76 dp, `surface2`) + 아래 이름(`caption`). */
@Composable
fun QuickAction(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Column(
        modifier
            .widthIn(min = Size.touchMin)
            .pressFeedback(interaction)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Box(
            Modifier.size(Size.touchMin).clip(CircleShape).background(c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(Size.icon))
        }
        Text(
            title,
            style = AutoLogTheme.type.caption,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
