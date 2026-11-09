package kr.autolog.vehicle.ui.design.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
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
