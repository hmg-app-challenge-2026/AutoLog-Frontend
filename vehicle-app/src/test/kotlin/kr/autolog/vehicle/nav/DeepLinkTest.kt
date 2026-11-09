package kr.autolog.vehicle.nav

import kr.autolog.vehicle.data.usage.UsageTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {
    @Test
    fun `알림 extra — 목적 확인은 운행 id가 있어야 하고 모르는 경로는 무시`() {
        assertEquals(Route.ReviewTrip("t-1"), deepLinkRoute(DeepLinkKeys.ROUTE_REVIEW, "t-1"))
        assertNull(deepLinkRoute(DeepLinkKeys.ROUTE_REVIEW, null))
        assertNull(deepLinkRoute(DeepLinkKeys.ROUTE_REVIEW, " "))
        assertNull(deepLinkRoute("unknown", "t-1"))
        assertNull(deepLinkRoute(null, null))
    }

    @Test
    fun `주차 중 알림을 누르면 Home 위에 목적 확인이 열린다`() {
        val nav = Navigator(Route.Home)
        nav.onTierChanged(UsageTier.FULL)
        nav.push(Route.Settings)

        nav.handleDeepLink(Route.ReviewTrip("t-1"))

        assertEquals(listOf(Route.Home, Route.ReviewTrip("t-1")), nav.backStack.value)
    }

    @Test
    fun `주행 중 알림은 기억했다가 주차하면 연다 — 운전 중엔 화면이 없다`() {
        val nav = Navigator(Route.Home)
        nav.onTierChanged(UsageTier.BACKGROUND)

        nav.handleDeepLink(Route.ReviewTrip("t-1"))
        assertEquals(listOf(Route.Home), nav.backStack.value)
        nav.onTierChanged(UsageTier.PEEK)
        assertEquals(listOf(Route.Home), nav.backStack.value)
        nav.onTierChanged(UsageTier.FULL)

        assertEquals(listOf(Route.Home, Route.ReviewTrip("t-1")), nav.backStack.value)
    }

    @Test
    fun `앱 시작 직후(단계 모름) 들어온 알림도 주차가 확인되면 연다`() {
        val nav = Navigator(Route.ProfilePicker)

        nav.handleDeepLink(Route.ReviewTrip("t-1"))
        assertEquals(listOf(Route.ProfilePicker), nav.backStack.value)
        nav.onTierChanged(UsageTier.FULL)

        assertEquals(listOf(Route.Home, Route.ReviewTrip("t-1")), nav.backStack.value)
    }
}
