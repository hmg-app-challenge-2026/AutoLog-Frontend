package kr.autolog.vehicle.ui.design

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R

/** "5초" / "2분 41초" */
@Composable
fun durationText(sec: Int): String = when {
    sec < 60 -> stringResource(R.string.duration_sec, sec)
    else -> stringResource(R.string.duration_min_sec, sec / 60, sec % 60)
}

/**
 * 조사 "(으)로"를 붙인다: 받침이 없거나 ㄹ 받침이면 "로", 그 밖의 받침이면 "으로". 한글이 아니면 "(으)로".
 * 예: 회의로 · 외근으로 · 거래처 방문으로 · 지점 이동으로. 문장 틀은 strings.xml, 조사만 여기서 고른다.
 */
fun withRo(word: String): String {
    val last = word.lastOrNull() ?: return word
    if (last !in '가'..'힣') return "$word(으)로"
    val jong = (last - '가') % 28
    return if (jong == 0 || jong == RIEUL) "${word}로" else "${word}으로"
}

/** 종성 ㄹ의 인덱스(유니코드 한글 음절 = 초성·중성·종성 28). */
private const val RIEUL = 8
