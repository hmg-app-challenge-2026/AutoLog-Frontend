package kr.autolog.vehicle.ui.design

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTokensTest {
    @Test
    fun `누름 — 주차는 살짝 줄었다가 snappy로 복귀, 정지형은 줄지 않는다`() {
        assertEquals(0.97f, MotionTokens.Standard.pressScale)
        assertEquals(1f, MotionTokens.Static.pressScale)
        assertTrue(MotionTokens.Static.snappy<Float>() is SnapSpec<*>)
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
