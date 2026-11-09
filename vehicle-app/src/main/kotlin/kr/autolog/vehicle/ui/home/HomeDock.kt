package kr.autolog.vehicle.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.DriverSide
import kr.autolog.vehicle.ui.design.LocalDriverSide
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.DockButton
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.QuickAction
import kr.autolog.vehicle.ui.design.components.TertiaryButton

/** 독의 동작 하나. [labelInWide]가 false면 넓은 창에서도 아이콘만(자주 쓰지 않는 설정). */
private data class DockItem(
    val icon: ImageVector,
    @StringRes val label: Int,
    val action: HomeAction,
    val labelInWide: Boolean = true,
)

/** 상태와 무관하게 늘 같은 자리 — 주유 기록 추가 · 운행 기록 · 설정(D-23). */
private val DOCK_ITEMS =
    listOf(
        DockItem(Icons.Rounded.LocalGasStation, R.string.action_fuel_add, HomeAction.AddFuelRecord),
        DockItem(Icons.Rounded.Description, R.string.action_records, HomeAction.OpenRecords),
        DockItem(Icons.Rounded.Settings, R.string.action_settings, HomeAction.OpenSettings, labelInWide = false),
    )

/**
 * 맨 아래 독. 끝 동작은 상태에 따라: 대기 = 작은 "직접 시작"(보통은 출발하면 자동), 기록 중 = [기록 종료](주 버튼), 복구 중 = 없음.
 * WIDE는 가로 한 줄(순서는 `driverSide` — 좌핸들이면 동작이 왼쪽·끝 동작이 오른쪽).
 * NARROW·TALL은 끝 동작을 위에 전폭으로 두고, 그 아래 원형 아이콘 3개.
 */
@Composable
internal fun HomeDock(state: HomeUiState, onAction: (HomeAction) -> Unit, wide: Boolean) {
    if (state.phase == HomePhase.NEEDS_RECOVERY) return
    val recording = state.phase != HomePhase.READY
    if (wide) {
        val items =
            @Composable {
                DOCK_ITEMS.forEach { item ->
                    DockButton(item.icon, stringResource(item.label), {
                        onAction(item.action)
                    }, showLabel = item.labelInWide)
                }
            }
        // 끝 동작: 기록 중이면 [기록 종료]가 남은 폭을 다 쓰는 주 버튼, 대기면 작은 "직접 시작"
        val trailing: @Composable RowScope.() -> Unit = {
            if (recording) {
                PrimaryButton(
                    stringResource(R.string.home_end),
                    onClick = { onAction(HomeAction.EndNow) },
                    modifier = Modifier.weight(1f),
                    height = Size.buttonMd,
                )
            } else {
                Spacer(Modifier.weight(1f))
                TertiaryButton(stringResource(R.string.home_start_manual), onClick = { onAction(HomeAction.StartTrip) })
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            if (LocalDriverSide.current == DriverSide.START) {
                items()
                trailing()
            } else {
                trailing()
                items()
            }
        }
    } else {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s4),
        ) {
            if (recording) {
                PrimaryButton(
                    stringResource(R.string.home_end),
                    onClick = { onAction(HomeAction.EndNow) },
                    modifier = Modifier.fillMaxWidth(),
                    // 분할 화면 한 칸은 높이가 모자라 76 dp, 세로가 넉넉한 TALL은 96 dp
                    height = if (LocalWindowLayout.current == WindowLayout.NARROW) Size.buttonMd else Size.buttonLg,
                )
            } else {
                TertiaryButton(stringResource(R.string.home_start_manual), onClick = { onAction(HomeAction.StartTrip) })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                DOCK_ITEMS.forEach { item ->
                    QuickAction(item.icon, stringResource(item.label), onClick = { onAction(item.action) })
                }
            }
        }
    }
}
