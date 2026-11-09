package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp

/**
 * 한 줄에 [count]개를 왼쪽부터 놓고, 다 안 들어가면 들어가는 만큼만 놓은 뒤 끝에 [more](숨은 개수)를 둔다.
 * 알림 띠처럼 "한 줄 고정 + 넘치면 외 N건"이 필요한 곳에 쓴다. 숨은 개수를 측정한 뒤에 [more]를 만들므로
 * 문구에 개수를 넣어도 된다(FlowRowOverflow는 이를 컴포지션 단계에서 읽을 수 없었다).
 * 숨은 항목은 화면에도 의미론(접근성)에도 없다 — 재는 사본만 의미론 없이 구성한다.
 */
@Composable
fun OverflowRow(
    count: Int,
    spacing: Dp,
    item: @Composable (index: Int) -> Unit,
    more: @Composable (hidden: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0)
        // 1) 재기만 하는 사본 — 의미론을 지워 화면 낭독기·테스트가 숨은 항목을 보지 않게 한다
        val widths = (0 until count).map { i ->
            subcompose("measure-$i") { Box(Modifier.clearAndSetSemantics {}) { item(i) } }.first().measure(loose).width
        }
        fun widthOf(n: Int) = widths.take(n).sum() + gap * (n - 1).coerceAtLeast(0)

        var shown = count
        var tail: Placeable? = null
        if (widthOf(count) > constraints.maxWidth) {
            // 끝의 "외 N건"까지 들어가는 가장 많은 개수를 찾는다
            for (n in count - 1 downTo 0) {
                val m = subcompose("more-$n") { more(count - n) }.first().measure(loose)
                if (widthOf(n) + (if (n > 0) gap else 0) + m.width <= constraints.maxWidth || n == 0) {
                    shown = n
                    tail = m
                    break
                }
            }
        }
        // 2) 실제로 보이는 것만 구성해 놓는다
        val placed =
            (0 until shown).map { i -> subcompose("show-$i") { item(i) }.first().measure(loose) } + listOfNotNull(tail)
        val height = placed.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            var x = 0
            placed.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width + gap
            }
        }
    }
}
