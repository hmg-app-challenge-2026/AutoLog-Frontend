package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/**
 * 디자인 언어 11장 TripListRow — 운행 한 건 목록 줄(Home "최근 운행 기록", 이력). `surface1`, `radius.md`, 최소 높이 76 dp.
 * 위 [title](경로, `label`, 한 줄 말줄임) · 아래 [subtitle](날짜·거리·목적, `caption`, `textSecondary`).
 * [needsAttention]이면 아래 줄을 `warning`으로 — 목적 확인이 필요한 운행. 줄 전체가 터치 대상, 끝에 ›.
 */
@Composable
fun TripListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    needsAttention: Boolean = false,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchMin)
            .pressFeedback(interaction)
            .clip(RoundedCornerShape(Radius.md))
            .background(c.surface1)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }.padding(horizontal = Space.s5, vertical = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.s1)) {
            Text(title, style = t.label, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = t.caption,
                color = if (needsAttention) c.warning else c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
    }
}
