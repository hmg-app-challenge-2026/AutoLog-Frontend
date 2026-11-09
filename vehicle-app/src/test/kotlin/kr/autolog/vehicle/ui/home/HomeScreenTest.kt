package kr.autolog.vehicle.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.data.interim.RecoveryChoice
import kr.autolog.vehicle.data.interim.StopReason
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val driver = DriverProfile("u-1", "김영업", "영업1팀")
private val recording =
    HomeUiState(phase = HomePhase.RECORDING_PARKED, driver = driver, tripKm = "23.4", destination = "판교 테크노밸리 거래처", elapsedMin = 35)
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
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class HomeScreenshotTest {
    @Test
    fun ready() =
        captureAll("home_ready") {
            HomeScreen(
                HomeUiState(phase = HomePhase.READY, driver = driver, odometerKm = "18,420.5", reviewCount = 1, missedTripKm = "15.0"),
                onAction = {},
            )
        }

    @Test
    fun recording() = captureAll("home_recording") { HomeScreen(recording, onAction = {}) }

    @Test
    fun autoEnding() = captureAll("home_autoend") { HomeScreen(autoEnding, onAction = {}) }

    @Test
    fun recovery() = captureAll("home_recovery") { HomeScreen(recovery, onAction = {}) }
}
