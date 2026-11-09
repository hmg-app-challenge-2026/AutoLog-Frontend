package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.screenshot.ShotSize
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Space
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `잠금이면 버튼이 비활성이고 눌러도 실행되지 않는다`() {
        var clicks = 0
        compose.setContent {
            AutoLogTheme {
                CompositionLocalProvider(LocalInputLocked provides true) {
                    PrimaryButton("운행 시작", onClick = { clicks++ })
                }
            }
        }

        compose.onNodeWithText("운행 시작").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test
    fun `잠금이 아니면 1탭으로 실행된다`() {
        var clicks = 0
        compose.setContent { AutoLogTheme { PrimaryButton("운행 시작", onClick = { clicks++ }) } }

        compose.onNodeWithText("운행 시작").assertIsEnabled().performClick()

        assertEquals(1, clicks)
    }
}

/** 스크린샷은 compose rule과 섞지 않고 별도 클래스로(각 캡처가 자기 Activity를 띄운다). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ComponentsScreenshotTest {
    @Test
    fun components() =
        captureAll("components", listOf(ShotSize.TALL)) {
            Column(Modifier.padding(Space.s6), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
                PrimaryButton("계속 기록", onClick = {})
                SecondaryButton("기록 종료", onClick = {})
                TertiaryButton("나중에 선택", onClick = {})
                StatusChip("확인 필요 1건", ChipTone.WARNING, onClick = {})
                HeroMetric("23.4", "km", "이번 운행", "계기판 거리를 읽지 못했어요", large = false)
                Banner("앱이 꺼진 동안 15.0 km를 달렸어요", listOf(BannerAction("추가") {}, BannerAction("무시") {}))
            }
        }
}
