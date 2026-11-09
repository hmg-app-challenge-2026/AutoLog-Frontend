package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 SectionHeader — 한 화면 안 영역의 소제목("최근 운행 정보", "알림"). 모든 섹션이 같은 모양:
 * `caption`, `textSecondary`, 아래 `space.3`. 내용보다 한 단계 낮아 영역 이름표로만 읽힌다.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.fillMaxWidth().padding(bottom = Space.s3),
        style = AutoLogTheme.type.caption,
        color = AutoLogTheme.colors.textSecondary,
    )
}
