package kr.autolog.vehicle.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.screenshot.SampleDriver
import kr.autolog.vehicle.screenshot.ShotDriver
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val month = MonthUi(tripCount = 23, totalKm = "412.6", businessKm = "388.1")

private val recent =
    listOf(
        RecentTripUi("t-1", "본사", "판교 테크노밸리 거래처", "23.4", TripDay.TODAY, 11, 9, "09:12", null, needsReview = true),
        RecentTripUi("t-2", "판교 테크노밸리 거래처", "본사", "22.8", TripDay.YESTERDAY, 11, 8, "17:40", "거래처 방문", false),
        RecentTripUi("t-3", "본사", "수원 물류센터", "41.2", TripDay.EARLIER, 11, 6, "13:05", "외근", false),
    )

private val ready =
    HomeUiState(
        phase = HomePhase.READY,
        month = month,
        recentTrips = recent,
        notices = listOf(HomeNotice.Review(1), HomeNotice.Receipt(1), HomeNotice.MissedTrip("15.0")),
    )

/** 정상 대기 — 알림 띠가 없다. */
private val readyQuiet = ready.copy(notices = emptyList())

private val recording =
    HomeUiState(
        phase = HomePhase.RECORDING_PARKED,
        tripKm = "23.4",
        origin = "본사",
        destination = "판교 테크노밸리 거래처",
        elapsedMin = 35,
    )

/** 기록 자체에 문제가 있을 때 — 계기판 미확정 + 영수증 도착, 긴 목적지 이름. */
private val recordingTrouble =
    recording.copy(
        tripKm = null,
        destination = "경기도 성남시 분당구 판교역로 테크노밸리 제2캠퍼스 거래처 본관",
        notices = listOf(HomeNotice.OdometerUnavailable, HomeNotice.Receipt(1)),
    )
private val autoEnding = recording.copy(phase = HomePhase.AUTO_ENDING, autoEnd = AutoEndUi(3, 0.6f, StopReason.GEAR_P))
private val recovery = HomeUiState(phase = HomePhase.NEEDS_RECOVERY, recoveryExtraKm = "3.2")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp")
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: HomeUiState, actions: MutableList<HomeAction> = mutableListOf()) = compose.setContent {
        AutoLogTheme { WindowLayoutHost { HomeScreen(state, onAction = { actions += it }) } }
    }

    @Test
    fun `자동 종료 안내에서 계속 기록은 1탭`() {
        val actions = mutableListOf<HomeAction>()
        show(autoEnding, actions)

        compose.onNodeWithText("계속 기록").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.KeepRecording), actions)
    }

    @Test
    fun `삭제는 한 번 더 확인한다`() {
        val actions = mutableListOf<HomeAction>()
        show(recovery, actions)

        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("지우기").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.Recover(RecoveryChoice.DELETE)), actions)
    }

    @Test
    fun `알림 띠 — 알약 1탭으로 처리 화면, 넘치면 외 N건 목록에서 놓친 운행을 추가`() {
        val actions = mutableListOf<HomeAction>()
        show(ready, actions)

        compose.onNodeWithText("운행 1건 목적 확인").performClick()
        compose.onNodeWithText("영수증 1장 도착").performClick()
        // 넓은 창 한 줄에 셋이 다 안 들어가 마지막은 "외 1건"으로 접힌다
        compose.onNodeWithText("외 1건").performClick()
        compose.onNodeWithText("추가").performScrollTo().performClick()

        assertEquals(listOf(HomeAction.OpenReview, HomeAction.OpenReceipt, HomeAction.AcceptMissedTrip), actions)
    }

    @Test
    fun `정상이면 알림 띠가 없고 최근 운행 기록이 보인다`() {
        show(readyQuiet)

        compose.onNodeWithText("운행 1건 목적 확인").assertDoesNotExist()
        compose.onNodeWithText("최근 운행 기록").assertIsDisplayed()
        compose.onNodeWithText("본사 → 판교 테크노밸리 거래처").assertIsDisplayed()
    }

    @Test
    fun `최근 운행 — 목적이 없는 운행을 누르면 그 운행의 목적 확인`() {
        val actions = mutableListOf<HomeAction>()
        show(readyQuiet, actions)

        compose.onNodeWithText("오늘 09:12 · 23.4 km · 목적 확인 필요").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.OpenTrip("t-1", needsReview = true)), actions)
    }

    @Test
    fun `타일 4개 — 운행 기록 조회·설정·기록 시작`() {
        val actions = mutableListOf<HomeAction>()
        show(readyQuiet, actions)

        compose.onNodeWithText("운행 기록 조회").performClick()
        compose.onNodeWithText("설정").performClick()
        compose.onNodeWithText("기록 시작").performClick()

        assertEquals(listOf(HomeAction.OpenRecords, HomeAction.OpenSettings, HomeAction.StartTrip), actions)
    }

    @Test
    fun `기록 중 — 상태 줄은 점 + 기록 중 · 경과, 4번째 타일은 기록 종료`() {
        val actions = mutableListOf<HomeAction>()
        show(recording, actions)

        compose.onNodeWithText("기록 중").assertIsDisplayed()
        compose.onNodeWithText("35분 경과").assertIsDisplayed()
        compose.onNodeWithText("운행 경로").assertIsDisplayed()
        compose.onNodeWithText("기록 시작").assertDoesNotExist()
        compose.onNodeWithText("기록 종료").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.EndNow), actions)
    }

    @Test
    fun `삭제 확인에서 X를 누르면 지우지 않고 앞 단계로 돌아간다`() {
        val actions = mutableListOf<HomeAction>()
        show(recovery, actions)

        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithContentDescription("닫기").performClick()

        compose.onNodeWithText("기록하기").assertIsDisplayed()
        assertEquals(emptyList<HomeAction>(), actions)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomeScreenshotTest {
    @Test
    fun ready() = captureAll("home_ready", driver = SampleDriver) { HomeScreen(ready, onAction = {}) }

    @Test
    fun quiet() = captureAll("home_ready_quiet", driver = SampleDriver) { HomeScreen(readyQuiet, onAction = {}) }

    @Test
    fun noDriver() = captureAll("home_nodriver", driver = ShotDriver(null, null)) { HomeScreen(ready, onAction = {}) }

    @Test
    fun recording() = captureAll("home_recording", driver = SampleDriver) { HomeScreen(recording, onAction = {}) }

    @Test
    fun recordingTrouble() = captureAll("home_recording_trouble", driver = SampleDriver) {
        HomeScreen(recordingTrouble, onAction = {})
    }

    @Test
    fun autoEnding() = captureAll("home_autoend", driver = SampleDriver) { HomeScreen(autoEnding, onAction = {}) }

    @Test
    fun recovery() = captureAll("home_recovery", driver = SampleDriver) { HomeScreen(recovery, onAction = {}) }
}
