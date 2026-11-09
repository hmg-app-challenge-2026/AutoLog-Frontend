package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
 * 디자인 언어 11장 DockTile — 화면 맨 아래 동작 타일. 모든 타일이 같은 모양: `surface1`, `radius.lg`, 높이 96 dp,
 * 아이콘(36 dp, `accent`) 위 · 이름(`label`) 아래, 가운데 정렬. 다른 것은 아이콘과 이름뿐이다.
 * [emphasized]면 파란 채움(`accentFill` + `onAccent`) — 같은 자리에서 상태가 바뀐 주 동작(기록 중의 [기록 종료]).
 * 폭은 부모가 정한다(보통 `weight(1f)`로 등분). 이름이 한 줄에 들어가려면 최소 [Size.dockTileMinWidth].
 */
@Composable
fun DockTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val c = AutoLogTheme.colors
    val fg = if (emphasized) c.onAccent else c.textPrimary
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Column(
        modifier
            .heightIn(min = Size.buttonLg)
            .pressFeedback(interaction)
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (emphasized) c.accentFill else c.surface1)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            }.padding(horizontal = Space.s3, vertical = Space.s3),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s1, Alignment.CenterVertically),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (emphasized) c.onAccent else c.accent,
            modifier = Modifier.size(Size.icon),
        )
        Text(label, style = AutoLogTheme.type.label, color = fg, textAlign = TextAlign.Center, maxLines = 1)
    }
}
