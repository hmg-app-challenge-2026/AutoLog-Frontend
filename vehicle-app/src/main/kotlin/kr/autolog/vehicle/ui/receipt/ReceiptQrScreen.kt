package kr.autolog.vehicle.ui.receipt

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.CloseButton
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.screenMargin

@Composable
fun ReceiptQrRoute() {
    val graph = LocalAppGraph.current
    val vm = viewModel { ReceiptQrViewModel(graph.receipts, graph.navigator, graph.clock) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    ReceiptQrScreen(state, vm::onAction)
}

/** 주유 기록 추가의 기본 경로. 영수증이 도착하면 ViewModel이 입력 화면으로 자동 교체한다. */
@Composable
fun ReceiptQrScreen(state: ReceiptQrUiState, onAction: (ReceiptQrAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    BoxWithConstraints(modifier.fillMaxSize().padding(screenMargin())) {
        val qrSize = minOf(maxWidth, maxHeight) * 0.45f
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (LocalWindowLayout.current == WindowLayout.NARROW) Spacer(Modifier.height(Size.touchMin))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.fuel_qr_title), style = t.title, color = c.textPrimary)
                CloseButton({ onAction(ReceiptQrAction.Back) }, Modifier.align(Alignment.CenterStart))
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    state.loading -> CircularProgressIndicator(color = c.accent)
                    state.url != null && !state.expired -> {
                        val qr = remember(state.url) { qrBitmap(state.url) }
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_description),
                            modifier = Modifier.size(qrSize).background(Color.White),
                        )
                        Spacer(Modifier.height(Space.s5))
                        Text(
                            stringResource(R.string.receipt_qr_hint),
                            style = t.caption,
                            color = c.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.s4))
                        Text(state.remainingText, style = t.title, color = c.textPrimary)
                    }
                    else -> {
                        Text(
                            stringResource(
                                if (state.expired) R.string.receipt_qr_expired else R.string.receipt_qr_failed,
                            ),
                            style = t.body,
                            color = c.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(Space.s5))
                        PrimaryButton(
                            stringResource(if (state.expired) R.string.receipt_qr_new else R.string.action_retry),
                            onClick = { onAction(ReceiptQrAction.Retry) },
                        )
                    }
                }
            }
            SecondaryButton(
                stringResource(R.string.receipt_manual_entry),
                onClick = { onAction(ReceiptQrAction.ManualEntry) },
                modifier = Modifier.fillMaxWidth().widthIn(max = Size.dialogMaxWidth),
            )
        }
    }
}

/** ZXing 기본 여백 4모듈. URL 자체는 화면 글자로 노출하지 않는다. */
private fun qrBitmap(url: String): Bitmap {
    val matrix = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 512, 512, mapOf(EncodeHintType.MARGIN to 4))
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}
