package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kr.autolog.vehicle.core.ContextApi
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.core.PurposeEngine
import kr.autolog.vehicle.core.SettingsResult
import kr.autolog.vehicle.core.TripFacts
import kr.autolog.vehicle.core.VoiceApi
import kr.autolog.vehicle.core.VoicePurpose
import kr.autolog.vehicle.core.VoiceResult
import kr.autolog.vehicle.data.interim.InputMode
import kr.autolog.vehicle.data.interim.LocalStore
import kr.autolog.vehicle.data.interim.TripRow
import kr.autolog.vehicle.data.interim.TripSessionApi

/** 목적 확인(스펙 7.3, F-06·08·09·17). */
interface ReviewRepository {
    fun review(tripId: String): Flow<TripReview?>

    /** [destination]: 도착지를 모르던 운행에 사용자가 고른 도착지(없으면 null — 그대로 둔다). */
    suspend fun confirm(
        tripId: String,
        detail: PurposeDetail,
        note: String?,
        byVoice: Boolean,
        destination: String? = null,
    )

    /** 목적지가 없을 때 고를 후보(내비 최근 목적지, 최대 3). */
    suspend fun destinationCandidates(): List<String>

    suspend fun shouldProposePlace(tripId: String, detail: PurposeDetail): Boolean

    suspend fun registerPlace(tripId: String, label: String): Outcome

    val voiceAvailable: StateFlow<Boolean>

    suspend fun listenPurpose(): VoiceResult<VoicePurpose>
}

class DefaultReviewRepository(
    private val store: LocalStore,
    private val session: TripSessionApi,
    private val purpose: PurposeEngine,
    private val context: ContextApi,
    private val voice: VoiceApi,
    private val vehicleId: () -> String,
) : ReviewRepository {
    override fun review(tripId: String): Flow<TripReview?> = flow {
        val row = store.trip(tripId)
        if (row == null) {
            emit(null)
            return@flow
        }
        val suggestion = purpose.suggest(row.facts())
        emit(
            TripReview(
                row.tripId,
                row.startTime,
                row.endTime,
                row.distanceKm,
                row.originLabel,
                row.destinationLabel,
                suggestion,
            ),
        )
    }

    override suspend fun confirm(
        tripId: String,
        detail: PurposeDetail,
        note: String?,
        byVoice: Boolean,
        destination: String?,
    ) = session.confirmPurpose(tripId, detail, note, if (byVoice) InputMode.VOICE else InputMode.TAP, destination)

    override suspend fun destinationCandidates(): List<String> = context.recentDestinations().map {
        it.label
    }.distinct().take(3)

    override suspend fun shouldProposePlace(tripId: String, detail: PurposeDetail): Boolean {
        val row = store.trip(tripId) ?: return false
        return purpose.shouldProposePlace(row.facts(), detail)
    }

    override suspend fun registerPlace(tripId: String, label: String): Outcome {
        val row = store.trip(tripId) ?: return Outcome.Failed("운행을 찾지 못했어요")
        return when (val r = purpose.registerClientPlace(row.facts(), label)) {
            SettingsResult.Ok -> Outcome.Ok
            is SettingsResult.Invalid -> Outcome.Failed(r.reason)
        }
    }

    override val voiceAvailable: StateFlow<Boolean> = voice.available

    override suspend fun listenPurpose(): VoiceResult<VoicePurpose> = voice.listenPurpose()

    private fun TripRow.facts() = TripFacts(
        tripId = tripId,
        vehicleId = vehicleId(),
        startTime = startTime,
        endTime = endTime,
        destinationLabel = destinationLabel,
        driverUserId = driverUserId,
    )
}
