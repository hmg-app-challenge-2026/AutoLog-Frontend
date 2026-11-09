package kr.autolog.vehicle.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.data.repository.ProfileBadge
import kr.autolog.vehicle.data.repository.UnassignedTripItem
import kr.autolog.vehicle.screenshot.captureAll
import kr.autolog.vehicle.ui.design.AutoLogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val profiles =
    listOf(
        DriverProfile("u-1", "김영업", "영업1팀", ProfileBadge.SCHEDULED),
        DriverProfile("u-2", "이현장", "기술지원팀", ProfileBadge.RECENT),
        DriverProfile("u-3", "박관리", "총무팀"),
    )

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1024dp-h640dp") // 기본 320×470dp에선 타일이 화면 밖으로 밀려 클릭이 안 닿는다
class ProfilePickerScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `조작 예산 — 운전자 선택은 1탭`() {
        val actions = mutableListOf<ProfilePickerAction>()
        compose.setContent {
            AutoLogTheme { ProfilePickerScreen(ProfilePickerUiState(profiles, loaded = true), onAction = { actions += it }) }
        }

        compose.onNodeWithText("이현장").performClick()

        assertEquals(listOf(ProfilePickerAction.Select("u-2")), actions)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProfilePickerScreenshotTest {
    @Test
    fun picker() =
        captureAll("picker") {
            ProfilePickerScreen(
                ProfilePickerUiState(profiles, UnassignedTripItem("t-9", "09:00 본사 → 판교 거래처 · 23.4 km", profiles), loaded = true),
                onAction = {},
            )
        }
}
