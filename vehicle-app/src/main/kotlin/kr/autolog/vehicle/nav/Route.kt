package kr.autolog.vehicle.nav

/** 백스택의 모든 화면은 주차(FULL) 전용이다(스펙 6.10). Peek은 Route가 아니라 루트가 그리는 층. */
sealed interface Route {
    data object ProfilePicker : Route

    data object Home : Route

    /** 운행 결과 카드(스펙 7.3, D-27) — 운행이 끝나면 자동으로, 알림 탭으로, 운행 기록 조회에서 연다. */
    data class TripResult(val tripId: String) : Route

    /** 주유 기록 추가의 기본 경로: 폰으로 영수증을 올릴 QR(스펙 7.4, D-20). */
    data object ReceiptQr : Route

    /**
     * 주유 기록 입력 창 하나(스펙 7.5, D-20). [receiptId]가 있으면 영수증 OCR 값이 채워진 채로,
     * null이면 빈 칸으로(QR 화면의 [직접 입력하기]) 열린다. 저장은 [추가하기] 한 번.
     */
    data class FuelEntry(val receiptId: String? = null) : Route

    data object Report : Route

    data object History : Route

    data object Settings : Route
}
