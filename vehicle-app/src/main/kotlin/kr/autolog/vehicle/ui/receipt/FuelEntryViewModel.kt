package kr.autolog.vehicle.ui.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.autolog.shared.OcrStatus
import kr.autolog.shared.ReceiptFields
import kr.autolog.shared.ReceiptPayload
import kr.autolog.vehicle.core.ConfirmResult
import kr.autolog.vehicle.core.TripCandidate
import kr.autolog.vehicle.data.repository.FuelDraft
import kr.autolog.vehicle.data.repository.ReceiptRepository
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.repository.TripSession
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route
import kr.autolog.vehicle.ui.design.components.applyKey
import kotlin.math.abs

enum class FuelNumberField { AMOUNT, LITERS, UNIT_PRICE, DISCOUNT }

enum class FuelTextField { MERCHANT, DATETIME, FUEL_TYPE, CARD_LAST4 }

data class FuelEntryUiState(
    val loading: Boolean = false,
    val receipt: ReceiptPayload? = null,
    val amount: String = "",
    val liters: String = "",
    val unitPrice: String = "",
    val discount: String = "",
    val merchant: String = "",
    val datetime: String = "",
    val fuelType: String = "",
    val cardLast4: String = "",
    val candidates: List<TripCandidate> = emptyList(),
    val selectedTripId: String? = null,
    val numberField: FuelNumberField? = null,
    val textField: FuelTextField? = null,
    val edited: Set<String> = emptySet(),
    val saving: Boolean = false,
    val error: String? = null,
) {
    val arithOk: Boolean?
        get() {
            val source = receipt ?: return null
            if (source.ocrStatus == OcrStatus.FAILED) return null
            val l = liters.toDoubleOrNull()
            val p = unitPrice.toIntOrNull()
            val a = amount.toIntOrNull()
            if (l != null && p != null && a != null) {
                return abs(l * p - (discount.toIntOrNull() ?: 0) - a) <= 1.0
            }
            return source.arithOk
        }
    val canAdd: Boolean get() = amount.toIntOrNull()?.let { it > 0 } == true && arithOk != false && !saving

    fun fields(): ReceiptFields = ReceiptFields(
        merchant = merchant.ifBlank { null },
        datetime = datetime.ifBlank { null },
        fuelType = fuelType.ifBlank { null },
        liters = liters.toDoubleOrNull(),
        unitPrice = unitPrice.toIntOrNull(),
        discountWon = discount.toIntOrNull(),
        amountWon = amount.toIntOrNull(),
        cardLast4 = cardLast4.ifBlank { null },
    )

    fun confidence(key: String): Double? = if (key in edited) 1.0 else receipt?.confidence?.get(key)
}

sealed interface FuelEntryAction {
    data class OpenNumber(val field: FuelNumberField) : FuelEntryAction
    data class OpenText(val field: FuelTextField) : FuelEntryAction
    data class Digit(val value: Char) : FuelEntryAction
    data object Delete : FuelEntryAction
    data class TextChanged(val value: String) : FuelEntryAction
    data object CloseEditor : FuelEntryAction
    data class SelectTrip(val tripId: String?) : FuelEntryAction
    data object Add : FuelEntryAction
    data object Back : FuelEntryAction
}

class FuelEntryViewModel(
    private val receiptId: String?,
    private val receipts: ReceiptRepository,
    private val session: SessionRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FuelEntryUiState(loading = receiptId != null))
    val uiState: StateFlow<FuelEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (receiptId == null) {
                val active = session.state.first()
                val candidate = when (active) {
                    is TripSession.Recording -> TripCandidate(
                        active.trip.tripId,
                        active.trip.startTime,
                        null,
                        "현재 운행",
                        false,
                    )
                    is TripSession.AutoEnding -> TripCandidate(
                        active.trip.tripId,
                        active.trip.startTime,
                        null,
                        "현재 운행",
                        false,
                    )
                    is TripSession.PendingReview -> TripCandidate(active.tripId, 0, null, "방금 끝난 운행", false)
                    else -> null
                }
                _uiState.update { it.copy(candidates = listOfNotNull(candidate), selectedTripId = candidate?.tripId) }
            } else {
                val receipt = receipts.receipt(receiptId).first()
                if (receipt == null) {
                    _uiState.update { it.copy(loading = false, error = "영수증을 찾지 못했어요") }
                    return@launch
                }
                val candidates = receipts.candidates(receipt)
                val autoSelect = receipt.arithOk == true &&
                    receipt.confidence.isNotEmpty() &&
                    receipt.confidence.values.all { it >= 0.95 } &&
                    candidates.size == 1
                val f = receipt.fields
                _uiState.update {
                    it.copy(
                        loading = false,
                        receipt = receipt,
                        amount = f.amountWon?.toString().orEmpty(),
                        liters = f.liters?.toString().orEmpty(),
                        unitPrice = f.unitPrice?.toString().orEmpty(),
                        discount = f.discountWon?.toString().orEmpty(),
                        merchant = f.merchant.orEmpty(),
                        datetime = f.datetime.orEmpty(),
                        fuelType = f.fuelType.orEmpty(),
                        cardLast4 = f.cardLast4.orEmpty(),
                        candidates = candidates,
                        selectedTripId = if (autoSelect) candidates.single().tripId else null,
                    )
                }
            }
        }
    }

    fun onAction(action: FuelEntryAction) {
        when (action) {
            is FuelEntryAction.OpenNumber -> _uiState.update { it.copy(numberField = action.field, textField = null) }
            is FuelEntryAction.OpenText -> _uiState.update { it.copy(textField = action.field, numberField = null) }
            is FuelEntryAction.Digit -> editNumber { applyKey(it, action.value) }
            FuelEntryAction.Delete -> editNumber { it.dropLast(1) }
            is FuelEntryAction.TextChanged -> editText(action.value)
            FuelEntryAction.CloseEditor -> _uiState.update { it.copy(numberField = null, textField = null) }
            is FuelEntryAction.SelectTrip -> _uiState.update { it.copy(selectedTripId = action.tripId) }
            FuelEntryAction.Add -> add()
            FuelEntryAction.Back -> navigator.pop()
        }
    }

    private fun editNumber(change: (String) -> String) {
        _uiState.update { state ->
            when (state.numberField) {
                FuelNumberField.AMOUNT -> state.copy(
                    amount = change(state.amount),
                    edited = state.edited + "amountWon",
                    error = null,
                )
                FuelNumberField.LITERS -> state.copy(
                    liters = change(state.liters),
                    edited = state.edited + "liters",
                    error = null,
                )
                FuelNumberField.UNIT_PRICE ->
                    state.copy(unitPrice = change(state.unitPrice), edited = state.edited + "unitPrice", error = null)
                FuelNumberField.DISCOUNT ->
                    state.copy(discount = change(state.discount), edited = state.edited + "discountWon", error = null)
                null -> state
            }
        }
    }

    private fun editText(value: String) {
        _uiState.update { state ->
            when (state.textField) {
                FuelTextField.MERCHANT -> state.copy(merchant = value, edited = state.edited + "merchant")
                FuelTextField.DATETIME -> state.copy(datetime = value, edited = state.edited + "datetime")
                FuelTextField.FUEL_TYPE -> state.copy(fuelType = value, edited = state.edited + "fuelType")
                FuelTextField.CARD_LAST4 -> state.copy(
                    cardLast4 = value.filter(Char::isDigit).take(4),
                    edited =
                    state.edited + "cardLast4",
                )
                null -> state
            }
        }
    }

    private fun add() {
        val state = _uiState.value
        if (!state.canAdd) return
        _uiState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            when (val result = receipts.add(FuelDraft(state.receipt, state.fields(), state.selectedTripId))) {
                is ConfirmResult.Saved -> navigator.replaceAll(Route.Home)
                is ConfirmResult.Rejected -> _uiState.update { it.copy(saving = false, error = result.reason) }
            }
        }
    }
}
