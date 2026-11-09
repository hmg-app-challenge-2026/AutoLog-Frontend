package kr.autolog.vehicle.ui.peek

import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.screenshot.SampleDriver
import kr.autolog.vehicle.screenshot.captureAll
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PeekScreenshotTest {
    @Test
    fun recording() = captureAll("peek_recording", driver = PeekDriver) {
        PeekScreen(PeekUiState.Recording("23.4", "본사", "판교 테크노밸리 거래처", null))
    }

    @Test
    fun autoEnd() = captureAll("peek_autoend", driver = PeekDriver) {
        PeekScreen(PeekUiState.Recording("23.4", "본사", null, 161))
    }

    @Test
    fun noTrip() = captureAll("peek_notrip", driver = PeekDriver) { PeekScreen(PeekUiState.NoTrip("18,420.5")) }
}

/** Peek의 배지는 표시 전용(누를 수 없음, 스펙 1.1). */
private val PeekDriver = SampleDriver.copy(clickable = false)
