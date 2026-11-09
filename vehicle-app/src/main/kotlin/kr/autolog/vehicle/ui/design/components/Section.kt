package kr.autolog.vehicle.ui.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.Space

/**
 * 디자인 언어 11장 SectionHeader — 한 화면 안 영역의 소제목("이번 달 운행 정보", "알림"). 모든 섹션이 같은 모양:
 * `section`(22 sp 굵게), `textSecondary`, 아래 `space.3`. 크기 대신 굵기로 제목임을 드러낸다 —
 * 섹션 안에는 이보다 큰 회색 설명 글씨를 두지 않는다(데이터 값과 카드 안 목록 항목은 예외).
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier.fillMaxWidth().padding(bottom = Space.s3),
        style = AutoLogTheme.type.section,
        color = AutoLogTheme.colors.textSecondary,
    )
}
