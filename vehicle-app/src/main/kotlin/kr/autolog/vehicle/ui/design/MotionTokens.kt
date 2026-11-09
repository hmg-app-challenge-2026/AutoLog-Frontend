package kr.autolog.vehicle.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/**
 * 디자인 언어 8.2·8.4. [isStatic]은 tier != FULL(정차 Peek) 또는 시스템 "애니메이션 줄이기"일 때 — 화면 전환·등장 모션이 멈춘다.
 * [ambient]는 그와 별개인 **상태 표시용 반복 모션**(경로 화살표 흐름 등) 허용 여부. Peek에서도 켜지고,
 * 시스템 "애니메이션 줄이기"이거나 스크린샷처럼 정지 화면이 필요할 때만 꺼진다.
 * 컴포넌트는 이 토큰만 쓰므로 단계가 바뀌면 자동으로 맞는 형태가 된다.
 */
@Immutable
class MotionTokens private constructor(val isStatic: Boolean, val ambient: Boolean) {
    fun <T> standard(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 0.9f, stiffness = 400f)

    fun <T> gentle(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 1.0f, stiffness = 200f)

    fun <T> snappy(): FiniteAnimationSpec<T> = if (isStatic) snap() else spring(dampingRatio = 0.85f, stiffness = 700f)

    /** 숫자 교체·작은 요소. 정지형에서도 FULL→PEEK 전환 한 번은 이 값까지 허용. */
    fun <T> fadeQuick(): FiniteAnimationSpec<T> = tween(durationMillis = 150, easing = LinearOutSlowInEasing)

    fun <T> fadeStandard(): FiniteAnimationSpec<T> =
        if (isStatic) tween(150, easing = LinearOutSlowInEasing) else tween(220, easing = FastOutSlowInEasing)

    val pressScale: Float get() = if (isStatic) 1f else 0.97f

    /** 흐름 표시(경로 화살표) 한 바퀴 시간. 급하지 않게 — 숨 쉬는 속도보다 조금 빠른 정도. */
    val flowPeriodMs: Int get() = 1_400

    companion object {
        val Standard = MotionTokens(isStatic = false, ambient = true)
        val Static = MotionTokens(isStatic = true, ambient = true)

        /** 반복 모션까지 모두 멈춤 — 시스템 "애니메이션 줄이기", 스크린샷. */
        val Still = MotionTokens(isStatic = true, ambient = false)
    }
}
