package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

/** 디자인 언어 11장 NumericKeypad: 3×4, 키 96 dp, 간격 16 dp. 키보드 대신 숫자를 넣는다(연결 코드·금액·리터). */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    allowDecimal: Boolean,
    modifier: Modifier = Modifier,
    keyWidth: Dp = Size.iconButton * 1.25f,
) {
    val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.s4)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                row.forEach { d -> Key(keyWidth, { onDigit(d) }) { KeyText(d.toString()) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (allowDecimal) {
                Key(keyWidth, {
                    onDigit('.')
                }) { KeyText(".") }
            } else {
                Spacer(Modifier.size(keyWidth, Size.iconButton))
            }
            Key(keyWidth, { onDigit('0') }) { KeyText("0") }
            val delete = stringResource(R.string.keypad_delete)
            Key(keyWidth, onDelete) {
                Icon(
                    Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = delete,
                    tint = AutoLogTheme.colors.textPrimary,
                    modifier = Modifier.size(Size.icon),
                )
            }
        }
    }
}

@Composable
private fun KeyText(text: String) =
    Text(text, style = AutoLogTheme.type.headline, color = AutoLogTheme.colors.textPrimary)

@Composable
private fun Key(width: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(width, Size.iconButton)
            .tappable(onClick)
            .clip(RoundedCornerShape(Radius.lg))
            .background(AutoLogTheme.colors.surface2),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 키 하나를 반영한 새 문자열. 소수점은 한 번만, 앞자리 0은 하나만, 최대 [maxLength]자. */
fun applyKey(current: String, key: Char, maxLength: Int = 9): String = when {
    current.length >= maxLength -> current
    key == '.' && (current.contains('.') || current.isEmpty()) -> current
    key == '0' && current == "0" -> current
    current == "0" && key != '.' -> key.toString()
    else -> current + key
}
