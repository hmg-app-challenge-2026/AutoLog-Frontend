package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 EdgeCountdownBar — 화면 맨 위 가장자리의 가는 막대(4 dp, `accent`, 양 끝 둥글게). 좌우 화면 끝에서 16 dp 띄운다
 * (2560 dp 화면 끝에서 끝까지 닿으면 넓대대해 보였다 — 2026-11-10 사용자 지시). 자동 진행까지 남은 비율(1 → 0)만큼
 * 왼쪽부터 채워 있고 **오른쪽 끝이 왼쪽으로 줄어든다**. 바탕 줄 없이 막대만 — 화면 위 정보와 경쟁하지 않는 시간 단서.
 * 1초마다 바뀌는 값을 1초 동안 선형으로 이어 그린다(Pleos 애니메이션 원칙 Linear, 상태 설명용). 정지형이면 바로 바뀐다.
 */
@Composable
fun EdgeCountdownBar(progress: Float, modifier: Modifier = Modifier) {
    val motion = AutoLogTheme.motion
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (motion.isStatic) snap() else tween(durationMillis = 1_000, easing = LinearEasing),
        label = "edgeCountdown",
    )
    Box(modifier.fillMaxWidth().padding(horizontal = Space.s4)) {
        Box(
            Modifier.fillMaxWidth(shown).height(Size.edgeBar).clip(CircleShape).background(AutoLogTheme.colors.accent),
        )
    }
}
