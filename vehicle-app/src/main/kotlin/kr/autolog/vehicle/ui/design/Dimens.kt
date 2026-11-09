package kr.autolog.vehicle.ui.design

import androidx.compose.ui.unit.dp

/** 디자인 언어 6.4 (8 dp 그리드). 문서 space.N = Space.sN */
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 24.dp
    val s6 = 32.dp
    val s7 = 48.dp
    val s8 = 64.dp
    val s9 = 96.dp
    val tallMargin = 40.dp
}

/** 디자인 언어 6.5. */
object Size {
    val touchMin = 76.dp
    val buttonMd = 76.dp
    val buttonLg = 96.dp
    val buttonXl = 120.dp
    val iconButton = 96.dp
    val iconSm = 28.dp // 닫기(X)처럼 작은 원 안의 아이콘
    val icon = 36.dp
    val iconLg = 48.dp
    val avatarChip = 48.dp
    val avatarSmall = 64.dp
    val avatarLarge = 96.dp // 팝업 머리 아바타(새 운전자 미리보기)
    val chipHeight = 76.dp // StatusChip 터치 최소 높이(시각 높이 56 dp는 안쪽 여백으로)
    val profileTileWide = 200.dp
    val profileTileNarrow = 160.dp
    val profileTileCompact = 128.dp
    val peekMaxWidth = 560.dp
    val sheetMaxWidth = 960.dp
    val dialogMaxWidth = 640.dp // ModalCard 기본 최대 폭
    val hairline = 1.dp
    val progressBar = 8.dp
    val ring = 3.dp
}

/** 디자인 언어 6.6. */
object Radius {
    val sm = 14.dp
    val md = 20.dp
    val lg = 28.dp
    val xl = 40.dp
}
