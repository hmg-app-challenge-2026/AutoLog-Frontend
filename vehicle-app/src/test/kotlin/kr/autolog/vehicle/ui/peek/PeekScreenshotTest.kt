package kr.autolog.vehicle.ui.peek

import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun recording() =
        captureAll("peek_recording") { PeekScreen(PeekUiState.Recording("김영업", "23.4", "판교 테크노밸리 거래처", 10, null)) }

    @Test
    fun autoEnd() = captureAll("peek_autoend") { PeekScreen(PeekUiState.Recording("김영업", "23.4", null, 4, 161)) }

    @Test
    fun noTrip() = captureAll("peek_notrip") { PeekScreen(PeekUiState.NoTrip("18,420.5")) }
}
