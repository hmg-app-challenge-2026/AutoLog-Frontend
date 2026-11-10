package kr.autolog.vehicle.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 연결 코드 규칙이 서버와 같은지(relay Tokens.CODE_ALPHABET · 앱 A DeviceRepository.CODE `[2-9A-HJ-NP-Z]{6}`). */
class ConnectCodeTest {
    private val server = Regex("[2-9A-HJ-NP-Z]{6}")

    @Test
    fun `글자 집합이 서버 정규식과 같다 — 32자, 0·1·I·O 없음`() {
        assertEquals(32, ConnectCode.ALPHABET.length)
        ConnectCode.ALPHABET.chunked(ConnectCode.LENGTH).filter { it.length == ConnectCode.LENGTH }.forEach {
            assertTrue(it, server.matches(it))
        }
        "01IO".forEach { assertFalse(ConnectCode.accepts(it)) }
    }

    @Test
    fun `소문자는 대문자로, 6자리가 차면 더 받지 않는다`() {
        val code = "7kq3mzW".fold("") { acc, c -> ConnectCode.append(acc, c) }
        assertEquals("7KQ3MZ", code)
        assertTrue(ConnectCode.isComplete(code))
        assertTrue(server.matches(code))
    }

    @Test
    fun `모자라거나 쓸 수 없는 글자가 있으면 완성이 아니다`() {
        assertFalse(ConnectCode.isComplete("7KQ3M"))
        assertFalse(ConnectCode.isComplete("7KQ3M0"))
    }
}
