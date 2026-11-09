package kr.autolog.vehicle.ui.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import kr.autolog.shared.OcrStatus
import kr.autolog.vehicle.R
import kr.autolog.vehicle.di.LocalAppGraph
import kr.autolog.vehicle.ui.design.AutoLogTheme
import kr.autolog.vehicle.ui.design.LocalWindowLayout
import kr.autolog.vehicle.ui.design.Radius
import kr.autolog.vehicle.ui.design.Size
import kr.autolog.vehicle.ui.design.Space
import kr.autolog.vehicle.ui.design.WindowLayout
import kr.autolog.vehicle.ui.design.components.ChoiceRow
import kr.autolog.vehicle.ui.design.components.CloseButton
import kr.autolog.vehicle.ui.design.components.ConfidenceRow
import kr.autolog.vehicle.ui.design.components.InputField
import kr.autolog.vehicle.ui.design.components.ModalCard
import kr.autolog.vehicle.ui.design.components.NumericKeypad
import kr.autolog.vehicle.ui.design.components.PrimaryButton
import kr.autolog.vehicle.ui.design.components.ScrollColumn
import kr.autolog.vehicle.ui.design.components.SecondaryButton
import kr.autolog.vehicle.ui.design.components.tappable
import kr.autolog.vehicle.ui.design.screenMargin
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FuelEntryRoute(receiptId: String?) {
    val graph = LocalAppGraph.current
    val vm = viewModel { FuelEntryViewModel(receiptId, graph.receipts, graph.session, graph.navigator) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    FuelEntryScreen(state, vm::onAction)
}

/** OCR·직접 입력 모두 같은 필드와 저장 버튼을 쓰는 주유 기록 창. */
@Composable
fun FuelEntryScreen(state: FuelEntryUiState, onAction: (FuelEntryAction) -> Unit, modifier: Modifier = Modifier) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val layout = LocalWindowLayout.current
    var imageExpanded by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize().padding(screenMargin())) {
        val availableHeight = maxHeight
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Space.s4)) {
            if (layout == WindowLayout.NARROW) Spacer(Modifier.height(Size.touchMin))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.fuel_entry_title), style = t.title, color = c.textPrimary)
                CloseButton({ onAction(FuelEntryAction.Back) }, Modifier.align(Alignment.CenterStart))
            }
            if (layout == WindowLayout.WIDE && state.receipt != null) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.s7),
                ) {
                    ReceiptImage(state, Modifier.weight(1f).height(availableHeight * 0.55f)) { imageExpanded = true }
                    ScrollColumn(Modifier.weight(1f)) { FuelForm(state, onAction) }
                }
            } else {
                ScrollColumn(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(Modifier.widthIn(max = Size.sheetMaxWidth).fillMaxWidth()) {
                            if (state.receipt != null) {
                                ReceiptImage(state, Modifier.fillMaxWidth().height(availableHeight / 3)) {
                                    imageExpanded =
                                        true
                                }
                                Spacer(Modifier.height(Space.s5))
                            }
                            FuelForm(state, onAction)
                        }
                    }
                }
            }
            PrimaryButton(
                stringResource(R.string.fuel_add_confirm),
                onClick = { onAction(FuelEntryAction.Add) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canAdd,
                loading = state.saving,
            )
        }
        state.numberField?.let { field -> NumberEditor(field, state, onAction) }
        state.textField?.let { field -> TextEditor(field, state, onAction) }
        if (imageExpanded) {
            ModalCard(title = stringResource(R.string.fuel_receipt_image), onClose = { imageExpanded = false }) {
                ReceiptImage(state, Modifier.fillMaxWidth().height(availableHeight * 0.75f)) { imageExpanded = false }
            }
        }
    }
}

@Composable
private fun ReceiptImage(state: FuelEntryUiState, modifier: Modifier, onClick: () -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    Box(
        modifier.tappable(onClick = onClick).clip(RoundedCornerShape(Radius.lg)).background(c.surface2),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            model = state.receipt?.thumbUrl,
            contentDescription = stringResource(R.string.fuel_receipt_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(Modifier.fillMaxSize().padding(Space.s5), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.fuel_receipt_image),
                        style = t.body,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            error = {
                Box(Modifier.fillMaxSize().padding(Space.s5), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.fuel_receipt_image_unavailable),
                        style = t.body,
                        color = c.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
    }
}

@Composable
private fun FuelForm(state: FuelEntryUiState, onAction: (FuelEntryAction) -> Unit) {
    val c = AutoLogTheme.colors
    val t = AutoLogTheme.type
    val receipt = state.receipt
    Column(verticalArrangement = Arrangement.spacedBy(Space.s4)) {
        if (state.loading) {
            Text(stringResource(R.string.fuel_loading), style = t.body, color = c.textSecondary)
            return@Column
        }
        if (receipt != null) {
            if (receipt.ocrStatus == OcrStatus.FAILED) {
                Text(stringResource(R.string.fuel_ocr_failed), style = t.caption, color = c.warning)
            }
            ConfidenceRow(
                stringResource(R.string.fuel_merchant),
                state.merchant.ifBlank { null },
                state.confidence("merchant"),
                onClick = { onAction(FuelEntryAction.OpenText(FuelTextField.MERCHANT)) },
            )
            ConfidenceRow(
                stringResource(R.string.fuel_datetime),
                state.datetime.ifBlank { null }?.let(::displayDateTime),
                state.confidence("datetime"),
                onClick = { onAction(FuelEntryAction.OpenText(FuelTextField.DATETIME)) },
            )
            ConfidenceRow(
                stringResource(R.string.fuel_type),
                state.fuelType.ifBlank { null },
                state.confidence("fuelType"),
                onClick = { onAction(FuelEntryAction.OpenText(FuelTextField.FUEL_TYPE)) },
            )
        }
        NumberRow(stringResource(R.string.fuel_amount), state.amount, "원", state, FuelNumberField.AMOUNT, onAction)
        NumberRow(stringResource(R.string.fuel_liters), state.liters, "L", state, FuelNumberField.LITERS, onAction)
        NumberRow(
            stringResource(R.string.fuel_unit_price),
            state.unitPrice,
            "원/L",
            state,
            FuelNumberField.UNIT_PRICE,
            onAction,
        )
        if (receipt != null) {
            NumberRow(
                stringResource(R.string.fuel_discount),
                state.discount,
                "원",
                state,
                FuelNumberField.DISCOUNT,
                onAction,
            )
            ConfidenceRow(
                stringResource(R.string.fuel_card_last4),
                state.cardLast4.takeIf {
                    it.isNotBlank()
                }?.let { "•••• $it" },
                state.confidence("cardLast4"),
                onClick = { onAction(FuelEntryAction.OpenText(FuelTextField.CARD_LAST4)) },
            )
            state.arithOk?.let { okay ->
                Text(
                    stringResource(if (okay) R.string.fuel_arith_ok else R.string.fuel_arith_failed),
                    style = t.caption,
                    color = if (okay) c.success else c.danger,
                )
            }
        }
        Text(stringResource(R.string.fuel_link_trip), style = t.section, color = c.textSecondary)
        state.candidates.forEach { candidate ->
            ChoiceRow(
                candidate.label + if (candidate.hasFuelStop) stringResource(R.string.fuel_stop_badge) else "",
                selected = state.selectedTripId == candidate.tripId,
                onClick = { onAction(FuelEntryAction.SelectTrip(candidate.tripId)) },
            )
        }
        ChoiceRow(
            stringResource(R.string.fuel_no_trip),
            selected = state.selectedTripId == null,
            onClick = { onAction(FuelEntryAction.SelectTrip(null)) },
        )
        state.error?.let { Text(it, style = t.caption, color = c.danger) }
    }
}

@Composable
private fun NumberRow(
    label: String,
    raw: String,
    suffix: String,
    state: FuelEntryUiState,
    field: FuelNumberField,
    onAction: (FuelEntryAction) -> Unit,
) {
    val value = raw.takeIf { it.isNotBlank() }?.let {
        val number = if (field == FuelNumberField.LITERS) {
            it
        } else {
            it.toLongOrNull()?.let { n ->
                NumberFormat.getNumberInstance(Locale.KOREA).format(n)
            } ?: it
        }
        "$number $suffix"
    }
    if (state.receipt != null) {
        val key = when (field) {
            FuelNumberField.AMOUNT -> "amountWon"
            FuelNumberField.LITERS -> "liters"
            FuelNumberField.UNIT_PRICE -> "unitPrice"
            FuelNumberField.DISCOUNT -> "discountWon"
        }
        ConfidenceRow(label, value, state.confidence(key), onClick = { onAction(FuelEntryAction.OpenNumber(field)) })
    } else {
        val c = AutoLogTheme.colors
        val t = AutoLogTheme.type
        Row(
            Modifier
                .fillMaxWidth()
                .tappable(onClick = { onAction(FuelEntryAction.OpenNumber(field)) })
                .clip(RoundedCornerShape(Radius.md))
                .background(c.surface1)
                .padding(Space.s5),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = t.caption, color = c.textSecondary)
            Text(value ?: "—", style = t.body, color = c.textPrimary)
        }
    }
}

@Composable
private fun NumberEditor(field: FuelNumberField, state: FuelEntryUiState, onAction: (FuelEntryAction) -> Unit) {
    val value = when (field) {
        FuelNumberField.AMOUNT -> state.amount
        FuelNumberField.LITERS -> state.liters
        FuelNumberField.UNIT_PRICE -> state.unitPrice
        FuelNumberField.DISCOUNT -> state.discount
    }
    ModalCard(
        title = stringResource(numberLabel(field)),
        onClose = { onAction(FuelEntryAction.CloseEditor) },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value.ifBlank { "0" },
                style = AutoLogTheme.type.headline,
                color = AutoLogTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(Space.s4))
            NumericKeypad(
                onDigit = { onAction(FuelEntryAction.Digit(it)) },
                onDelete = { onAction(FuelEntryAction.Delete) },
                allowDecimal = field == FuelNumberField.LITERS,
            )
        }
    }
}

@Composable
private fun TextEditor(field: FuelTextField, state: FuelEntryUiState, onAction: (FuelEntryAction) -> Unit) {
    val value = when (field) {
        FuelTextField.MERCHANT -> state.merchant
        FuelTextField.DATETIME -> state.datetime
        FuelTextField.FUEL_TYPE -> state.fuelType
        FuelTextField.CARD_LAST4 -> state.cardLast4
    }
    ModalCard(
        title = stringResource(textLabel(field)),
        onClose = { onAction(FuelEntryAction.CloseEditor) },
        footer = {
            SecondaryButton(
                stringResource(R.string.action_done),
                onClick = { onAction(FuelEntryAction.CloseEditor) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        InputField(stringResource(textLabel(field)), value, { onAction(FuelEntryAction.TextChanged(it)) })
    }
}

private fun numberLabel(field: FuelNumberField): Int = when (field) {
    FuelNumberField.AMOUNT -> R.string.fuel_amount
    FuelNumberField.LITERS -> R.string.fuel_liters
    FuelNumberField.UNIT_PRICE -> R.string.fuel_unit_price
    FuelNumberField.DISCOUNT -> R.string.fuel_discount
}

private fun textLabel(field: FuelTextField): Int = when (field) {
    FuelTextField.MERCHANT -> R.string.fuel_merchant
    FuelTextField.DATETIME -> R.string.fuel_datetime
    FuelTextField.FUEL_TYPE -> R.string.fuel_type
    FuelTextField.CARD_LAST4 -> R.string.fuel_card_last4
}

private fun displayDateTime(raw: String): String =
    runCatching { OffsetDateTime.parse(raw).format(DateTimeFormatter.ofPattern("M/d HH:mm", Locale.KOREA)) }
        .getOrDefault(raw)
