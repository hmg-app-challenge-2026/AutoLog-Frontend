package kr.autolog.vehicle.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.data.repository.ProfileBadge
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.ActionTile
import kr.autolog.vehicle.ui.design.components.ProfileTile
import kr.autolog.vehicle.ui.design.components.ScrimSheet
import kr.autolog.vehicle.ui.design.components.TertiaryButton

@Composable
fun ProfilePickerRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { ProfilePickerViewModel(graph.session, graph.profile, graph.navigator) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    ProfilePickerScreen(state, vm::onAction)
}

/**
 * 스펙 7.1. 제목과 타일이 화면 가운데 주인공이다(넷플릭스식). 타일 순서는 [ProfilePickerUiState.tiles]가 정한다.
 * WIDE = 한 줄, NARROW·TALL = 3열.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilePickerScreen(
    state: ProfilePickerUiState,
    onAction: (ProfilePickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val layout = LocalWindowLayout.current
    // WIDE 200 dp·한 줄 / TALL 160 dp·3열 / NARROW(분할 화면 한 칸) 128 dp·3열 — 세 개가 스크롤 없이 한 줄에 보이게
    val tile =
        when (layout) {
            WindowLayout.WIDE -> Size.profileTileWide
            WindowLayout.TALL -> Size.profileTileNarrow
            WindowLayout.NARROW -> Size.profileTileCompact
        }
    val gap = if (layout == WindowLayout.NARROW) Space.s5 else Space.s6
    BoxWithConstraints(modifier.fillMaxSize()) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(Space.s6),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s6, Alignment.CenterVertically),
        ) {
            Text(
                // 분할 화면 한 칸(NARROW)은 한 줄에 안 들어간다 — 어절 단위 두 줄을 문자열로 정해 둔다
                // (Phrase 줄바꿈은 기기에선 되지만 Robolectric 스크린샷에선 글자 단위로 꺾인다)
                stringResource(
                    if (layout ==
                        WindowLayout.NARROW
                    ) {
                        R.string.picker_title_two_lines
                    } else {
                        R.string.picker_title
                    },
                ),
                style = t.display,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
            if (state.loaded && state.profiles.isEmpty()) {
                Text(stringResource(R.string.picker_empty), style = t.body, color = c.textSecondary)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(Space.s5),
                maxItemsInEachRow = if (layout == WindowLayout.WIDE) Int.MAX_VALUE else 3,
            ) {
                state.tiles.forEach { PickerTileOf(it, tile, onAction) }
            }
            TertiaryButton(stringResource(R.string.picker_later), onClick = { onAction(ProfilePickerAction.Later) })
        }
        if (state.showAll) AllProfilesSheet(state.profiles, onAction)
        state.addForm?.let { AddDriverSheet(it, onAction) }
    }
}

@Composable
private fun PickerTileOf(tile: PickerTile, size: Dp, onAction: (ProfilePickerAction) -> Unit) = when (tile) {
    is PickerTile.Person -> ProfileTileOf(tile.profile, size) {
        onAction(ProfilePickerAction.Select(tile.profile.userId))
    }
    PickerTile.More ->
        ActionTile(Icons.Rounded.MoreHoriz, stringResource(R.string.picker_more), onClick = {
            onAction(ProfilePickerAction.ShowAll)
        }, tileSize = size)
    PickerTile.Add ->
        ActionTile(Icons.Rounded.Add, stringResource(R.string.picker_add), onClick = {
            onAction(ProfilePickerAction.OpenAdd)
        }, tileSize = size)
}

@Composable
private fun AllProfilesSheet(profiles: List<DriverProfile>, onAction: (ProfilePickerAction) -> Unit) {
    val c = AutoLogTheme.colors
    ScrimSheet {
        Text(stringResource(R.string.picker_all_title), style = AutoLogTheme.type.headline, color = c.textPrimary)
        Spacer(Modifier.height(Space.s5))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Size.profileTileNarrow),
            horizontalArrangement = Arrangement.spacedBy(Space.s5),
            verticalArrangement = Arrangement.spacedBy(Space.s5),
            modifier = Modifier.fillMaxWidth().height(Size.profileTileWide * 2),
        ) {
            items(profiles, key = { it.userId }) { p ->
                // 뒤 화면의 같은 사람 타일과 shared element 키가 겹치지 않게 시트 쪽은 끈다
                ProfileTileOf(p, Size.profileTileNarrow, sharedKey = null) {
                    onAction(ProfilePickerAction.Select(p.userId))
                }
            }
        }
        TertiaryButton(stringResource(R.string.action_cancel), onClick = { onAction(ProfilePickerAction.HideAll) })
    }
}

@Composable
private fun ProfileTileOf(p: DriverProfile, size: Dp, sharedKey: String? = p.userId, onClick: () -> Unit) = ProfileTile(
    userId = p.userId,
    name = p.name,
    dept = p.dept,
    scheduled = p.badge == ProfileBadge.SCHEDULED,
    recent = p.badge == ProfileBadge.RECENT,
    onClick = onClick,
    tileSize = size,
    sharedKey = sharedKey,
)
