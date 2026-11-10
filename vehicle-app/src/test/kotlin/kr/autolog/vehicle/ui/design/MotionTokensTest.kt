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

    @Test
    fun `스프링 세 가지 — smooth·quick은 넘침 없음, bouncy만 살짝 튄다`() {
        val m = MotionTokens.Standard
        val smooth = m.smooth<Float>() as SpringSpec<Float>
        val quick = m.quick<Float>() as SpringSpec<Float>
        val bouncy = m.bouncy<Float>() as SpringSpec<Float>
        assertEquals(1f, smooth.dampingRatio)
        assertEquals(320f, smooth.stiffness)
        assertEquals(1f, quick.dampingRatio)
        assertTrue(quick.stiffness > smooth.stiffness) // 사라짐은 나타남보다 빨리 끝난다
        assertTrue(bouncy.dampingRatio < 1f)
    }

    @Test
    fun `정차·애니메이션 줄이기·스크린샷이면 스프링 셋 모두 즉시`() {
        for (m in listOf(MotionTokens.Static, MotionTokens.Still)) {
            assertTrue(m.smooth<Float>() is SnapSpec<*>)
            assertTrue(m.quick<Float>() is SnapSpec<*>)
            assertTrue(m.bouncy<Float>() is SnapSpec<*>)
        }
    }
}
