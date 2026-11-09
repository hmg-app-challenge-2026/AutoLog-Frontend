package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

data class BannerAction(val text: String, val onClick: () -> Unit)

/** 이보다 좁으면 버튼을 문장 아래로 내린다(문장이 3줄 이상으로 눌리지 않게). */
private val BannerInlineMinWidth = 720.dp

/** 화면 상단 전폭 알림. 버튼 최대 2개(첫 번째가 주 동작). */
@Composable
fun Banner(text: String, actions: List<BannerAction>, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    BoxWithConstraints(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.md)).background(c.surface2).padding(Space.s5),
    ) {
        val message =
            @Composable { m: Modifier ->
                Row(
                    m,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s4),
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier.size(Size.icon),
                    )
                    Text(text, style = AutoLogTheme.type.body, color = c.textPrimary)
                }
            }
        val buttons =
            @Composable {
                actions.take(2).forEachIndexed { i, a ->
                    if (i ==
                        0
                    ) {
                        PrimaryButton(a.text, a.onClick, height = Size.buttonMd)
                    } else {
                        SecondaryButton(a.text, a.onClick)
                    }
                }
            }
        if (maxWidth >= BannerInlineMinWidth) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s4),
            ) {
                message(Modifier.weight(1f))
                buttons()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                message(Modifier)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) { buttons() }
            }
        }
    }
}
