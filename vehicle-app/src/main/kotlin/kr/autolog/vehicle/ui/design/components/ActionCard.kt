package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/**
 * 디자인 언어 11장 ActionCard: 넓은 창의 보조 동작 타일. 아이콘(왼쪽 위) + 이름(아래), 카드 전체가 터치 대상.
 * 세로 배치라 2열 격자에서도 이름이 한 줄을 온전히 쓴다(가로 배치는 아이콘 몫만큼 이름 폭이 줄어 글자 단위로 꺾였다).
 */
@Composable
fun ActionCard(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier, badge: Int = 0) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Column(
        modifier
            .heightIn(min = Size.buttonLg)
            .pressFeedback(interaction)
            .clip(RoundedCornerShape(Radius.md))
            .background(c.surface1)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }.padding(Space.s4),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(Size.icon))
            Spacer(Modifier.weight(1f))
            if (badge > 0) CountBadge(badge)
        }
        Text(title, style = AutoLogTheme.type.label, color = c.textPrimary, maxLines = 2)
    }
}

/** 좁은 창의 보조 동작: 원형 아이콘 버튼(96 dp) + 아래 글자. */
@Composable
fun QuickAction(icon: ImageVector, title: String, onClick: () -> Unit, modifier: Modifier = Modifier, badge: Int = 0) {
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
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier.size(Size.touchMin).clip(CircleShape).background(c.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(Size.icon))
            }
            if (badge > 0) CountBadge(badge)
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

/** 숫자 배지(도착한 영수증 등). accentFill + 흰 글씨. */
@Composable
fun CountBadge(count: Int) {
    val c = AutoLogTheme.colors
    Text(
        count.toString(),
        style = AutoLogTheme.type.caption,
        color = c.onAccent,
        modifier = Modifier.clip(
            CircleShape,
        ).background(c.accentFill).padding(horizontal = Space.s3, vertical = Space.s1),
    )
}
