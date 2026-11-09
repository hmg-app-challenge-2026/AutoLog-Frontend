package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 ChoiceRow — 여러 개 중 하나를 고르는 **선택형 행**(운행 결과의 운행 목적).
 * 같은 폭의 행을 8 dp 간격으로 쌓아 한 묶음으로 읽히게 한다. 행 = `surface1` 면(`radius.md`, 76 dp) + 왼쪽 원 28 dp + 글자 `body`, 행 전체가 터치 대상.
 * 선택: `accent` 테두리(3 dp) + 원 가운데 `accent` 점. [onClick]이 null이면 **보기 전용**(누를 수 없고, 고르지 않은 행 글자는 흐리게).
 */
@Composable
fun ChoiceRow(text: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val ring = if (selected) c.accent else c.textTertiary
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchMin)
            .semantics { this.selected = selected }
            .tappable(onClick, role = Role.RadioButton)
            .clip(shape)
            .background(c.surface1)
            .then(if (selected) Modifier.border(Size.ring, c.accent, shape) else Modifier)
            .padding(horizontal = Space.s5),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s4),
    ) {
        Box(Modifier.size(Size.iconSm).border(Size.ring, ring, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(Size.statusDot).clip(CircleShape).background(c.accent))
        }
        val readOnlyOther = onClick == null && !selected
        Text(text, style = AutoLogTheme.type.body, color = if (readOnlyOther) c.textSecondary else c.textPrimary)
    }
}
