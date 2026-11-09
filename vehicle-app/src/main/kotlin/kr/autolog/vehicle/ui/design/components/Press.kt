package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kr.autolog.vehicle.ui.design.AutoLogTheme

/** 디자인 언어 8.2 press: 누름 0.97배 tween(90) / 뗌 spring.snappy. 정지형이면 크기 변화 없음. */
fun Modifier.pressFeedback(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val motion = AutoLogTheme.motion
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = if (pressed) tween(90) else motion.snappy(),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
