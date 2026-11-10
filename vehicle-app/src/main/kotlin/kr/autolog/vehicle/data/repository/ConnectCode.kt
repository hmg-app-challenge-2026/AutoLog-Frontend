package kr.autolog.vehicle.data.repository

/**
 * 차량 연결 코드 규칙(F-01, F-40) — 운영 웹에서 발급한 6자리. 서버가 만드는 글자와 같아야 입력할 수 있다:
 * relay `Tokens.CODE_ALPHABET`("23456789ABCDEFGHJKLMNPQRSTUVWXYZ") · 앱 A `DeviceRepository.CODE`(`[2-9A-HJ-NP-Z]{6}`).
 * 헷갈리는 0·1·I·O는 쓰지 않는다. 백엔드 내부 패키지는 화면에서 import하지 않으므로 같은 값을 여기 둔다 —
 * 서버 규칙이 바뀌면 이 파일 하나만 고친다(`docs/integration/backend-link.md` G-01).
 */
object ConnectCode {
    const val LENGTH = 6
    const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    /** 입력할 수 있는 글자인가(소문자는 대문자로 본다). */
    fun accepts(key: Char): Boolean = key.uppercaseChar() in ALPHABET

    /** 키 하나를 반영한 새 코드. 쓸 수 없는 글자이거나 이미 [LENGTH]자면 그대로. */
    fun append(current: String, key: Char): String =
        if (current.length >= LENGTH || !accepts(key)) current else current + key.uppercaseChar()

    fun isComplete(code: String): Boolean = code.length == LENGTH && code.all(::accepts)
}
