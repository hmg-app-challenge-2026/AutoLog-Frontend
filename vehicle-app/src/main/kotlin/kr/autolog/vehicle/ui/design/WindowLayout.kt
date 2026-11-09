package kr.autolog.vehicle.ui.design

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** 스펙 6.8. 임계값은 잠정값(V-01) — FormFactorConsole 측정 후 고친다. */
enum class WindowLayout { NARROW, WIDE, TALL }

const val WIDE_MIN_ASPECT = 1.25f
const val WIDE_MIN_WIDTH_DP = 840f
const val TALL_MAX_ASPECT = 0.70f // 0.80이면 분할 화면 한 칸(≈504×640, 0.79)까지 TALL로 잡혀 바꿈(2026-11-09)

fun windowLayoutOf(widthDp: Float, heightDp: Float): WindowLayout {
    val aspect = widthDp / heightDp
    return when {
        aspect >= WIDE_MIN_ASPECT && widthDp >= WIDE_MIN_WIDTH_DP -> WindowLayout.WIDE
        aspect <= TALL_MAX_ASPECT -> WindowLayout.TALL
        else -> WindowLayout.NARROW
    }
}

/** 디자인 언어 6.3. 한국 좌핸들 = START. */
enum class DriverSide { START, END }

val LocalWindowLayout = compositionLocalOf { WindowLayout.NARROW }
val LocalDriverSide = staticCompositionLocalOf { DriverSide.START }

/**
 * 화면 바깥 여백(디자인 언어 6.4) — 창 크기마다 하나. 화면 콘텐츠와 운전자 배지가 같은 값을 써서 오른쪽 위 모서리가 맞물린다.
 * 화면은 이 값을 직접 계산하지 않는다.
 */
@Composable
fun screenMargin(): Dp = when (LocalWindowLayout.current) {
    WindowLayout.WIDE -> Space.s7
    WindowLayout.TALL -> Space.tallMargin
    WindowLayout.NARROW -> Space.s6
}

/** 현재 창 크기로 [LocalWindowLayout]을 계산해 내려 준다. 루트와 스크린샷 테스트가 쓴다. */
@Composable
fun WindowLayoutHost(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val layout = windowLayoutOf(maxWidth.value, maxHeight.value)
        CompositionLocalProvider(LocalWindowLayout provides layout) { content() }
    }
}
