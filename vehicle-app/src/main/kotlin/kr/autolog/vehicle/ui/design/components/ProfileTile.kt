package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic
import kr.autolog.vehicle.ui.design.sharedAvatar

/** 디자인 언어 11장 ProfileTile. 1탭 = 선택 + 진입(조작 최소화 2.6). */
@Composable
fun ProfileTile(
    userId: String,
    name: String,
    dept: String?,
    scheduled: Boolean,
    recent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = Size.profileTileNarrow,
    /** shared element 키. 같은 사람이 한 화면에 두 번 나오면 한쪽은 null로 준다(색은 userId로 유지). */
    sharedKey: String? = userId,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Column(
        modifier
            .width(tileSize)
            .pressFeedback(interaction)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.Button) {
                view.confirmHaptic()
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        // 배지는 아바타 아래 가장자리에 걸친다 — 이니셜을 가리지 않는다
        Box(contentAlignment = Alignment.BottomCenter) {
            val ring =
                if (scheduled) {
                    Modifier.border(Size.ring, c.accent, RoundedCornerShape(Radius.xl)).padding(Space.s2)
                } else {
                    Modifier.padding(Space.s2)
                }
            // 아래 여백(배지 자리)은 링 바깥에 둔다 — 링이 아바타와 같은 정사각형으로 남게
            val framed = Modifier.padding(bottom = Space.s3).then(ring)
            Avatar(userId, name, tileSize - Space.s4, if (sharedKey != null) framed.sharedAvatar(sharedKey) else framed)
            if (scheduled || recent) {
                TileBadge(
                    stringResource(if (scheduled) R.string.badge_scheduled else R.string.badge_recent),
                    emphasized = scheduled,
                )
            }
        }
        Text(name, style = t.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (dept != null) Text(dept, style = t.caption, color = c.textSecondary, maxLines = 1)
    }
}

/** 타일 배지(배차·최근). 배차는 강조(accentFill + 흰 글씨 4.7:1), 최근은 surface3 + textSecondary. */
@Composable
private fun TileBadge(text: String, emphasized: Boolean) {
    val c = AutoLogTheme.colors
    Text(
        text,
        style = AutoLogTheme.type.caption,
        color = if (emphasized) c.onAccent else c.textSecondary,
        modifier =
        Modifier
            .clip(CircleShape)
            .background(if (emphasized) c.accentFill else c.surface3)
            .padding(horizontal = Space.s3, vertical = Space.s1),
    )
}
