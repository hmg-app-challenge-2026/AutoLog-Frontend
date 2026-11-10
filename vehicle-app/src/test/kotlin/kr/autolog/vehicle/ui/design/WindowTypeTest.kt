package kr.autolog.vehicle.ui.design

import org.junit.Assert.assertEquals
import org.junit.Test

/** 창 크기별 글자 역할 표를 고정한다 — 좁은 창에서 한 단계 작아지는 것만 다르다(디자인 언어 6.2). */
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
    fun `좁은 창 — 한 단계씩 작고 타일 부서는 글자 하한 fine`() {
        val n = windowTypeOf(WindowLayout.NARROW, t)
        assertEquals(t.headline, n.screenTitle)
        assertEquals(t.label, n.modalTitle)
        assertEquals(t.label, n.tileName)
        assertEquals(t.caption, n.tileAction)
        assertEquals(t.fine, n.tileCaption)
        assertEquals(t.caption, n.dockLabel)
        assertEquals(t.label, n.resultTitle)
    }

    @Test
    fun `세로 창 — 글자는 넓은 창과 같고 결과 분류·버튼만 label`() {
        val w = windowTypeOf(WindowLayout.WIDE, t)
        val tall = windowTypeOf(WindowLayout.TALL, t)
        assertEquals(w.copy(resultClassification = t.label, resultHomeButton = t.label), tall)
    }
}
