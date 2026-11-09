package kr.autolog.vehicle.ui.design

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalAutoLogColors = staticCompositionLocalOf { DarkColors }
val LocalAutoLogType = staticCompositionLocalOf { autoLogType() }
val LocalMotion = staticCompositionLocalOf { MotionTokens.Standard }

/** 스펙 6.9 4번: tier != FULL 또는 FULL 진입 직후 300 ms. 모든 상호작용 컴포넌트가 읽는다. */
val LocalInputLocked = compositionLocalOf { false }

object AutoLogTheme {
    val colors: AutoLogColors
        @Composable @ReadOnlyComposable
        get() = LocalAutoLogColors.current
    val type: AutoLogType
        @Composable @ReadOnlyComposable
        get() = LocalAutoLogType.current
    val motion: MotionTokens
        @Composable @ReadOnlyComposable
        get() = LocalMotion.current
}

@Composable
private fun systemAnimationsOff(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** 디자인 언어 5.5 M3 매핑. 화면 코드는 MaterialTheme이 아니라 AutoLogTheme.colors를 쓴다. */
private fun AutoLogColors.toMaterial() =
    if (isDark) {
        darkColorScheme(
            primary = accentFill,
            onPrimary = onAccent,
            background = bg,
            surface = bg,
            surfaceContainerLow = surface1,
            surfaceContainer = surface2,
            surfaceContainerHigh = surface3,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            outline = outlineStrong,
            outlineVariant = outline,
            error = danger,
            onError = onAccent,
            scrim = scrim,
        )
    } else {
        lightColorScheme(
            primary = accentFill,
            onPrimary = onAccent,
            background = bg,
            surface = bg,
            surfaceContainerLow = surface1,
            surfaceContainer = surface2,
            surfaceContainerHigh = surface3,
            onBackground = textPrimary,
            onSurface = textPrimary,
            onSurfaceVariant = textSecondary,
            outline = outlineStrong,
            outlineVariant = outline,
            error = danger,
            onError = onAccent,
            scrim = scrim,
        )
    }

@Composable
fun AutoLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    staticMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val type = remember { autoLogType() }
    val motion = if (staticMotion || systemAnimationsOff()) MotionTokens.Static else MotionTokens.Standard
    CompositionLocalProvider(
        LocalAutoLogColors provides colors,
        LocalAutoLogType provides type,
        LocalMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = Typography(bodyLarge = type.body, labelLarge = type.label, titleLarge = type.title),
            content = content,
        )
    }
}
