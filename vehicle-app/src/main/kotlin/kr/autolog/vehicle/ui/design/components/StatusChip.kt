package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/** [MUTED] = 아무 일도 없는 상태(대기 중) — 보조 글자색으로 조용하게. */
enum class ChipTone { NEUTRAL, MUTED, ACCENT, SUCCESS, WARNING, DANGER }

/** [onClick]이 null이면 터치할 수 없는 표시용(Peek). */
@Composable
fun StatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = AutoLogTheme.colors
    val fg =
        when (tone) {
            ChipTone.NEUTRAL -> c.textPrimary
            ChipTone.MUTED -> c.textSecondary
            ChipTone.ACCENT -> c.accent
            ChipTone.SUCCESS -> c.success
            ChipTone.WARNING -> c.warning
            ChipTone.DANGER -> c.danger
        }
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val clickable = onClick != null && !LocalInputLocked.current
    var m = modifier.heightIn(min = if (onClick != null) Size.chipHeight else Size.iconLg)
    if (onClick != null) {
        m =
            m.pressFeedback(interaction).clickable(interaction, null, enabled = clickable, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }
    }
    Row(m, verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.clip(CircleShape).background(c.surface3).padding(horizontal = Space.s5, vertical = Space.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            leading?.invoke()
            if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(Size.icon))
            Text(text, style = AutoLogTheme.type.label, color = fg)
        }
    }
}
