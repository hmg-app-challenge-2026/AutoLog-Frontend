package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/** 지금 떠 있는 모달 수. 운전자 배지 등 화면 위 요소는 0이 아니면 숨는다(모달이 화면 전체를 덮어야 하므로). */
val LocalModalCount = compositionLocalOf<MutableIntState?> { null }

/** 아래에서 올라오는 시트 + scrim(디자인 언어 11장 Sheet). 단계가 FULL에서 내려가면 루트가 화면째 지운다. */
@Composable
fun ScrimSheet(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = AutoLogTheme.colors
    val modal = LocalModalCount.current
    DisposableEffect(modal) {
        modal?.let { it.intValue += 1 }
        onDispose { modal?.let { it.intValue -= 1 } }
    }
    // scrim은 뒤 화면으로 터치가 새지 않게 삼킨다(클릭 의미론은 없음 — Peek 검사에 걸리지 않는다)
    Box(
        modifier.fillMaxSize().background(c.scrim).pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = Size.sheetMaxWidth)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg))
                .background(c.surface2)
                .padding(Space.s6),
            content = content,
        )
    }
}
