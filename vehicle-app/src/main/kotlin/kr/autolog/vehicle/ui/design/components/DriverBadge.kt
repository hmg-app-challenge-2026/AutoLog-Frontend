package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.sharedAvatar

/**
 * 디자인 언어 11장 DriverBadge — 넷플릭스처럼 화면 오른쪽 위에 네모 프로필 아이콘 + "○○님".
 * 운전자 이름은 앱 전체에서 이 자리에만 보인다. [onClick]이 null이면 표시 전용(Peek).
 */
@Composable
fun DriverBadge(userId: String?, name: String?, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    Row(
        modifier
            .heightIn(min = Size.touchMin)
            .tappable(onClick)
            .clip(RoundedCornerShape(Radius.md))
            .padding(horizontal = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        if (userId != null && name != null) {
            Avatar(userId, name, Size.avatarChip, Modifier.sharedAvatar(userId))
            Text(
                stringResource(R.string.driver_honorific, name),
                style = AutoLogTheme.type.label,
                color = c.textPrimary,
            )
        } else {
            IconAvatar(Icons.Rounded.Person, Size.avatarChip)
            Text(stringResource(R.string.home_choose_driver), style = AutoLogTheme.type.label, color = c.accent)
        }
    }
}
