package kr.autolog.vehicle.ui.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kr.autolog.vehicle.data.usage.UsageTier
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import kr.autolog.vehicle.ui.peek.PeekRoute

/** FULL 진입 직후 오조작 방지(디자인 언어 12장). */
const val FULL_ENTRY_GUARD_MS = 300L

/**
 * 스펙 6.9 2번: FULL → 전체 앱 / PEEK → Peek 패널만 / BACKGROUND → 물러남.
 * 입력 잠금 = tier != FULL 또는 FULL 진입 직후 300 ms.
 */
@Composable
fun AutoLogRoot(graph: AppGraph, onRequestBackground: () -> Unit) {
    val tier by graph.usage.tier.collectAsStateWithLifecycle()
    var entryGuard by remember { mutableStateOf(false) }
    LaunchedEffect(tier) {
        graph.navigator.onTierChanged(tier)
        when (tier) {
            UsageTier.BACKGROUND -> onRequestBackground()
            UsageTier.FULL -> {
                entryGuard = true
                delay(FULL_ENTRY_GUARD_MS)
                entryGuard = false
            }
            UsageTier.PEEK -> Unit
        }
    }
    CompositionLocalProvider(LocalAppGraph provides graph) {
        AutoLogTheme(staticMotion = tier != UsageTier.FULL) {
            CompositionLocalProvider(LocalInputLocked provides (tier != UsageTier.FULL || entryGuard)) {
                Box(
                    Modifier.fillMaxSize().background(
                        AutoLogTheme.colors.bg,
                    ).windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    WindowLayoutHost {
                        when (tier) {
                            UsageTier.FULL -> FullApp(graph)
                            UsageTier.PEEK -> PeekRoute()
                            UsageTier.BACKGROUND -> Unit
                        }
                    }
                    graph.demoOverlay?.invoke()
                }
            }
        }
    }
}
