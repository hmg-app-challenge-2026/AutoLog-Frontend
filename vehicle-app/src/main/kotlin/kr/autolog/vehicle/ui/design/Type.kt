package kr.autolog.vehicle.ui.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kr.autolog.vehicle.R

@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: Int) = Font(
    R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Asta Sans (42dot, OFL, 가변 300–800). 디자인 언어 6.1. */
val AstaSans = FontFamily(astaSans(300), astaSans(400), astaSans(500), astaSans(600), astaSans(700))

/** 디자인 언어 6.2. */
@Immutable
data class AutoLogType(
    val hero: TextStyle,
    val heroSmall: TextStyle,
    val heroCompact: TextStyle,
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** 영역 소제목("이번 달 운행 정보", "운행 경로"). 크기는 caption과 같고 굵기로 제목임을 드러낸다 — 소제목보다 큰 설명 글씨를 두지 않는다. */
    val section: TextStyle,
    /**
     * **글자 하한**(20 sp) — 공간이 정말 모자란 곳에만: 좁은 창에서 버튼 여러 개를 한 줄에 둘 때(dense 버튼).
     * Pleos 보조 텍스트 높이 3 mm ≈ 19 sp(기준 밀도 160 dpi)에 맞춘 값. 이보다 작은 글자를 만들지 않는다.
     */
    val fine: TextStyle,
    /** [section]의 좁은 창판(20 sp 굵게, 글자 하한) — 분할 화면처럼 세로 공간이 모자란 화면의 소제목. */
    val sectionFine: TextStyle,
)

/** 한국어를 단어(어절) 중간에서 자르지 않는다 — "달 / 렸어요" 방지 (Android 13+ Phrase 줄바꿈, 미만은 기본 동작). */
private val KoreanPhraseLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

private fun style(sizeSp: Int, weight: Int, lineHeight: Float, trackingEm: Float, tnum: Boolean = false) = TextStyle(
    lineBreak = KoreanPhraseLineBreak,
    fontFamily = AstaSans,
    fontWeight = FontWeight(weight),
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * lineHeight).sp,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = if (tnum) "tnum" else null,
)

fun autoLogType(): AutoLogType = AutoLogType(
    hero = style(140, 300, 1.0f, -0.02f, tnum = true),
    heroSmall = style(96, 300, 1.0f, -0.015f, tnum = true),
    heroCompact = style(72, 300, 1.0f, -0.015f, tnum = true),
    display = style(56, 600, 1.15f, -0.01f),
    headline = style(44, 600, 1.2f, -0.005f),
    title = style(36, 600, 1.25f, 0f),
    body = style(28, 400, 1.4f, 0f),
    label = style(28, 600, 1.2f, 0f),
    caption = style(22, 500, 1.3f, 0.005f),
    section = style(22, 700, 1.3f, 0.01f),
    fine = style(20, 500, 1.3f, 0.005f),
    sectionFine = style(20, 700, 1.3f, 0.01f),
)

/**
 * Material 3 글자 슬롯 15개 전부를 Asta Sans로. 우리 화면은 [AutoLogType]만 쓰지만, M3 기본 컴포넌트가
 * 자기 슬롯(bodyMedium·labelSmall 등)을 쓰는 순간 시스템 글꼴이 섞이지 않게 막는다(디자인 언어 6.1).
 * 자주 쓰는 세 슬롯은 우리 토큰과 같은 값을 쓴다.
 */
fun autoLogMaterialTypography(type: AutoLogType): Typography {
    val d = Typography()
    fun TextStyle.asta() = copy(fontFamily = AstaSans, lineBreak = KoreanPhraseLineBreak)
    return Typography(
        displayLarge = d.displayLarge.asta(),
        displayMedium = d.displayMedium.asta(),
        displaySmall = d.displaySmall.asta(),
        headlineLarge = d.headlineLarge.asta(),
        headlineMedium = d.headlineMedium.asta(),
        headlineSmall = d.headlineSmall.asta(),
        titleLarge = type.title,
        titleMedium = d.titleMedium.asta(),
        titleSmall = d.titleSmall.asta(),
        bodyLarge = type.body,
        bodyMedium = d.bodyMedium.asta(),
        bodySmall = d.bodySmall.asta(),
        labelLarge = type.label,
        labelMedium = d.labelMedium.asta(),
        labelSmall = d.labelSmall.asta(),
    )
}
