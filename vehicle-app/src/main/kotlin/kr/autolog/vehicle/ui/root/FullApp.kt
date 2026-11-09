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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.nav.Route
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalNavAnimatedScope
import kr.autolog.vehicle.ui.design.LocalSharedTransitionScope
import kr.autolog.vehicle.ui.home.HomeRoute
import kr.autolog.vehicle.ui.profile.ProfilePickerRoute

/** 주차(FULL) 층: 백스택 맨 위 화면. 전환은 motion.screen, 프로필→Home은 motion.hero. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FullApp(graph: AppGraph) {
    val stack by graph.navigator.backStack.collectAsStateWithLifecycle()
    val motion = AutoLogTheme.motion
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
            ) {
                when (route) {
                    Route.ProfilePicker -> ProfilePickerRoute()
                    Route.Home -> HomeRoute()
                }
            }
        }
    }
}
