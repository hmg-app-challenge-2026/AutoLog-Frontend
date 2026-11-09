package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 StatusLine — 화면 맨 위 상태 한 줄. 배경 없는 점 + 글자: `● 기록 중 · 35분 경과`.
 * 점 색이 상태를 말하고(기록 중 `success`, 대기 `textTertiary`), 글자는 본문색, [detail]은 회색 보조.
 * 알약 배경이 없어 아래 주인공 숫자와 경쟁하지 않으면서, 맨 위 왼쪽 자리라 가장 먼저 읽힌다. 누를 수 없다.
 */
@Composable
fun StatusLine(text: String, dot: Color, modifier: Modifier = Modifier, detail: String? = null) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Box(Modifier.size(Size.statusDot).clip(CircleShape).background(dot))
        Text(text, style = t.label, color = c.textPrimary)
        if (detail != null) {
            Text("·", style = t.label, color = c.textTertiary)
            Text(detail, style = t.body, color = c.textSecondary)
        }
    }
}
