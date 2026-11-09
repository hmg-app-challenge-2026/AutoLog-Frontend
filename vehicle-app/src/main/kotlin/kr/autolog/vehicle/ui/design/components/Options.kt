package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/** 디자인 언어 11장 SegmentedPurpose의 선택지 하나. 선택되면 accent 링 + 체크. 고르면 바로 실행되는 곳에 쓴다. */
@Composable
fun OptionButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    val shape = RoundedCornerShape(Radius.lg)
    Row(
        modifier
            .heightIn(min = Size.buttonMd)
            .pressFeedback(interaction)
            .clip(shape)
            .background(if (selected) c.surface3 else c.surface2)
            .then(if (selected) Modifier.border(Size.ring, c.accent, shape) else Modifier)
            .clickable(interaction, null, enabled = !LocalInputLocked.current, role = Role.RadioButton) {
                view.confirmHaptic()
                onClick()
            }.padding(horizontal = Space.s5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s2),
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(Size.icon),
            )
        }
        Text(text, style = AutoLogTheme.type.label, color = c.textPrimary)
    }
}
