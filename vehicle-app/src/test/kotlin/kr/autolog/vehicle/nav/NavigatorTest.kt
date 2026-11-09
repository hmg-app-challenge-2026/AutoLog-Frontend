package kr.autolog.vehicle.nav

import kr.autolog.vehicle.data.usage.UsageTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NavigatorTest {
    @Test
    fun `주차에서 화면을 쌓고 뒤로 갈 수 있다`() {
        val nav = Navigator(Route.Home)

        nav.push(Route.ProfilePicker)
        nav.pop()

        assertEquals(listOf(Route.Home), nav.backStack.value)
    }

    @Test
    fun `마지막 화면은 뒤로 가지 않는다`() {
        val nav = Navigator(Route.Home)

        assertFalse(nav.pop())
        assertEquals(Route.Home, nav.current)
    }

    @Test
    fun `영수증이 오면 QR을 입력 화면으로 교체한다`() {
        val nav = Navigator(Route.Home)
        nav.push(Route.ReceiptQr)

        nav.replaceTop(Route.FuelEntry("r-1"))

        assertEquals(listOf(Route.Home, Route.FuelEntry("r-1")), nav.backStack.value)
    }

    @Test
    fun `주차에서 정차로 내려가면 백스택이 Home 하나로 줄어든다`() {
        val nav = Navigator(Route.Home)
        nav.push(Route.ProfilePicker)

        nav.onTierChanged(UsageTier.PEEK)

        assertEquals(listOf(Route.Home), nav.backStack.value)
    }

    @Test
    fun `주차가 아니면 화면을 열 수 없다`() {
        val nav = Navigator(Route.Home)
        nav.onTierChanged(UsageTier.BACKGROUND)

        assertFalse(nav.push(Route.ProfilePicker))
        assertFalse(nav.replaceAll(Route.ProfilePicker))
        assertEquals(listOf(Route.Home), nav.backStack.value)
    }

    @Test
    fun `다시 주차해도 닫힌 화면을 되살리지 않는다`() {
        val nav = Navigator(Route.ProfilePicker)
        nav.onTierChanged(UsageTier.PEEK)

        nav.onTierChanged(UsageTier.FULL)

        assertEquals(listOf(Route.Home), nav.backStack.value)
    }
}
