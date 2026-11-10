package kr.autolog.vehicle.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

/** 창 크기별 글자 역할 표를 고정한다 — 넓은 창과 Pleos 1:1 분할 한 칸은 같고, 세로로 긴 창만 일부가 다르다(디자인 언어 6.2). */
class WindowTypeTest {
    private val t = autoLogType()

    @Test
    fun `넓은 창 — 화면 제목 display, 팝업 제목 headline, 결과 메인 화면 버튼은 caption`() {
        val w = windowTypeOf(WindowLayout.WIDE, t)
        assertEquals(t.display, w.screenTitle)
        assertEquals(t.headline, w.modalTitle)
        assertEquals(t.title, w.resultClassification)
        assertEquals(t.caption, w.resultHomeButton)
    }

    @Test
    fun `분할 화면 한 칸은 넓은 창과 같은 글자 — 실제 크기 1280×1268이라 줄일 이유가 없다`() {
        assertEquals(windowTypeOf(WindowLayout.WIDE, t), windowTypeOf(WindowLayout.NARROW, t))
    }

    @Test
    fun `세로 창 — 글자는 넓은 창과 같고 결과 분류·버튼만 label`() {
        val w = windowTypeOf(WindowLayout.WIDE, t)
        val tall = windowTypeOf(WindowLayout.TALL, t)
        assertEquals(w.copy(resultClassification = t.label, resultHomeButton = t.label), tall)
    }
}
