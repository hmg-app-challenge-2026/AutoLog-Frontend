package kr.autolog.vehicle.ui.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.core.PurposeDetail
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

private val summary = ReviewSummaryUi("09:00", "09:35", "23.4", "본사", "판교 테크노밸리 거래처")

/** 확신 추천(거래처 방문) — 큰 버튼 1개, 선택지는 접힘. */
private val confident =
    ReviewUiState(
        loading = false,
        summary = summary,
        suggestion = PurposeDetail.CLIENT_VISIT,
        showOptions = false,
        voiceAvailable = true,
    )

/** 확신이 낮음 — 추천 없이 선택지. */
private val unsure = confident.copy(suggestion = null, showOptions = true)

/** 말로 입력 — 알아들은 상태. */
private val heard =
    confident.copy(voice = VoiceUi.Heard("판교 A고객사 미팅", PurposeDetail.CLIENT_VISIT, "판교 A고객사 미팅"))

/** 도착지를 모르는 운행 — 최근 목적지 후보. */
private val noDestination =
    unsure.copy(
        summary = summary.copy(destination = null),
        destinationCandidates = listOf("판교 테크노밸리 거래처", "본사", "수원 지점"),
    )

/** 운행 기록 조회에서 이미 확정한 운행을 고치러 옴 — 지금 목적(외근)이 체크. */
private val editing = unsure.copy(currentPurpose = PurposeDetail.FIELD_WORK)

private val placeProposal = confident.copy(place = PlaceProposalUi("판교 테크노밸리 거래처"))

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp")
class ReviewScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: ReviewUiState, actions: MutableList<ReviewAction>) = compose.setContent {
        AutoLogTheme { WindowLayoutHost { ReviewScreen(state, onAction = { actions += it }) } }
    }

    @Test
    fun `확신 추천은 큰 버튼 1탭으로 확정 — 조사까지 맞춘 문구`() {
        val actions = mutableListOf<ReviewAction>()
        show(confident, actions)

        compose.onNodeWithText("거래처 방문으로 기록").performClick()

        assertEquals(listOf<ReviewAction>(ReviewAction.Confirm(PurposeDetail.CLIENT_VISIT)), actions)
    }

    @Test
    fun `확신이 낮으면 선택지 3묶음, 하나를 누르면 바로 확정`() {
        val actions = mutableListOf<ReviewAction>()
        show(unsure, actions)

        compose.onNodeWithText("거래처 방문으로 기록").assertDoesNotExist()
        compose.onNodeWithText("출퇴근용").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("퇴근").performScrollTo().performClick()

        assertEquals(listOf<ReviewAction>(ReviewAction.Confirm(PurposeDetail.COMMUTE_OUT)), actions)
    }

    @Test
    fun `다른 목적·말로 입력·나중에`() {
        val actions = mutableListOf<ReviewAction>()
        show(confident, actions)

        compose.onNodeWithText("다른 목적").performScrollTo().performClick()
        compose.onNodeWithContentDescription("말로 입력").performScrollTo().performClick()
        compose.onNodeWithText("나중에").performScrollTo().performClick()

        assertEquals(listOf(ReviewAction.ShowOptions, ReviewAction.Listen, ReviewAction.Later), actions)
    }

    @Test
    fun `알아들으면 원문과 저장 버튼`() {
        val actions = mutableListOf<ReviewAction>()
        show(heard, actions)

        compose.onNodeWithText("“판교 A고객사 미팅”").assertIsDisplayed()
        compose.onNodeWithText("저장").performScrollTo().performClick()

        assertEquals(listOf<ReviewAction>(ReviewAction.SaveVoice), actions)
    }

    @Test
    fun `거래처 등록 팝업 — 등록, X는 아니요`() {
        val actions = mutableListOf<ReviewAction>()
        show(placeProposal, actions)

        compose.onNodeWithText("등록").performClick() // 하단 [등록] 버튼(제목은 "거래처 등록")
        compose.onNodeWithContentDescription("아니요").performClick()

        assertEquals(listOf(ReviewAction.RegisterPlace, ReviewAction.SkipPlace), actions)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ReviewScreenshotTest {
    @Test
    fun confident() = captureAll("review_confident", driver = SampleDriver) { ReviewScreen(confident, onAction = {}) }

    @Test
    fun unsure() = captureAll("review_options", driver = SampleDriver) { ReviewScreen(unsure, onAction = {}) }

    @Test
    fun listening() = captureAll("review_listening", driver = SampleDriver) {
        ReviewScreen(confident.copy(voice = VoiceUi.Listening), onAction = {})
    }

    @Test
    fun heard() = captureAll("review_heard", driver = SampleDriver) { ReviewScreen(heard, onAction = {}) }

    @Test
    fun noDestination() = captureAll("review_no_destination", driver = SampleDriver) {
        ReviewScreen(noDestination, onAction = {})
    }

    @Test
    fun editing() = captureAll("review_editing", driver = SampleDriver) { ReviewScreen(editing, onAction = {}) }

    @Test
    fun place() = captureAll("review_place", driver = SampleDriver) { ReviewScreen(placeProposal, onAction = {}) }
}
