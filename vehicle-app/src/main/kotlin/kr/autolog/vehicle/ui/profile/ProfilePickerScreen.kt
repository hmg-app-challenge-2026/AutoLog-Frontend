package kr.autolog.vehicle.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.autolog.vehicle.R
import kr.autolog.vehicle.data.repository.DriverProfile
import kr.autolog.vehicle.data.repository.ProfileBadge
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalInputLocked
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.Avatar
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
 * 스펙 7.1. 제목과 타일이 화면 가운데 주인공이다(넷플릭스식).
 * 미확정 운행 질문(F-20)은 보조라서 아래쪽 한 줄로 둔다. WIDE = 타일 한 줄 + 하단 가로 배치, NARROW·TALL = 2열 + 세로 쌓기.
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
    val wide = layout == WindowLayout.WIDE
    // WIDE 200 dp·한 줄 / TALL 160 dp·3열 / NARROW(분할 화면 한 칸) 128 dp·3열 — 세 명이 스크롤 없이 한 줄에 보이게
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
                stringResource(R.string.picker_title),
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
                maxItemsInEachRow = if (wide) PICKER_VISIBLE_MAX + 1 else 3,
            ) {
                state.visible.forEach { p -> ProfileTileOf(p, tile) { onAction(ProfilePickerAction.Select(p.userId)) } }
                if (state.hasMore) MoreTile(tile) { onAction(ProfilePickerAction.ShowAll) }
            }
            val later = @Composable {
                TertiaryButton(stringResource(R.string.picker_later), onClick = { onAction(ProfilePickerAction.Later) })
            }
            val previous =
                @Composable {
                    state.unassigned?.let { item ->
                        UnassignedRow(item.label, item.candidates, stacked = !wide) {
                            onAction(ProfilePickerAction.AssignPrevious(item.tripId, it))
                        }
                    }
                }
            if (wide) {
                Row(
                    Modifier.widthIn(max = Size.sheetMaxWidth).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.s6),
                ) {
                    later()
                    Spacer(Modifier.weight(1f))
                    previous()
                }
            } else {
                previous()
                later()
            }
        }
        if (state.showAll) {
            ScrimSheet {
                Text(stringResource(R.string.picker_all_title), style = t.headline, color = c.textPrimary)
                Spacer(Modifier.height(Space.s5))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Size.profileTileNarrow),
                    horizontalArrangement = Arrangement.spacedBy(Space.s5),
                    verticalArrangement = Arrangement.spacedBy(Space.s5),
                    modifier = Modifier.fillMaxWidth().height(Size.profileTileWide * 2),
                ) {
                    items(state.profiles, key = { it.userId }) { p ->
                        ProfileTileOf(p, Size.profileTileNarrow) { onAction(ProfilePickerAction.Select(p.userId)) }
                    }
                }
                TertiaryButton(stringResource(R.string.action_cancel), onClick = {
                    onAction(ProfilePickerAction.HideAll)
                })
            }
        }
    }
}

@Composable
private fun ProfileTileOf(p: DriverProfile, size: Dp, onClick: () -> Unit) = ProfileTile(
    userId = p.userId,
    name = p.name,
    dept = p.dept,
    scheduled = p.badge == ProfileBadge.SCHEDULED,
    recent = p.badge == ProfileBadge.RECENT,
    onClick = onClick,
    tileSize = size,
)

@Composable
private fun MoreTile(size: Dp, onClick: () -> Unit) {
    val c = AutoLogTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s3)) {
        Box(
            Modifier
                .size(size - Space.s4)
                .clip(RoundedCornerShape(Radius.xl))
                .background(c.surface2)
                .clickable(enabled = !LocalInputLocked.current, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.picker_more),
                tint = c.textSecondary,
                modifier = Modifier.size(Size.iconLg),
            )
        }
        Text(stringResource(R.string.picker_more), style = AutoLogTheme.type.title, color = c.textSecondary)
    }
}

/**
 * 스펙 7.1 ④ / F-20: 미확정 운행의 운전자를 후보 1탭으로 지정. 보조 질문이라 작게(아바타 64 dp, 터치 76 dp).
 * 같은 사람이 위 타일에도 있으므로 shared element는 쓰지 않는다.
 */
@Composable
private fun UnassignedRow(label: String, candidates: List<DriverProfile>, stacked: Boolean, onPick: (String) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val question =
        @Composable {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                Text(stringResource(R.string.picker_unassigned_title), style = t.label, color = c.textPrimary)
                Text(label, style = t.caption, color = c.textSecondary)
            }
        }
    val picks = @Composable { candidates.forEach { p -> SmallPick(p) { onPick(p.userId) } } }
    val box = Modifier.clip(RoundedCornerShape(Radius.md)).background(c.surface1).padding(Space.s4)
    if (stacked) {
        // 좁은 창: 질문 위, 후보 아래
        Column(box, verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            question()
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s5)) { picks() }
        }
    } else {
        Row(
            box,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s5),
        ) {
            question()
            picks()
        }
    }
}

@Composable
private fun SmallPick(p: DriverProfile, onClick: () -> Unit) {
    val c = AutoLogTheme.colors
    Column(
        Modifier
            .widthIn(min = Size.touchMin)
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(enabled = !LocalInputLocked.current, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        Avatar(p.userId, p.name, Size.avatarSmall)
        Text(p.name, style = AutoLogTheme.type.caption, color = c.textPrimary, maxLines = 1)
    }
}
