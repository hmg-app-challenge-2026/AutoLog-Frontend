package kr.autolog.vehicle.ui.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * [main] 아래에 [tail]을 [gap]만큼 띄워 놓되, **둘 다 들어갈 높이일 때만** [tail]을 그린다 — 모자라면 [tail]을 통째로 뺀다.
 * 스크롤도 터치도 없는 화면(Peek)에서 아래 줄이 반쯤 잘려 읽히지 않는 것을 막는다(스펙 7.0 "넘치면 ⑤ → ④ 순으로 생략").
 * 높이 제한이 없는 부모 안에서는 늘 둘 다 그린다.
 */
@Composable
fun FitOrDrop(gap: Dp, main: @Composable () -> Unit, tail: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Layout(contents = listOf(main, tail), modifier = modifier) { (mainMeasurables, tailMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val mains = mainMeasurables.map { it.measure(loose) }
        val mainHeight = mains.sumOf { it.height }
        val tails = tailMeasurables.map { it.measure(loose.copy(maxHeight = Constraints.Infinity)) }
        val tailHeight = tails.sumOf { it.height }
        val gapPx = gap.roundToPx()
        val fits = !constraints.hasBoundedHeight || mainHeight + gapPx + tailHeight <= constraints.maxHeight
        val shownTails = if (fits && tails.isNotEmpty()) tails else emptyList()
        val width = (mains + shownTails).maxOfOrNull { it.width }?.coerceIn(constraints.minWidth, constraints.maxWidth)
            ?: constraints.minWidth
        val height = (mainHeight + if (shownTails.isEmpty()) 0 else gapPx + tailHeight)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var y = 0
            mains.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            if (shownTails.isNotEmpty()) y += gapPx
            shownTails.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}
