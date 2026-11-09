package kr.autolog.vehicle.ui.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.screenshot.SampleDriver
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 방금 끝난 운행 — 보기 모드, 5초 중 3초 남음. */
private val viewing =
    TripResultUiState(
        loading = false,
        km = "48.2",
        minutes = 52,
        origin = "판교 테크노밸리",
        destination = "서울 강남구 역삼동",
        autoCloseSec = 3,
    )

/** [수정하기] 뒤 — 카운트다운 멈춤, 출퇴근을 고름. */
private val editing = viewing.copy(editing = true, autoCloseSec = null, selected = PurposeCategory.COMMUTE)

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp")
class TripResultScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: TripResultUiState, actions: MutableList<TripResultAction>) = compose.setContent {
        AutoLogTheme { WindowLayoutHost { TripResultScreen(state, onAction = { actions += it }) } }
    }

    @Test
    fun `보기 모드 — 결과와 분류 한 줄, 라디오는 눌러도 아무 일 없고 수정하기·메인 화면으로`() {
        val actions = mutableListOf<TripResultAction>()
        show(viewing, actions)

        compose.onNodeWithText("운행 기록을 완료했어요").assertIsDisplayed()
        compose.onNodeWithText("일반 업무용 거리 48.2 km로 분류돼요").assertIsDisplayed()
        compose.onNodeWithText("3초 후 메인 화면으로 돌아가요").assertIsDisplayed()
        compose.onNodeWithText("출퇴근").performClick()
        compose.onNodeWithText("수정하기").performClick()
        compose.onNodeWithText("메인 화면으로").performClick()

        assertEquals(listOf(TripResultAction.Edit, TripResultAction.GoHome), actions)
    }

    @Test
    fun `수정 모드 — 라디오로 고르고 저장`() {
        val actions = mutableListOf<TripResultAction>()
        show(editing, actions)

        compose.onNodeWithText("개인 용무").performClick()
        compose.onNodeWithText("저장").performClick()

        assertEquals(listOf(TripResultAction.Select(PurposeCategory.NON_BUSINESS), TripResultAction.Save), actions)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class TripResultScreenshotTest {
    @Test
    fun viewing() = captureAll("trip_result", driver = SampleDriver) { TripResultScreen(viewing, onAction = {}) }

    @Test
    fun editing() = captureAll("trip_result_editing", driver = SampleDriver) {
        TripResultScreen(editing, onAction = {})
    }
}
