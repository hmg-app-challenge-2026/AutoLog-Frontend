package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Box(contentAlignment = Alignment.TopEnd) {
            val ring =
                if (scheduled) {
                    Modifier.border(Size.ring, c.accent, RoundedCornerShape(Radius.xl)).padding(Space.s2)
                } else {
                    Modifier.padding(Space.s2)
                }
            Avatar(userId, name, tileSize - Space.s4, ring.sharedAvatar(userId))
            if (scheduled || recent) {
                StatusChip(
                    text = stringResource(if (scheduled) R.string.badge_scheduled else R.string.badge_recent),
                    tone = if (scheduled) ChipTone.ACCENT else ChipTone.NEUTRAL,
                )
            }
        }
        Text(name, style = t.title, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (dept != null) Text(dept, style = t.caption, color = c.textSecondary, maxLines = 1)
    }
}
