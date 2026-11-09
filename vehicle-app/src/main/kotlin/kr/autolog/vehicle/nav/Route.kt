package kr.autolog.vehicle.nav

/** 백스택의 모든 화면은 주차(FULL) 전용이다(스펙 6.10). Peek은 Route가 아니라 루트가 그리는 층. */
sealed interface Route {
    data object ProfilePicker : Route

    data object Home : Route

    data class ReviewTrip(val tripId: String) : Route

    data object ReceiptQr : Route

    data class ReceiptReview(val receiptId: String) : Route

    data class FuelEntry(val tripId: String?) : Route

    data object Report : Route

    data object History : Route

    data object Settings : Route
}
