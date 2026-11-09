package kr.autolog.vehicle.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.core.QrResult
import kr.autolog.vehicle.data.repository.ReceiptRepository
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

data class ReceiptQrUiState(
    val loading: Boolean = true,
    val url: String? = null,
    val expiresAt: Long = 0,
    val now: Long = 0,
    val failed: Boolean = false,
) {
    val expired: Boolean get() = url != null && now >= expiresAt
    val remainingText: String get() {
        val seconds = ((expiresAt - now).coerceAtLeast(0) + 999) / 1_000
        return "%d:%02d".format(seconds / 60, seconds % 60)
    }
}

sealed interface ReceiptQrAction {
    data object Retry : ReceiptQrAction
    data object ManualEntry : ReceiptQrAction
    data object Back : ReceiptQrAction
}

class ReceiptQrViewModel(
    private val receipts: ReceiptRepository,
    private val navigator: Navigator,
    private val clock: AppClock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReceiptQrUiState(now = clock.nowMs()))
    val uiState: StateFlow<ReceiptQrUiState> = _uiState.asStateFlow()
    private var issued = false
    private var left = false

    init {
        viewModelScope.launch {
            receipts.inbox.collect { inbox ->
                if (left) return@collect
                val latest = inbox.maxByOrNull { it.takenAt ?: "" }
                if (latest != null) {
                    left = navigator.replaceTop(Route.FuelEntry(latest.receiptId))
                } else if (!issued) {
                    issued = true
                    issue()
                }
            }
        }
        viewModelScope.launch {
            tickerFlow(clock).collect { now -> _uiState.update { it.copy(now = now) } }
        }
    }

    fun onAction(action: ReceiptQrAction) {
        when (action) {
            ReceiptQrAction.Retry -> viewModelScope.launch { issue() }
            ReceiptQrAction.ManualEntry -> {
                left = navigator.push(Route.FuelEntry())
            }
            ReceiptQrAction.Back -> {
                left = navigator.pop()
            }
        }
    }

    private suspend fun issue() {
        _uiState.update { it.copy(loading = true, failed = false, url = null) }
        when (val result = receipts.issueQr()) {
            is QrResult.Issued -> {
                // 가짜 서버의 무기한 QR은 화면 시연을 위해 10분으로 표시한다.
                val expiry = if (result.expiresAt == Long.MAX_VALUE) clock.nowMs() + 600_000 else result.expiresAt
                _uiState.update { it.copy(loading = false, url = result.url, expiresAt = expiry, now = clock.nowMs()) }
            }
            is QrResult.Failed -> _uiState.update { it.copy(loading = false, failed = true) }
        }
    }
}
