package kr.autolog.vehicle.ui.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.verticalScrollFade

/** 지금 떠 있는 모달 수. 운전자 배지 등 화면 위 요소는 0이 아니면 숨는다(모달이 화면 전체를 덮어야 하므로). */
val LocalModalCount = compositionLocalOf<MutableIntState?> { null }

/** 등장 시 카드가 커지기 시작하는 배율(애플 알림처럼 살짝 앞으로 다가온다). */
private const val MODAL_ENTER_SCALE = 0.94f

/**
 * 디자인 언어 11장 ModalCard — 앱의 **유일한 팝업 모양**. 화면 가운데 둥근 카드(`radius.xl`) + scrim.
 * 아래에 붙는 시트는 쓰지 않는다(2026-11-09 사용자 결정: 옛 안드로이드 앱처럼 보였다).
 * - 면: [kr.autolog.vehicle.ui.design.AutoLogColors.surfaceRaised] + 머리카락 테두리. 그림자는 쓰지 않는다(평면 — 테슬라식,
 *   scrim과 테두리만으로 떠 있는 면이 읽힌다. 그림자는 렌더러마다 다르게 그려졌다).
 * - 등장: scrim은 투명도, 카드는 투명도 + 0.94→1 배율(`motion.standard`, 정적 모드면 즉시).
 * - 키보드가 올라오면 카드가 그 위로 올라가고(imePadding), 내용이 넘치면 카드 안에서 스크롤한다(가장자리 흐림).
 * - [footer]는 스크롤 밖 카드 하단에 고정된다 — 결정 버튼은 여기에 둔다(주 버튼은 스크롤해야 보이면 안 된다, 디자인 언어 7장).
 * - [onClose]가 있으면 카드 오른쪽 위에 X([CloseButton])가 고정된다 — 고르기만 하는 팝업(목록)용. 결정 팝업은 footer로 끝낸다.
 * - 바깥 탭은 뒤 화면으로 새지 않게 삼킨다(클릭 의미론 없음 — Peek 입력 0개 검사에 걸리지 않는다). 닫기는 카드 안 버튼으로만.
 * 단계가 FULL에서 내려가면 루트가 화면째 지운다.
 */
@Composable
fun ModalCard(
    modifier: Modifier = Modifier,
    maxWidth: Dp = Size.dialogMaxWidth,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = AutoLogTheme.colors
    val motion = AutoLogTheme.motion
    val modal = LocalModalCount.current
    DisposableEffect(modal) {
        modal?.let { it.intValue += 1 }
        onDispose { modal?.let { it.intValue -= 1 } }
    }
    val appear = remember { Animatable(if (motion.isStatic) 1f else 0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, motion.standard()) }
    val shape = RoundedCornerShape(Radius.xl)
    Box(modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }.background(c.scrim))
        Box(Modifier.fillMaxSize().imePadding().padding(Space.s6), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val p = appear.value
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = MODAL_ENTER_SCALE + (1f - MODAL_ENTER_SCALE) * p
                        scaleY = scaleX
                    }.clip(shape)
                    .background(c.surfaceRaised)
                    .border(Size.hairline, c.outline, shape),
            ) {
                // X는 스크롤 밖 머리 줄에 고정 — 내용 위에 겹치지 않고, 스크롤해도 항상 닿는다
                if (onClose != null) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = Space.s3, end = Space.s3),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        CloseButton(onClose)
                    }
                }
                val scroll = rememberScrollState()
                Column(
                    Modifier
                        .weight(1f, fill = false)
                        .verticalScrollFade(scroll, Space.s7)
                        .verticalScroll(scroll)
                        .padding(
                            start = Space.s6,
                            end = Space.s6,
                            bottom = Space.s6,
                            top = if (onClose !=
                                null
                            ) {
                                Space.s1
                            } else {
                                Space.s6
                            },
                        ),
                    content = content,
                )
                if (footer != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = Space.s6, end = Space.s6, bottom = Space.s6),
                        content = footer,
                    )
                }
            }
        }
    }
}

/** 팝업 제목(`headline`). **모든 팝업의 제목·설명은 가운데 정렬**(D-21) — 정렬을 이 함수 한 곳에 묶는다. */
@Composable
fun ModalTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.fillMaxWidth(),
        style = AutoLogTheme.type.headline,
        color = AutoLogTheme.colors.textPrimary,
        textAlign = TextAlign.Center,
    )
}

/** 팝업 설명·요약 문구. 기본은 `body` + `textSecondary`, 강조가 필요하면 [style]·[color]만 바꾼다. 가운데 정렬. */
@Composable
fun ModalText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AutoLogTheme.type.body,
    color: Color = AutoLogTheme.colors.textSecondary,
) {
    Text(text, modifier.fillMaxWidth(), style = style, color = color, textAlign = TextAlign.Center)
}
