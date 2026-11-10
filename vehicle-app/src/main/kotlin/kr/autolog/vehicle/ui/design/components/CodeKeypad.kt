package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 CodeKeypad — 글자와 숫자가 섞인 코드(차량 연결 코드)를 키보드 없이 넣는 키패드.
 * [keys]를 [columns]개씩 줄로 놓고(차량 연결 코드 32자 → 8×4: 2~9 / A~H / J~R / S~Z), 마지막 줄 오른쪽에 지우기(키 두 칸 폭).
 * 키는 [NumericKeypad]와 같은 모양(96 dp 높이, `surface3`, 글자 `headline`) — 폭은 줄을 [columns]로 나눈다.
 * 차 화면에서 누르기 쉽게 팝업을 넓게(`sheetMaxWidth`) 쓴다: 960 dp 카드면 키 하나 ≈ 100 dp.
 */
@Composable
fun CodeKeypad(
    keys: String,
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 8,
) {
    val gap = Space.s3
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        keys.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { key -> KeypadKey({ onKey(key) }, Modifier.weight(1f)) { KeyText(key.toString()) } }
                // 마지막 줄이 덜 차면 빈칸으로 같은 폭을 지킨다
                repeat(columns - row.length) { Spacer(Modifier.weight(1f)) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.weight((columns - DELETE_SPAN).toFloat()))
            KeypadKey(onDelete, Modifier.weight(DELETE_SPAN.toFloat())) { DeleteGlyph() }
        }
    }
}

/** 지우기 키가 차지하는 칸 수 — 가장 자주 누르는 보조 키라 넓게. */
private const val DELETE_SPAN = 2
