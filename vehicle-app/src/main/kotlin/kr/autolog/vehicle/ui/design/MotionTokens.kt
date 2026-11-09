package kr.autolog.vehicle.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * 디자인 언어 8.2·8.4. [Static]은 tier != FULL(정차 Peek) 또는 시스템 "애니메이션 줄이기"일 때 쓴다.
 * 컴포넌트는 이 토큰만 쓰므로 단계가 바뀌면 자동으로 정지형이 된다.
 */
@Immutable
class MotionTokens private constructor(val isStatic: Boolean) {
    fun <T> standard(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 0.9f, stiffness = 400f)

    fun <T> gentle(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 1.0f, stiffness = 200f)

    fun <T> snappy(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 0.85f, stiffness = 700f)

    /** 숫자 교체·작은 요소. 정지형에서도 FULL→PEEK 전환 한 번은 이 값까지 허용. */
    fun <T> fadeQuick(): FiniteAnimationSpec<T> = tween(durationMillis = 150, easing = LinearOutSlowInEasing)

    fun <T> fadeStandard(): FiniteAnimationSpec<T> =
        if (isStatic) tween(150, easing = LinearOutSlowInEasing) else tween(220, easing = FastOutSlowInEasing)

    val pressScale: Float get() = if (isStatic) 1f else 0.97f

    companion object {
        val Standard = MotionTokens(isStatic = false)
        val Static = MotionTokens(isStatic = true)
    }
}
