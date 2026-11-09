package kr.autolog.vehicle.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.autolog.vehicle.data.usage.UsageTier

/** 스펙 D-06·6.10. 단계 규칙을 한 곳에서 처리한다. */
class Navigator(start: Route = Route.ProfilePicker) {
    private val _backStack = MutableStateFlow(listOf(start))
    val backStack: StateFlow<List<Route>> = _backStack.asStateFlow()
    private var tier = UsageTier.FULL

    val current: Route get() = _backStack.value.last()

    fun push(route: Route): Boolean {
        if (tier != UsageTier.FULL) return false
        _backStack.value = _backStack.value + route
        return true
    }

    fun replaceAll(route: Route): Boolean {
        if (tier != UsageTier.FULL) return false
        _backStack.value = listOf(route)
        return true
    }

    fun pop(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.value = _backStack.value.dropLast(1)
        return true
    }

    /** FULL에서 내려가면 [Home]만 남긴다(스펙 6.9 3번). 올라올 때는 아무것도 되살리지 않는다. */
    fun onTierChanged(newTier: UsageTier) {
        if (newTier != UsageTier.FULL && (tier == UsageTier.FULL || _backStack.value != listOf(Route.Home))) {
            _backStack.value = listOf(Route.Home)
        }
        tier = newTier
    }
}
