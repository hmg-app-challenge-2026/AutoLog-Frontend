package kr.autolog.vehicle.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.screenshot.SampleDriver
import kr.autolog.vehicle.screenshot.ShotDriver
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val ready =
    HomeUiState(
        phase = HomePhase.READY,
        odometerKm = "18,420.5",
        reviewCount = 1,
        receiptCount = 1,
        missedTripKm = "15.0",
    )

private val recording =
    HomeUiState(
        phase = HomePhase.RECORDING_PARKED,
        tripKm = "23.4",
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
    fun noDriver() = captureAll("home_nodriver", driver = ShotDriver(null, null)) { HomeScreen(ready, onAction = {}) }

    @Test
    fun recording() = captureAll("home_recording", driver = SampleDriver) { HomeScreen(recording, onAction = {}) }

    @Test
    fun autoEnding() = captureAll("home_autoend", driver = SampleDriver) { HomeScreen(autoEnding, onAction = {}) }

    @Test
    fun recovery() = captureAll("home_recovery", driver = SampleDriver) { HomeScreen(recovery, onAction = {}) }
}
