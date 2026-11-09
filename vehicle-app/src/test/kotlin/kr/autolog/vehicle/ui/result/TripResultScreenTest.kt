package kr.autolog.vehicle.ui.result

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 기어 P로 종료 대기 — 보기 모드, 3초 남음, [계속 기록하기] 가능. */
private val viewing =
    TripResultUiState(
        loading = false,
        km = "48.2",
        minutes = 52,
        origin = "판교 테크노밸리",
        destination = "서울 강남구 역삼동",
        autoCloseSec = 3,
        canContinue = true,
    )

/** 이미 끝난 운행(직접 [기록 종료]·이력에서 열기) — [계속 기록하기] 없음. */
private val ended = viewing.copy(canContinue = false)

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
        compose.onNodeWithText("메인 화면으로 · 3초").assertIsDisplayed() // 넓은 창: 남은 시간은 주 버튼 글자에
        compose.onNodeWithText("출퇴근").performClick()
        compose.onNodeWithText("계속 기록하기").performClick()
        compose.onNodeWithText("수정하기").performClick()
        compose.onNodeWithText("메인 화면으로 · 3초").performClick()

        assertEquals(listOf(TripResultAction.Continue, TripResultAction.Edit, TripResultAction.GoHome), actions)
    }

    @Test
    fun `이미 끝난 운행엔 계속 기록하기가 없다`() {
        show(ended, mutableListOf())

        compose.onNodeWithText("계속 기록하기").assertDoesNotExist()
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
    // 실제 앱처럼 운전자 배지 없이(FullApp.showsDriverBadge)
    @Test
    fun viewing() = captureAll("trip_result") { TripResultScreen(viewing, onAction = {}) }

    @Test
    fun editing() = captureAll("trip_result_editing") {
        TripResultScreen(editing, onAction = {})
    }

    @Test
    fun ended() = captureAll("trip_result_ended") { TripResultScreen(ended, onAction = {}) }
}
