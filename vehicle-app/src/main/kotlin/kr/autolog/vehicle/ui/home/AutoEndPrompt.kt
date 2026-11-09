package kr.autolog.vehicle.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.DriverSide
import kr.autolog.vehicle.ui.design.LocalDriverSide
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.ScrimSheet
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.durationText

/**
 * 스펙 7.2.1 / 디자인 언어 2.6·11장. 아무것도 안 누르면 끝나는 것이 기본값이므로
 * 기본값에서 벗어나는 [계속 기록]이 PrimaryButton(운전자 쪽), 기본값을 앞당기는 [기록 종료]가 Secondary.
 */
@Composable
fun AutoEndPrompt(state: AutoEndUi, tripKm: String?, destination: String?, onKeep: () -> Unit, onEnd: () -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val motion = AutoLogTheme.motion
    val progress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = if (motion.isStatic) snap() else tween(durationMillis = 1_000, easing = LinearEasing),
        label = "autoEndProgress",
    )
    ScrimSheet {
        Text(stringResource(R.string.autoend_title), style = t.headline, color = c.textPrimary)
        Spacer(Modifier.height(Space.s3))
        // 거리와 목적지를 나눈다 — 한 줄로 이으면 좁은 창에서 목적지가 단어 중간에서 끊긴다
        tripKm?.let { Text("$it ${stringResource(R.string.unit_km)}", style = t.title, color = c.textPrimary) }
        destination?.let { Text(it, style = t.body, color = c.textSecondary) }
        Spacer(Modifier.height(Space.s3))
        Text(
            stringResource(R.string.autoend_remaining, durationText(state.remainingSec)),
            style = t.body,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(Space.s4))
        Box(Modifier.fillMaxWidth().height(Size.progressBar).clip(CircleShape).background(c.surface3)) {
            Box(Modifier.fillMaxWidth(progress).height(Size.progressBar).clip(CircleShape).background(c.accent))
        }
        Spacer(Modifier.height(Space.s6))
        val keep: @Composable (
            Modifier,
        ) -> Unit = { m -> PrimaryButton(stringResource(R.string.autoend_keep), onKeep, m) }
        val end: @Composable (Modifier) -> Unit = { m ->
            SecondaryButton(stringResource(R.string.autoend_end), onEnd, m, height = Size.buttonLg)
        }
        if (LocalWindowLayout.current == WindowLayout.WIDE) {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                if (LocalDriverSide.current == DriverSide.START) {
                    keep(Modifier.weight(1f))
                    end(Modifier.weight(1f))
                } else {
                    end(Modifier.weight(1f))
                    keep(Modifier.weight(1f))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                end(Modifier.fillMaxWidth())
                keep(Modifier.fillMaxWidth())
            }
        }
    }
}
