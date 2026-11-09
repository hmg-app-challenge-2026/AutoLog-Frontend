package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kotlin.math.abs

/** 화살표 갈매기 수. */
private const val CHEVRONS = 3

/** 불이 꺼진 갈매기의 투명도 — 완전히 사라지지 않아 방향이 늘 읽힌다. */
private const val CHEVRON_REST_ALPHA = 0.18f

/** 빛 덩어리의 폭(갈매기 칸 단위). 1보다 크게 — 이웃 갈매기가 함께 번지며 "슉" 하고 흐르는 느낌. */
private const val GLOW_SPREAD = 1.2f

/**
 * 디자인 언어 11장 RouteFlow — 출발지(`label`, 회색) → 흐르는 화살표 → 목적지(`label`, 두 줄까지). 같은 역할이라 같은 스타일. "지금 이 경로를 가는 중"을 한눈에.
 * 화살표는 하이브리드 회생 제동 표시처럼 갈매기(⌄) 3개에 빛이 위에서 아래로 주기적으로 흐른다([MotionTokens.flowPeriodMs]).
 * 반복 모션은 [kr.autolog.vehicle.ui.design.MotionTokens.ambient]일 때만 — 꺼지면 위→아래로 짙어지는 정지 그림(방향은 그대로 읽힌다).
 * [origin]이 null이면 출발지·화살표 없이 목적지만. 목적지를 아직 모르면 [destinationColor]로 흐리게 안내 문구를 넣는다.
 */
@Composable
fun RouteFlow(
    origin: String?,
    destination: String,
    modifier: Modifier = Modifier,
    destinationColor: Color = AutoLogTheme.colors.textPrimary,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s2)) {
        if (origin != null) {
            // 출발지·목적지는 같은 역할(장소 이름)이라 같은 스타일 — 출발지만 회색
            Text(origin, style = t.label, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            FlowChevrons(Modifier.padding(vertical = Space.s1))
        }
        // 긴 거래처 이름은 두 줄까지, 넘치면 말줄임
        Text(destination, style = t.label, color = destinationColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 아래를 향한 갈매기 3개. 칸 높이 `space.4`, 폭 `iconSm`, 선 `ring`(3 dp), 둥근 끝. */
@Composable
private fun FlowChevrons(modifier: Modifier = Modifier) {
    val motion = AutoLogTheme.motion
    val accent = AutoLogTheme.colors.accent
    val phase: State<Float> =
        if (motion.ambient) {
            rememberInfiniteTransition(label = "routeFlow").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(motion.flowPeriodMs, easing = LinearEasing),
                    RepeatMode.Restart,
                ),
                label = "routeFlowPhase",
            )
        } else {
            remember { mutableFloatStateOf(-1f) } // 정지: 아래 alphaOf가 고정 그라데이션을 쓴다
        }
    val p by phase
    Canvas(modifier.size(width = Size.iconSm, height = Space.s4 * CHEVRONS)) {
        val cell = size.height / CHEVRONS
        val stroke = Stroke(width = Size.ring.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val inset = stroke.width / 2
        for (i in 0 until CHEVRONS) {
            val top = i * cell + cell * 0.25f
            val bottom = top + cell * 0.5f
            val path =
                Path().apply {
                    moveTo(inset, top)
                    lineTo(size.width / 2, bottom)
                    lineTo(size.width - inset, top)
                }
            drawPath(path, accent.copy(alpha = alphaOf(i, p)), style = stroke)
        }
    }
}

/**
 * 갈매기 [index]의 투명도. 빛의 머리가 위(-0.5)에서 아래(CHEVRONS + 0.5)로 지나가며 가까운 갈매기를 밝힌다 — 마지막 뒤에 짧은 쉼.
 * [phase] < 0 이면 정지 그림: 위에서 아래로 짙어진다.
 */
internal fun alphaOf(index: Int, phase: Float): Float {
    if (phase < 0f) return 0.35f + 0.65f * index / (CHEVRONS - 1)
    val head = -0.5f + phase * (CHEVRONS + 1.5f)
    val glow = (1f - abs(head - index) / GLOW_SPREAD).coerceIn(0f, 1f)
    return CHEVRON_REST_ALPHA + (1f - CHEVRON_REST_ALPHA) * glow
}
