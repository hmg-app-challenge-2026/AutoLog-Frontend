package kr.autolog.vehicle.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.nav.Route
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalNavAnimatedScope
import kr.autolog.vehicle.ui.design.LocalSharedTransitionScope
import kr.autolog.vehicle.ui.history.HistoryRoute
import kr.autolog.vehicle.ui.home.HomeRoute
import kr.autolog.vehicle.ui.profile.ProfilePickerRoute
import kr.autolog.vehicle.ui.receipt.FuelEntryRoute
import kr.autolog.vehicle.ui.receipt.ReceiptQrRoute
import kr.autolog.vehicle.ui.report.ReportRoute
import kr.autolog.vehicle.ui.result.TripResultRoute
import kr.autolog.vehicle.ui.settings.SettingsRoute

/** 주차(FULL) 층: 백스택 맨 위 화면. 전환은 motion.screen, 프로필→오른쪽 위 배지는 motion.hero. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullApp(graph: AppGraph) {
    val stack by graph.navigator.backStack.collectAsStateWithLifecycle()
    val motion = AutoLogTheme.motion
    val stores: RouteViewModelStores = viewModel()
    // 스택에서 빠진 화면의 ViewModel은 지운다(전환 애니메이션이 끝난 뒤 다음 구성에서)
    LaunchedEffect(stack) { stores.retainOnly(stack) }
    BackHandler(enabled = stack.size > 1) { graph.navigator.pop() }
    SharedTransitionLayout {
        AnimatedContent(
            targetState = stack.last(),
            transitionSpec = {
                (
                    fadeIn(motion.fadeStandard()) +
                        scaleIn(motion.gentle(), initialScale = 0.98f) +
                        slideInHorizontally(motion.gentle()) { it / 25 }
                    ) togetherWith fadeOut(motion.fadeQuick())
            },
            label = "route",
        ) { route ->
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                LocalNavAnimatedScope provides this,
                // 화면마다 자기 ViewModel 저장소 — 화면의 viewModel {}은 이 화면이 스택에 있는 동안만 산다
                LocalViewModelStoreOwner provides remember(route) { stores.ownerFor(route) },
            ) {
                if (route.showsDriverBadge()) {
                    // 오른쪽 위 운전자 배지 → 탭하면 운전자 선택
                    DriverBadgeScaffold(onClick = { graph.navigator.push(Route.ProfilePicker) }) { RouteContent(route) }
                } else {
                    RouteContent(route)
                }
            }
        }
    }
}

/**
 * 오른쪽 위 운전자 배지를 얹는 화면인가(디자인 언어 11장 DriverBadge). 배지를 빼는 화면은 여기 한곳에 적는다:
 * 운전자 선택(배지가 여는 화면 자체) · 운행 결과(D-27 — 게임 결과처럼 결과 하나에 집중, 제목이 가운데).
 */
private fun Route.showsDriverBadge(): Boolean = this != Route.ProfilePicker && this !is Route.TripResult

@Composable
private fun RouteContent(route: Route) {
    when (route) {
        Route.ProfilePicker -> ProfilePickerRoute()
        Route.Home -> HomeRoute()
        is Route.TripResult -> TripResultRoute(route.tripId, route.mode)
        Route.ReceiptQr -> ReceiptQrRoute()
        is Route.FuelEntry -> FuelEntryRoute(route.receiptId)
        Route.Report -> ReportRoute()
        Route.History -> HistoryRoute()
        Route.Settings -> SettingsRoute()
    }
}
