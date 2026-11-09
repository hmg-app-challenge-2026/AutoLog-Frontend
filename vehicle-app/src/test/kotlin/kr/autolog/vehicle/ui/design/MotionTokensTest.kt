package kr.autolog.vehicle.ui.design

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTokensTest {
    @Test
    fun `정차 이상 단계의 정지형 세트는 위치·크기 변화를 즉시 끝낸다`() {
        assertTrue(MotionTokens.Static.standard<Float>() is SnapSpec<*>)
        assertTrue(MotionTokens.Static.gentle<Float>() is SnapSpec<*>)
        assertEquals(1f, MotionTokens.Static.pressScale)
    }

    @Test
    fun `주차 단계는 오버슈트 없는 스프링을 쓴다`() {
        val spec = MotionTokens.Standard.standard<Float>() as SpringSpec<Float>
        assertEquals(0.9f, spec.dampingRatio)
        assertEquals(400f, spec.stiffness)
        assertEquals(0.97f, MotionTokens.Standard.pressScale)
    }
}
