package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.screenshot.ShotSize
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp")
class FormComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `키패드로 숫자를 넣고 지운다`() {
        var value = ""
        compose.setContent {
            AutoLogTheme {
                var v by remember { mutableStateOf("") }
                NumericKeypad(onDigit = {
                    v += it
                    value = v
                }, onDelete = {
                    v = v.dropLast(1)
                    value = v
                }, allowDecimal = true)
            }
        }

        compose.onNodeWithText("6").performClick()
        compose.onNodeWithText("8").performClick()
        compose.onNodeWithText(".").performClick()
        compose.onNodeWithContentDescription("지우기").performClick()

        assertEquals("68", value)
    }

    @Test
    fun `잠금이면 입력 필드가 비활성이고 이유를 보여 준다`() {
        compose.setContent {
            AutoLogTheme {
                CompositionLocalProvider(LocalInputLocked provides true) {
                    InputField(label = "서버 주소", value = "https://", onValueChange = {})
                }
            }
        }

        compose.onNodeWithText("https://").assertIsNotEnabled() // 비활성 필드엔 SetText 동작이 없다
        compose.onNodeWithText("지금은 입력할 수 없어요").assertExists()
    }

    @Test
    fun `QR 매트릭스는 여백 4모듈을 포함한다`() {
        val m = qrMatrix("https://autolog.example/r/abcd1234?t=demo")

        assertTrue(m.width >= 21 + 8)
        assertTrue((0 until 4).all { !m.get(it, it) }) // 왼쪽 위 여백은 비어 있다
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FormComponentsScreenshotTest {
    @Test
    fun forms() = captureAll("components_forms", listOf(ShotSize.TALL)) {
        Column(Modifier.padding(Space.s6), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            ActionCard(Icons.Rounded.Settings, "설정", onClick = {})
            OptionButton("거래처 방문", selected = true, onClick = {})
            OptionButton("외근", selected = false, onClick = {})
            InputField(label = "서버 주소", value = "https://autolog.acme.co.kr", onValueChange = {})
            ConfidenceRow("상호", "SK 판교주유소", 0.98, onClick = {})
            ConfidenceRow("수량", "4.0 L", 0.41, onClick = {})
            ConfidenceRow("금액", null, null, onClick = {})
            QrImage("https://autolog.example/r/abcd1234?t=demo", Space.s9 * 2)
            NumericKeypad(onDigit = {}, onDelete = {}, allowDecimal = false)
        }
    }
}
