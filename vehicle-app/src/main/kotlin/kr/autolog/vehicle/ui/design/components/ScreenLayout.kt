package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kr.autolog.vehicle.ui.design.LocalTopEndReserved
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.verticalScrollFade

/**
 * 화면 맨 위 줄(높이 76 dp) — 상태 줄(Home)이나 화면 제목이 놓인다. 오른쪽은 운전자 배지 자리로 비운다([LocalTopEndReserved]).
 * 모든 전체 화면이 같은 높이·같은 자리에서 시작하게 하는 공용 뼈대.
 */
@Composable
fun TopLine(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().heightIn(min = Size.touchMin).padding(end = LocalTopEndReserved.current),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 위쪽 정렬로 쌓고, 넘치면 스크롤 + 더 있는 쪽 가장자리를 흐린다(디자인 언어 9장). 높이는 부모가 정한다.
 * 화면이 스크롤 위치를 움직여야 하면(예: 수정 모드에서 선택지를 보이게) [scroll]을 넘긴다.
 */
@Composable
fun ScrollColumn(
    modifier: Modifier = Modifier,
    scroll: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.verticalScrollFade(scroll, Space.s7).verticalScroll(scroll), content = content)
}
