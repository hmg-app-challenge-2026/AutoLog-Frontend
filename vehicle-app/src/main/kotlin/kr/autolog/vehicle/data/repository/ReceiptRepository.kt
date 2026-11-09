package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kr.autolog.shared.ReceiptFields
import kr.autolog.shared.ReceiptPayload
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.ConfirmResult
import kr.autolog.vehicle.core.ExpenseSource
import kr.autolog.vehicle.core.QrResult
import kr.autolog.vehicle.core.ReceiptApi
import kr.autolog.vehicle.core.TripCandidate
import kr.autolog.vehicle.core.VoiceApi
import kr.autolog.vehicle.core.VoiceFuel
import kr.autolog.vehicle.core.VoiceResult
import kr.autolog.vehicle.data.interim.LocalStore

/** 영수증·주유(스펙 7.4~7.5, D-20, F-18·19·30). */
interface ReceiptRepository {
    suspend fun issueQr(): QrResult

    val inbox: Flow<List<ReceiptPayload>>

    fun receipt(receiptId: String): Flow<ReceiptPayload?>

    suspend fun candidates(receipt: ReceiptPayload): List<TripCandidate>

    suspend fun confirm(receipt: ReceiptPayload, tripId: String?, edits: ReceiptFields): ConfirmResult

    suspend fun saveFuel(tripId: String?, fuel: VoiceFuel, byVoice: Boolean): ConfirmResult

    val voiceAvailable: StateFlow<Boolean>

    suspend fun listenFuel(): VoiceResult<VoiceFuel>
}

/**
 * [ack]: 확정한 영수증을 inbox에서 지운다. 실제 ReceiptApi(앱 B)는 confirm 안에서 ack하지만,
 * interim 저장소를 쓰는 동안에는 여기서 직접 지운다.
 */
class DefaultReceiptRepository(
    private val api: ReceiptApi,
    private val store: LocalStore,
    private val voice: VoiceApi,
    private val clock: AppClock,
    private val ack: (String) -> Unit,
) : ReceiptRepository {
    override suspend fun issueQr(): QrResult = api.issueQr()

    override val inbox: Flow<List<ReceiptPayload>> = store.receiptInbox

    override fun receipt(receiptId: String): Flow<ReceiptPayload?> =
        store.receiptInbox.map { list -> list.firstOrNull { it.receiptId == receiptId } }

    override suspend fun candidates(receipt: ReceiptPayload): List<TripCandidate> = api.candidates(receipt).take(3)

    override suspend fun confirm(receipt: ReceiptPayload, tripId: String?, edits: ReceiptFields): ConfirmResult {
        val result = api.confirm(receipt, tripId, edits)
        if (result is ConfirmResult.Saved) ack(receipt.receiptId)
        return result
    }

    override suspend fun saveFuel(tripId: String?, fuel: VoiceFuel, byVoice: Boolean): ConfirmResult =
        api.saveFuel(tripId, fuel, if (byVoice) ExpenseSource.VOICE else ExpenseSource.TAP, clock.nowMs())

    override val voiceAvailable: StateFlow<Boolean> = voice.available

    override suspend fun listenFuel(): VoiceResult<VoiceFuel> = voice.listenFuel()
}
