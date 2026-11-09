package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 RadioOption — 여러 개 중 하나를 고르는 한 줄(운행 결과의 "이번 운행의 목적").
 * 왼쪽 원(28 dp): 선택이면 `accent` 테두리 + 가운데 `accent` 점, 아니면 `textTertiary` 테두리. 오른쪽 글자 `body`. 줄 전체가 터치 대상(76 dp).
 * [onClick]이 null이면 **보기 전용**(누를 수 없고, 고르지 않은 줄은 흐리게) — 결과를 먼저 보여 주고 [수정하기]에서만 바꾸게 할 때.
 */
@Composable
fun RadioOption(text: String, selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val ring = if (selected) c.accent else c.textTertiary
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = Size.touchMin)
            .semantics { this.selected = selected }
            .tappable(onClick, role = Role.RadioButton),
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
