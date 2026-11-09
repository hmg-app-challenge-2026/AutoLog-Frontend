package kr.autolog.vehicle.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import kr.autolog.vehicle.R
import kr.autolog.vehicle.ui.design.DriverSide
import kr.autolog.vehicle.ui.design.LocalDriverSide
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.components.DockTile

/** 타일 하나. [emphasized] = 같은 자리에서 상태가 바뀐 주 동작(파란 채움). */
private data class DockItem(
    val icon: ImageVector,
    @StringRes val label: Int,
    val action: HomeAction,
    val emphasized: Boolean = false,
)

private val FUEL = DockItem(Icons.Rounded.LocalGasStation, R.string.action_fuel_add, HomeAction.AddFuelRecord)
private val RECORDS = DockItem(Icons.Rounded.Description, R.string.action_records, HomeAction.OpenRecords)
private val SETTINGS = DockItem(Icons.Rounded.Settings, R.string.action_settings, HomeAction.OpenSettings)
private val START = DockItem(Icons.Rounded.PlayArrow, R.string.home_start_manual, HomeAction.StartTrip)
private val END = DockItem(Icons.Rounded.Stop, R.string.home_end, HomeAction.EndNow, emphasized = true)

/** 한 줄에 둘 타일 수. */
private const val DOCK_COLUMNS_WIDE = 4
private const val DOCK_COLUMNS_NARROW = 2

/**
 * 맨 아래 타일 4개(D-24): [주유 기록 추가] · [운행 기록 조회] · [설정] · [기록 시작].
 * 모두 같은 [DockTile] — 크기·모양·간격이 같고 아이콘만 다르다. 기록 중에는 4번째가 같은 자리에서 [기록 종료](파란 채움).
 * 이름이 한 줄에 들어가는 폭이면 4칸 한 줄, 아니면 2×2. 순서는 `driverSide`(우핸들이면 좌우 반전).
 */
@Composable
internal fun HomeDock(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    if (state.phase == HomePhase.NEEDS_RECOVERY) return
    val last = if (state.phase == HomePhase.READY) START else END
    val ordered = listOf(FUEL, RECORDS, SETTINGS, last)
    val items = if (LocalDriverSide.current == DriverSide.START) ordered else ordered.reversed()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fitsOneRow = maxWidth >= Size.dockTileMinWidth * DOCK_COLUMNS_WIDE + Space.s4 * (DOCK_COLUMNS_WIDE - 1)
        val columns = if (fitsOneRow) DOCK_COLUMNS_WIDE else DOCK_COLUMNS_NARROW
        Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s4)) {
                    row.forEach { item ->
                        DockTile(
                            item.icon,
                            stringResource(item.label),
                            onClick = { onAction(item.action) },
                            modifier = Modifier.weight(1f),
                            emphasized = item.emphasized,
                        )
                    }
                }
            }
        }
    }
}
