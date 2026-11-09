package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space

/** 값이 바뀌었을 때 새 숫자가 이 투명도에서 1로 돌아온다(motion.fadeQuick). 정지형이면 즉시 교체. */
private const val NUMBER_CHANGE_ALPHA = 0.4f

/**
 * 숫자 + 단위 + 라벨. [value]가 null이면 숫자 대신 [unavailableText](추정값을 그리지 않는다).
 * 숫자는 항상 Text로 그려 베이스라인 정렬을 유지한다(AnimatedContent는 베이스라인이 없고 자기 영역을 자른다).
 */
@Composable
fun HeroMetric(
    value: String?,
    unit: String,
    label: String?,
    unavailableText: String,
    modifier: Modifier = Modifier,
    large: Boolean = true,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val motion = AutoLogTheme.motion
    Column(modifier) {
        if (label != null) Text(label, style = t.body, color = c.textSecondary)
        if (value == null) {
            Text(unavailableText, style = t.title, color = c.warning)
        } else {
            val alpha = remember { Animatable(1f) }
            val shown = remember { mutableListOf(value) }
            LaunchedEffect(value) {
                if (shown.last() != value) {
                    shown[0] = value
                    if (!motion.isStatic) {
                        alpha.snapTo(NUMBER_CHANGE_ALPHA)
                        alpha.animateTo(1f, motion.fadeQuick())
                    }
                }
            }
            Row {
                Text(
                    value,
                    style = if (large) t.hero else t.heroSmall,
                    color = c.textPrimary,
                    modifier = Modifier.alignByBaseline().graphicsLayer { this.alpha = alpha.value },
                )
                Spacer(Modifier.width(Space.s3))
                Text(unit, style = t.headline, color = c.textTertiary, modifier = Modifier.alignByBaseline())
            }
        }
    }
}
