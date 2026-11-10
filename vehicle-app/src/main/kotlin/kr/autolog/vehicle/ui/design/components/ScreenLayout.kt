package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalTopEndReserved
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
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

/**
 * 보조 화면(주유·운행 기록·운행기록부 …) 맨 위 머리 줄: 가운데 제목 + 왼쪽 닫기(X). 오른쪽 위는 운전자 배지 자리.
 * - 넓은 창: 배지와 같은 줄. 제목 양옆을 똑같이(배지 폭과 X 폭 중 큰 쪽) 비워 화면 가운데를 지키면서 X·배지와 겹치지 않는다.
 * - 좁은·세로 창: 폭이 모자라 배지 아래로 한 줄(76 dp) 내리고, 양옆은 X 폭만 비운다. 긴 제목은 두 줄까지.
 * 부모 Column에 형제로 놓인다(부모 줄 간격을 그대로 쓴다).
 */
@Composable
fun ScreenHeader(title: String, onClose: () -> Unit) {
    val wide = LocalWindowLayout.current == WindowLayout.WIDE
    if (!wide) Spacer(Modifier.height(Size.touchMin))
    val side = if (wide) maxOf(Size.touchMin, LocalTopEndReserved.current) else Size.touchMin
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = side),
            style = AutoLogTheme.windowType.headerTitle,
            color = AutoLogTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        CloseButton(onClose, Modifier.align(Alignment.CenterStart))
    }
}
