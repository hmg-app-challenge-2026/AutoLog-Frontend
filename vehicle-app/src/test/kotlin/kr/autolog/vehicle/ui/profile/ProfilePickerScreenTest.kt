package kr.autolog.vehicle.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.autolog.vehicle.data.repository.AddDriverResult
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.data.repository.ProfileBadge
import kr.autolog.vehicle.screenshot.ShotSize
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
            AutoLogTheme {
                ProfilePickerScreen(ProfilePickerUiState(profiles, loaded = true), onAction = { actions += it })
            }
        }

        compose.onNodeWithText("이현장").performClick()

        assertEquals(listOf(ProfilePickerAction.Select("u-2")), actions)
    }

    @Test
    fun `새 운전자 추가 타일은 1탭으로 시트를 연다`() {
        val actions = mutableListOf<ProfilePickerAction>()
        compose.setContent {
            AutoLogTheme {
                ProfilePickerScreen(ProfilePickerUiState(profiles, loaded = true), onAction = { actions += it })
            }
        }

        compose.onNodeWithText("새 운전자 추가").performClick()

        assertEquals(listOf<ProfilePickerAction>(ProfilePickerAction.OpenAdd), actions)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ProfilePickerScreenshotTest {
    @Test
    fun picker() = captureAll("picker") {
        ProfilePickerScreen(ProfilePickerUiState(profiles, loaded = true), onAction = {})
    }

    @Test
    fun pickerMany() = captureAll("picker_many") {
        val many = profiles + listOf("정다은", "최민수", "한지민").mapIndexed { i, n -> DriverProfile("u-${i + 4}", n, "영업2팀") }
        ProfilePickerScreen(ProfilePickerUiState(many, loaded = true), onAction = {})
    }

    @Test
    fun addSheet() = captureAll("picker_add") {
        ProfilePickerScreen(
            ProfilePickerUiState(profiles, addForm = AddDriverForm(name = "최신입"), loaded = true),
            onAction = {},
        )
    }

    @Test
    fun addSheetError() = captureAll("picker_add_error", sizes = listOf(ShotSize.WIDE)) {
        ProfilePickerScreen(
            ProfilePickerUiState(
                profiles,
                addForm = AddDriverForm(name = "김영업", error = AddDriverResult.Rejected("이미 등록된 이름이에요")),
                loaded = true,
            ),
            onAction = {},
        )
    }
}
