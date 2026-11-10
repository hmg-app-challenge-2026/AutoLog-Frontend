package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout

/**
 * 디자인 언어 11장 DockTile — 화면 맨 아래 동작. **아이콘 네모 + 그 아래 이름**(앱 아이콘·테슬라 하단 바 방식).
 * 화면 폭에 따라 112/96/88 dp 네모 안에 아이콘, 아래 이름을 놓는다. 터치 영역은 네모 + 이름 칸 전체(76 dp 이상).
 * [emphasized]면 네모가 파란 채움(`accentFill` + `onAccent` 아이콘), 이름은 `accent` — 같은 자리에서 상태가 바뀐 주 동작(기록 중의 [기록 종료]).
 * 폭은 부모가 정한다(보통 `weight(1f)`로 등분 — 네모가 같은 간격으로 놓인다).
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
    val layout = LocalWindowLayout.current
    val iconSize = when (layout) {
        WindowLayout.WIDE -> Size.dockIcon
        WindowLayout.TALL -> Size.dockIconTall
        WindowLayout.NARROW -> Size.dockIconNarrow
    }
    Column(
        modifier
            .widthIn(min = Size.touchMin)
            .tappable(onClick)
            .clip(RoundedCornerShape(Radius.md)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        Box(
            Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(Radius.lg))
                .background(if (emphasized) c.accentFill else c.surface1),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (emphasized) c.onAccent else c.accent,
                modifier = Modifier.size(Size.iconLg),
            )
        }
        Text(
            label,
            style = AutoLogTheme.windowType.dockLabel,
            color = if (emphasized) c.accent else c.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
