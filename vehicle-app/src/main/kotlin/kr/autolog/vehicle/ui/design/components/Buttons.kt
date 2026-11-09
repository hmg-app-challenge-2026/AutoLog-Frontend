package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space

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
    // 잠금이면 눌리지 않을 뿐 아니라 비활성 색으로 보인다(누를 수 없음을 미리 알림)
    val active = enabled && !LocalInputLocked.current && !loading
    Box(
        modifier =
        modifier
            .heightIn(min = height)
            .defaultMinSize(minWidth = Size.touchMin)
            .tappable(onClick, enabled = enabled && !loading)
            .clip(RoundedCornerShape(Radius.lg))
            .background(if (active || loading) container else disabledContainer)
            .padding(horizontal = Space.s6),
        contentAlignment = Alignment.Center,
    ) {
        // 로딩 중에도 글자를 투명하게 남겨 버튼 폭이 흔들리지 않게 한다
        Text(
            text = text,
            style = AutoLogTheme.type.label,
            color = if (loading) {
                Color.Transparent
            } else if (active) {
                content
            } else {
                colors.textTertiary
            },
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
    // surface3: 시트(surface2) 위에서도 버튼 면이 보이게 한 단계 위 서피스
    ButtonBody(text, onClick, modifier, height, c.surface3, c.textPrimary, c.surface3, enabled)
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

/**
 * 닫기(X). 보이는 원은 `iconLg`(48 dp, `surface3`)지만 터치 영역은 `touchMin`(76 dp) — 팔을 들어 누르는 차 화면 기준.
 * 고르기만 하는 팝업(전체 직원 등)의 오른쪽 위에 둔다. 결정이 필요한 팝업은 X 대신 하단 버튼으로 끝낸다(디자인 언어 11장).
 */
@Composable
fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.action_close),
) {
    val c = AutoLogTheme.colors
    Box(
        modifier
            .size(Size.touchMin)
            .tappable(onClick)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(Size.iconLg).clip(CircleShape).background(c.surface3), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = label,
                tint = c.textSecondary,
                modifier = Modifier.size(Size.iconSm),
            )
        }
    }
}

/**
 * 디자인 언어 11장 IconButtonXL — 96 dp 원형(`surface2`), 아이콘 36 dp(`accent`). 마이크처럼 글자 없이도 뜻이 분명한 동작에만.
 * [label]은 접근성 이름(화면에 안 보임) — 보이는 이름이 필요하면 옆에 글자를 따로 둔다.
 */
@Composable
fun IconButtonXL(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    Box(
        modifier
            .size(Size.iconButton)
            .tappable(onClick)
            .clip(CircleShape)
            .background(c.surface2),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = c.accent, modifier = Modifier.size(Size.icon))
    }
}
