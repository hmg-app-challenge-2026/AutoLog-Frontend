package kr.autolog.vehicle.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import org.robolectric.RuntimeEnvironment

/** 스펙 10.2 잠정 크기 [V-01]. FormFactorConsole 측정 후 바꾼다. */
enum class ShotSize(val qualifier: String) {
    WIDE("w1024dp-h640dp"),
    NARROW("w504dp-h640dp"),
    TALL("w720dp-h1100dp"),
}

/** 레이아웃 × 테마마다 한 장씩. 파일: src/test/screenshots/<name>_<size>_<theme>.png */
fun captureAll(name: String, sizes: List<ShotSize> = ShotSize.entries, content: @Composable () -> Unit) {
    for (size in sizes) {
        for (dark in listOf(true, false)) {
            RuntimeEnvironment.setQualifiers("${size.qualifier}-${if (dark) "night" else "notnight"}-xhdpi")
            val theme = if (dark) "dark" else "light"
            captureRoboImage("src/test/screenshots/${name}_${size.name.lowercase()}_$theme.png") {
                AutoLogTheme(darkTheme = dark) {
                    Box(Modifier.fillMaxSize().background(AutoLogTheme.colors.bg)) {
                        WindowLayoutHost { content() }
                    }
                }
            }
        }
    }
}
