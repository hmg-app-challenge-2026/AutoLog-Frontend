package kr.autolog.vehicle.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.takahirom.roborazzi.captureRoboImage
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.WindowLayoutHost
import kr.autolog.vehicle.ui.root.BadgeScaffold
import org.robolectric.RuntimeEnvironment

/** 스펙 10.2 잠정 크기 [V-01]. FormFactorConsole 측정 후 바꾼다. */
enum class ShotSize(val qualifier: String) {
    WIDE("w1024dp-h640dp"),
    NARROW("w504dp-h640dp"),
    TALL("w720dp-h1100dp"),
}

/** 실제 앱처럼 오른쪽 위 운전자 배지를 얹을 때. [clickable]=false는 Peek(표시 전용). */
data class ShotDriver(val userId: String?, val name: String?, val clickable: Boolean = true)

/** 스크린샷 공통 운전자(배차된 김영업). */
val SampleDriver = ShotDriver("u-1", "김영업")

/** 레이아웃 × 테마마다 한 장씩. 파일: src/test/screenshots/<name>_<size>_<theme>.png. [driver]가 있으면 실제 앱과 같은 [BadgeScaffold]로 감싼다. */
fun captureAll(
    name: String,
    sizes: List<ShotSize> = ShotSize.entries,
    driver: ShotDriver? = null,
    content: @Composable () -> Unit,
) {
    for (size in sizes) {
        for (dark in listOf(true, false)) {
            RuntimeEnvironment.setQualifiers("${size.qualifier}-${if (dark) "night" else "notnight"}-xhdpi")
            val theme = if (dark) "dark" else "light"
            captureRoboImage("src/test/screenshots/${name}_${size.name.lowercase()}_$theme.png") {
                // 반복 상태 모션(경로 화살표 등)은 멈춘 그림으로 — 무한 애니메이션은 캡처를 끝나지 않게 한다
                AutoLogTheme(darkTheme = dark, ambientMotion = false) {
                    Box(Modifier.fillMaxSize().background(AutoLogTheme.colors.bg)) {
                        WindowLayoutHost {
                            if (driver == null) {
                                content()
                            } else {
                                BadgeScaffold(driver.userId, driver.name, if (driver.clickable) ({}) else null, content)
                            }
                        }
                    }
                }
            }
        }
    }
}
