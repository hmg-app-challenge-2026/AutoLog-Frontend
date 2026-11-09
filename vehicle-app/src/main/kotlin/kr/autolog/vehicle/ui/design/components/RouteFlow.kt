package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * 디자인 언어 11장 RouteFlow — **경로선**: 왼쪽 표시 칸(출발 ○ · 흐르는 화살표 · 도착 ●)과 오른쪽 글자 칸(장소 이름) 두 칸.
 * 출발지와 목적지가 같은 세로선에서 시작해 두 지점이 짝지어 읽힌다(지도 앱 경로 요약 방식).
 * 위계: 섹션 소제목(22 굵게) → **목적지**(`label` 28 세미볼드, 값, 두 줄까지) → 출발지(`caption` 22 보통, 회색, 보조).
 * 출발지를 소제목보다 약하게 둔다 — 같은 회색에 더 크면 또 하나의 소제목처럼 읽혔다(2026-11-09 좁은 창 점검).
 * 화살표는 하이브리드 회생 제동 표시처럼 갈매기(⌄) 3개에 빛이 위에서 아래로 주기적으로 흐른다([MotionTokens.flowPeriodMs]).
 * 반복 모션은 [kr.autolog.vehicle.ui.design.MotionTokens.ambient]일 때만 — 꺼지면 위→아래로 짙어지는 정지 그림(방향은 그대로 읽힌다).
 * [origin]이 null이면 출발지·화살표 없이 도착 점과 목적지만. 목적지를 아직 모르면 [destinationColor]로 흐리게 안내 문구를 넣는다.
 * [live] = 지금 달리는 운행(흐르는 화살표). 끝난 운행(운행 결과·이력)은 false — 화살표 대신 ○와 ●를 잇는 가는 선(지도 앱 경로 요약처럼),
 * 흐름이 "진행 중"으로 잘못 읽히지 않게.
 * [dense](좁은 창 반쪽 칸): 출발지·목적지를 **같은 크기**(`caption` 22 sp, 같은 글자색)로 맞추고 **굵기로만** 구분 — 목적지 굵게, 출발지 보통.
 */
@Composable
fun RouteFlow(
    origin: String?,
    destination: String,
    modifier: Modifier = Modifier,
    destinationColor: Color = AutoLogTheme.colors.textPrimary,
    live: Boolean = true,
    dense: Boolean = false,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val density = LocalDensity.current
    val originStyle = t.caption
    val destinationStyle = if (dense) t.caption.copy(fontWeight = FontWeight.Bold) else t.label
    val originColor = if (dense) destinationColor else c.textSecondary
    // 점을 그 줄 글자의 첫 줄 가운데에 맞추려고, 점 칸 높이를 글자 한 줄 높이로 둔다
    val originLine = with(density) { originStyle.lineHeight.toDp() }
    val destinationLine = with(density) { destinationStyle.lineHeight.toDp() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s1)) {
        if (origin != null) {
            RouteRow(markerHeight = originLine, marker = { OriginMarker() }) {
                Text(origin, style = originStyle, color = originColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RouteRow(markerHeight = null, marker = { if (live) FlowChevrons() else StaticConnector() }) {}
        }
        RouteRow(markerHeight = destinationLine, marker = { DestinationMarker() }) {
            // 긴 거래처 이름은 두 줄까지, 넘치면 말줄임
            Text(
                destination,
                style = destinationStyle,
                color = destinationColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 경로선 한 줄: 왼쪽 표시 칸(폭 `iconSm`, [markerHeight]가 있으면 그 높이 가운데) + 오른쪽 글자 칸. */
@Composable
private fun RouteRow(markerHeight: Dp?, marker: @Composable () -> Unit, text: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.s3), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.width(Size.iconSm).then(if (markerHeight != null) Modifier.height(markerHeight) else Modifier),
            contentAlignment = Alignment.Center,
        ) { marker() }
        text()
    }
}

/** 끝난 운행의 출발 → 도착 연결선: 굵기 `ring`(3 dp), `textTertiary`, 둥근 끝. 화살표 칸과 같은 높이라 경로선 전체 높이가 같다. */
@Composable
private fun StaticConnector() {
    Box(
        Modifier
            .size(width = Size.ring, height = Space.s3 * CHEVRONS)
            .clip(CircleShape)
            .background(AutoLogTheme.colors.textTertiary),
    )
}

/** 출발 ○ — 회색 테두리 원. */
@Composable
private fun OriginMarker() {
    Box(Modifier.size(Size.routeDot).border(Size.ring, AutoLogTheme.colors.textSecondary, CircleShape))
}

/** 도착 ● — 화살표와 같은 accent 채움. */
@Composable
private fun DestinationMarker() {
    Box(Modifier.size(Size.routeDot).clip(CircleShape).background(AutoLogTheme.colors.accent))
}

/** 아래를 향한 갈매기 3개. 칸 높이 `space.3`(12 dp), 폭 `space.5`, 선 `ring`(3 dp), 둥근 끝. */
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
    Canvas(modifier.size(width = Space.s5, height = Space.s3 * CHEVRONS)) {
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
