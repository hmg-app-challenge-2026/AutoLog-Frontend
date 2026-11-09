package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.confirmHaptic

/** 모든 버튼의 공통 몸체. 잠금(LocalInputLocked)이면 스스로 비활성(디자인 언어 11장). */
@Composable
private fun ButtonBody(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    height: Dp,
    container: Color,
    content: Color,
    disabledContainer: Color,
    enabled: Boolean,
    loading: Boolean = false,
) {
    val colors = AutoLogTheme.colors
    val active = enabled && !LocalInputLocked.current && !loading
    val interaction = remember { MutableInteractionSource() }
    val view = LocalView.current
    Box(
        modifier =
            modifier
                .heightIn(min = height)
                .defaultMinSize(minWidth = Size.touchMin)
                .pressFeedback(interaction)
                .clip(RoundedCornerShape(Radius.lg))
                .background(if (active || loading) container else disabledContainer)
                .clickable(interactionSource = interaction, indication = null, enabled = active, role = Role.Button) {
                    view.confirmHaptic()
                    onClick()
                }.padding(horizontal = Space.s6),
        contentAlignment = Alignment.Center,
    ) {
        // 로딩 중에도 글자를 투명하게 남겨 버튼 폭이 흔들리지 않게 한다
        Text(
            text = text,
            style = AutoLogTheme.type.label,
            color = if (loading) Color.Transparent else if (active) content else colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        if (loading) {
            CircularProgressIndicator(color = content, strokeWidth = Space.s1, modifier = Modifier.size(Size.icon))
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = Size.buttonLg,
    loading: Boolean = false,
    enabled: Boolean = true,
) {
    val c = AutoLogTheme.colors
    ButtonBody(text, onClick, modifier, height, c.accentFill, c.onAccent, c.surface3, enabled, loading)
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = Size.buttonMd,
    enabled: Boolean = true,
) {
    val c = AutoLogTheme.colors
    ButtonBody(text, onClick, modifier, height, c.surface2, c.textPrimary, c.surface2, enabled)
}

@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = AutoLogTheme.colors
    ButtonBody(text, onClick, modifier, Size.touchMin, Color.Transparent, c.accent, Color.Transparent, enabled)
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = AutoLogTheme.colors
    ButtonBody(text, onClick, modifier, Size.buttonMd, c.dangerFill, c.onAccent, c.surface3, enabled)
}
