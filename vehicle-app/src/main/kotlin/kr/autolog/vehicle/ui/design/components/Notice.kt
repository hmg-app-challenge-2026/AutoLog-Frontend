package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/**
 * 디자인 언어 11장 NoticeRow — 알림 목록의 한 줄(처리할 일 하나). `surface1`, `radius.md`, 최소 높이 76 dp.
 * 의미색 아이콘 + 문장. [onClick]이 있으면 줄 전체가 터치 대상이고 끝에 ›. [actions]가 있으면 문장 아래에 버튼을 둔다
 * (줄 안에서 바로 끝내는 일 — 예: 놓친 운행 [추가]·[무시]). 둘을 함께 쓰지 않는다.
 */
@Composable
fun NoticeRow(
    icon: ImageVector,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val tap =
        if (onClick != null) {
            Modifier.pressFeedback(
                interaction,
            ).clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }
        } else {
            Modifier
        }
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchMin)
            .clip(RoundedCornerShape(Radius.md))
            .background(c.surface1)
            .then(tap)
            .padding(horizontal = Space.s5, vertical = Space.s3),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.heightIn(min = Size.touchMin - Space.s3 * 2), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Size.icon))
            Text(
                text,
                style = AutoLogTheme.type.label,
                color = c.textPrimary,
                modifier = Modifier.weight(1f).padding(horizontal = Space.s4),
            )
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
            }
        }
        if (actions != null) {
            Row(
                Modifier.padding(start = Size.icon),
                horizontalArrangement = Arrangement.spacedBy(Space.s2),
                content = actions,
            )
        }
    }
}

/**
 * 디자인 언어 11장 NoticePill — 넓은 창 알림 띠의 알림 한 건. 알약(`radius.full`) `surface1`, 높이 76 dp,
 * 의미색 아이콘(28 dp) + 문장(`label`). [onClick]이 있으면 알약 전체가 터치 대상이고 끝에 ›.
 * [trailing]이 있으면 문장 뒤에 그 줄 안에서 끝내는 버튼을 둔다(놓친 운행 [추가] · X). 둘을 함께 쓰지 않는다.
 */
@Composable
fun NoticePill(
    icon: ImageVector,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val tap =
        if (onClick != null) {
            Modifier.pressFeedback(
                interaction,
            ).clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }
        } else {
            Modifier
        }
    Row(
        modifier
            .heightIn(min = Size.touchMin)
            .clip(CircleShape)
            .background(c.surface1)
            .then(tap)
            .padding(start = Space.s5, end = if (trailing != null) Space.s1 else Space.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Size.iconSm))
        Text(text, style = AutoLogTheme.type.label, color = c.textPrimary, maxLines = 1)
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
        trailing?.invoke(this)
    }
}
