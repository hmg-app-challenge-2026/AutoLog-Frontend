package kr.autolog.vehicle.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

class TextsTest {
    @Test
    fun `받침 없거나 ㄹ 받침이면 로, 그 밖의 받침이면 으로`() {
        assertEquals("회의로", withRo("회의"))
        assertEquals("지점 이동으로", withRo("지점 이동"))
        assertEquals("외근으로", withRo("외근"))
        assertEquals("거래처 방문으로", withRo("거래처 방문"))
        assertEquals("출발로", withRo("출발"))
        assertEquals("A(으)로", withRo("A"))
    }
}
