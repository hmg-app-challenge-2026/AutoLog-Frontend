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

/**
 * 스크린샷 창 크기. **[PLEOS]가 기준**(2026-11-10 사용자 지시): 공식 Pleos Connect v2.1.0 에뮬레이터의 앱 영역
 * 2560×1268 dp(디스플레이 2560×1440 px · 160 dpi에서 상태 표시줄 76 px · 하단 바 96 px를 뺀 것, V-01)를 밀도 1.0으로 찍어
 * 에뮬레이터 화면과 같은 픽셀로 본다. 나머지 셋은 레이아웃 3단계를 확인하는 잠정 크기[V-01] — 분할 화면 한 칸·세로 프리셋을 재면 바꾼다.
 */
enum class ShotSize(val qualifier: String, val density: String = "xhdpi") {
    PLEOS("w2560dp-h1268dp", density = "mdpi"),
    WIDE("w1024dp-h640dp"),
    NARROW("w504dp-h640dp"),
    TALL("w720dp-h1100dp"),
}

/** 실제 앱처럼 오른쪽 위 운전자 배지를 얹을 때. [clickable]=false는 Peek(표시 전용). */
data class ShotDriver(val userId: String?, val name: String?, val clickable: Boolean = true)

/** 스크린샷 기록 위치(로컬 검토용, 커밋하지 않음 — CONVENTIONS 4장). */
const val SCREENSHOT_DIR = "src/test/screenshots"

/** 스크린샷 공통 운전자(배차된 김영업). */
val SampleDriver = ShotDriver("u-1", "김영업")

/**
 * 레이아웃 × 테마마다 한 장씩. 파일: `src/test/screenshots/<size>/<name>_<theme>.png` — 창 크기(pleos·wide·narrow·tall)마다 폴더를 나눠
 * 같은 크기끼리 넘겨 보며 비교한다(2026-11-09 사용자 결정). [driver]가 있으면 실제 앱과 같은 [BadgeScaffold]로 감싼다.
 */
fun captureAll(
    name: String,
    sizes: List<ShotSize> = ShotSize.entries,
    driver: ShotDriver? = null,
    content: @Composable () -> Unit,
) {
    for (size in sizes) {
        for (dark in listOf(true, false)) {
            RuntimeEnvironment.setQualifiers("${size.qualifier}-${if (dark) "night" else "notnight"}-${size.density}")
            val theme = if (dark) "dark" else "light"
            captureRoboImage("$SCREENSHOT_DIR/${size.name.lowercase()}/${name}_$theme.png") {
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
