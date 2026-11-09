package kr.autolog.vehicle.nav

/**
 * 알림 탭 딥링크(스펙 6.10). 앱 A의 기록 알림이 `MainActivity`를 열 때 extra로 넣는 키.
 * 키 상수는 앱 A·프론트가 같은 값을 써야 해서 `core`에 정의해 달라고 요청했다(R-3) — 오면 이 상수를 그것으로 바꾼다.
 */
object DeepLinkKeys {
    const val ROUTE = "autolog.route"
    const val TRIP_ID = "autolog.tripId"

    /** [ROUTE] 값: 운행 결과(옛 목적 확인 — 앱 A와 맞춘 값이라 "review" 그대로). [TRIP_ID]가 함께 와야 한다. */
    const val ROUTE_TRIP_RESULT = "review"
}

/** extra 값 → 화면. 모르는 경로이거나 필요한 값이 없으면 null(무시). Android Intent와 분리해 단위 테스트한다. */
fun deepLinkRoute(route: String?, tripId: String?): Route? = when (route) {
    DeepLinkKeys.ROUTE_TRIP_RESULT -> tripId?.takeIf { it.isNotBlank() }?.let { Route.TripResult(it) }
    else -> null
}
