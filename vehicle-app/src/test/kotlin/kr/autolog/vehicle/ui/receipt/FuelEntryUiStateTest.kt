package kr.autolog.vehicle.ui.receipt

import kr.autolog.vehicle.fake.FakeReceiptApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEntryUiStateTest {
    @Test
    fun `직접 입력은 금액이 있어야 추가할 수 있다`() {
        assertFalse(FuelEntryUiState().canAdd)
        assertTrue(FuelEntryUiState(amount = "68000").canAdd)
    }

    @Test
    fun `OCR 검산 실패는 수정 전 추가를 막고 값을 고치면 허용한다`() {
        val receipt = FakeReceiptApi.SAMPLE_UNSURE
        val original = FuelEntryUiState(receipt = receipt, amount = "68000", liters = "4.0", unitPrice = "1700")
        val corrected = original.copy(liters = "40.0")

        assertFalse(original.canAdd)
        assertTrue(corrected.canAdd)
    }

    @Test
    fun `OCR 실패는 금액만 입력해도 추가할 수 있다`() {
        val receipt = FakeReceiptApi.SAMPLE_FAILED
        val state = FuelEntryUiState(receipt = receipt, amount = "50000")

        assertNull(state.arithOk)
        assertTrue(state.canAdd)
    }
}
