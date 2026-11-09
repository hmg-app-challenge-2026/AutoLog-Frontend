package kr.autolog.vehicle.ui.design.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFlowTest {
    @Test
    fun `정지 그림은 위에서 아래로 짙어져 방향이 읽힌다`() {
        val a = (0..2).map { alphaOf(it, -1f) }

        assertTrue(a[0] < a[1] && a[1] < a[2])
        assertEquals(1f, a[2], 0.001f)
    }

    @Test
    fun `빛은 위에서 아래로 흐른다 — 가장 밝은 갈매기가 시간에 따라 아래로 내려간다`() {
        fun brightest(phase: Float) = (0..2).maxBy { alphaOf(it, phase) }

        assertEquals(0, brightest(0.12f))
        assertEquals(1, brightest(0.33f))
        assertEquals(2, brightest(0.55f))
    }

    @Test
    fun `빛이 지나간 뒤에도 갈매기는 완전히 사라지지 않는다`() {
        (0..2).forEach { i -> assertTrue(alphaOf(i, 0.99f) > 0.1f) }
    }
}
