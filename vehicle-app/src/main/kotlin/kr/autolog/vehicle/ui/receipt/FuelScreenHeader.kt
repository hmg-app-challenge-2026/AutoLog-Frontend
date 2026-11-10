package kr.autolog.vehicle.ui.receipt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.CloseButton

/**
 * 주유 화면(QR · 입력 창) 공용 머리 줄: 가운데 제목 + 왼쪽 닫기(X).
 * 좁은 창은 오른쪽 위 운전자 배지가 제목 줄을 덮지 않게 한 줄(76 dp) 내려 놓는다.
 * 부모 Column에 형제로 놓인다(부모의 줄 간격을 그대로 쓴다).
 */
@Composable
internal fun FuelScreenHeader(title: String, onClose: () -> Unit) {
    if (LocalWindowLayout.current == WindowLayout.NARROW) Spacer(Modifier.height(Size.touchMin))
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(title, style = AutoLogTheme.type.title, color = AutoLogTheme.colors.textPrimary)
        CloseButton(onClose, Modifier.align(Alignment.CenterStart))
    }
}
