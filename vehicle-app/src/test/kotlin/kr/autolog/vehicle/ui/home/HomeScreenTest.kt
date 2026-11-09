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

private val ready =
    HomeUiState(
        phase = HomePhase.READY,
        month = month,
        notices = listOf(HomeNotice.Review(1), HomeNotice.Receipt(1), HomeNotice.MissedTrip("15.0")),
    )

/** 처리할 일이 없는 대기 — 알림 칸이 사라지고 주인공이 넓어진다. */
private val readyQuiet = HomeUiState(phase = HomePhase.READY, month = month)

private val recording =
    HomeUiState(
        phase = HomePhase.RECORDING_PARKED,
        tripKm = "23.4",
        origin = "본사",
        destination = "판교 테크노밸리 거래처",
        elapsedMin = 35,
    )
private val autoEnding = recording.copy(phase = HomePhase.AUTO_ENDING, autoEnd = AutoEndUi(3, 0.6f, StopReason.GEAR_P))
private val recovery = HomeUiState(phase = HomePhase.NEEDS_RECOVERY, recoveryExtraKm = "3.2")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp")
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `자동 종료 안내에서 계속 기록은 1탭`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent { AutoLogTheme { HomeScreen(autoEnding, onAction = { actions += it }) } }

        compose.onNodeWithText("계속 기록").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.KeepRecording), actions)
    }

    @Test
    fun `삭제는 한 번 더 확인한다`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent { AutoLogTheme { HomeScreen(recovery, onAction = { actions += it }) } }

        compose.onNodeWithText("삭제").performClick()
        compose.onNodeWithText("지우기").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.Recover(RecoveryChoice.DELETE)), actions)
    }

    @Test
    fun `알림 줄 1탭으로 처리 화면을 열고, 놓친 운행은 줄 안에서 추가한다`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent { AutoLogTheme { WindowLayoutHost { HomeScreen(ready, onAction = { actions += it }) } } }

        // 알림이 많으면 알림 칸이 스크롤된다 — 사람처럼 줄을 보이게 한 뒤 누른다
        compose.onNodeWithText("운행 1건 목적 확인").performScrollTo().performClick()
        compose.onNodeWithText("영수증 1장 도착").performScrollTo().performClick()
        compose.onNodeWithText("추가").performScrollTo().performClick()

        assertEquals(listOf(HomeAction.OpenReview, HomeAction.OpenReceipt, HomeAction.AcceptMissedTrip), actions)
    }

    @Test
    fun `타일 4개 — 운행 기록 조회·설정·기록 시작`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent {
            AutoLogTheme { WindowLayoutHost { HomeScreen(readyQuiet, onAction = { actions += it }) } }
        }

        compose.onNodeWithText("운행 기록 조회").performClick()
        compose.onNodeWithText("설정").performClick()
        compose.onNodeWithText("기록 시작").performClick()

        assertEquals(listOf(HomeAction.OpenRecords, HomeAction.OpenSettings, HomeAction.StartTrip), actions)
    }

    @Test
    fun `기록 중에는 같은 자리의 4번째 타일이 기록 종료`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent { AutoLogTheme { WindowLayoutHost { HomeScreen(recording, onAction = { actions += it }) } } }

        compose.onNodeWithText("기록 시작").assertDoesNotExist()
        compose.onNodeWithText("기록 종료").performClick()

        assertEquals(listOf<HomeAction>(HomeAction.EndNow), actions)
    }

    @Test
    fun `알림이 없어도 알림 칸은 남아 새 알림이 없다고 말한다`() {
        compose.setContent { AutoLogTheme { WindowLayoutHost { HomeScreen(readyQuiet, onAction = {}) } } }

        compose.onNodeWithText("알림").assertIsDisplayed()
        compose.onNodeWithText("새 알림이 없어요").assertIsDisplayed()
        compose.onNodeWithText("최근 운행 정보").assertIsDisplayed()
    }

    @Test
    fun `삭제 확인에서 X를 누르면 지우지 않고 앞 단계로 돌아간다`() {
        val actions = mutableListOf<HomeAction>()
        compose.setContent { AutoLogTheme { HomeScreen(recovery, onAction = { actions += it }) } }

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
    fun autoEnding() = captureAll("home_autoend", driver = SampleDriver) { HomeScreen(autoEnding, onAction = {}) }

    @Test
    fun recovery() = captureAll("home_recovery", driver = SampleDriver) { HomeScreen(recovery, onAction = {}) }
}
