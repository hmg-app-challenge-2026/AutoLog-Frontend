package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size

/**
 * 디자인 언어 11장 CountdownBar — 자동 진행까지 남은 시간(1 → 0)을 줄어드는 막대로. 높이 8 dp, `surface3` 위 `accent`.
 * 1초마다 바뀌는 값을 1초 동안 선형으로 이어 그려 끊기지 않게 줄어든다(Pleos 애니메이션 원칙 Linear, 상태 설명용). 정지형이면 바로 바뀐다.
 * 자동 종료 팝업과 운행 결과 카드가 같이 쓴다.
 */
@Composable
fun CountdownBar(progress: Float, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val motion = AutoLogTheme.motion
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (motion.isStatic) snap() else tween(durationMillis = 1_000, easing = LinearEasing),
        label = "countdown",
    )
    Box(modifier.fillMaxWidth().height(Size.progressBar).clip(CircleShape).background(c.surface3)) {
        Box(Modifier.fillMaxWidth(shown).height(Size.progressBar).clip(CircleShape).background(c.accent))
    }
}
