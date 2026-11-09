package kr.autolog.vehicle.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kr.autolog.vehicle.core.PurposeDetail
import kr.autolog.vehicle.data.repository.Outcome
import kr.autolog.vehicle.data.repository.ReviewRepository
import kr.autolog.vehicle.data.repository.TripReview
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

/** 듣기 최대 시간(스펙 7.3 "최대 8초"). */
const val VOICE_TIMEOUT_MS = 8_000L

/**
 * 목적 확인(스펙 7.3). 확정하면 이전 화면(Home 또는 운행 기록 조회)으로 돌아간다 —
 * 단, 엔진이 거래처 등록을 제안하면 그 팝업을 먼저 띄운다(F-17).
 */
class ReviewViewModel(
    private val tripId: String,
    private val review: ReviewRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val interaction = MutableStateFlow(ReviewInteraction())
    private var listening: Job? = null

    /** 운행 정보는 한 번 읽는다. 첫 값 전에는 "불러오는 중"(null 대신 [Loaded.NotYet]). */
    private val trip =
        review.review(tripId).map<TripReview?, Loaded> { Loaded.Value(it) }.onStart { emit(Loaded.NotYet) }

    private val candidates = flow { emit(review.destinationCandidates()) }.onStart { emit(emptyList()) }

    val uiState: StateFlow<ReviewUiState> =
        combine(trip, candidates, review.voiceAvailable, interaction) { t, c, voice, i ->
            reduceReview((t as? Loaded.Value)?.review, t is Loaded.Value, c, voice, i)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    fun onAction(action: ReviewAction) {
        when (action) {
            is ReviewAction.Confirm -> confirm(action.detail, note = null, byVoice = false)
            ReviewAction.SaveVoice -> (interaction.value.voice as? VoiceUi.Heard)?.let {
                confirm(it.detail, it.note, byVoice = true)
            }
            ReviewAction.ShowOptions -> interaction.update { it.copy(showOptions = true) }
            ReviewAction.Later -> leave()
            ReviewAction.Listen -> listen()
            is ReviewAction.ChooseDestination -> interaction.update {
                // 같은 칩을 다시 누르면 고른 것을 푼다
                it.copy(chosenDestination = action.label.takeIf { label -> label != it.chosenDestination })
            }
            is ReviewAction.PlaceLabelChanged -> interaction.update {
                it.copy(place = it.place?.copy(label = action.value, error = null))
            }
            ReviewAction.RegisterPlace -> registerPlace()
            ReviewAction.SkipPlace -> leave()
        }
    }

    private fun confirm(detail: PurposeDetail, note: String?, byVoice: Boolean) {
        val now = interaction.value
        if (now.saving) return
        listening?.cancel()
        interaction.update { it.copy(saving = true) }
        viewModelScope.launch {
            review.confirm(tripId, detail, note, byVoice, now.chosenDestination)
            if (review.shouldProposePlace(tripId, detail)) {
                // 이름 프리셋 = 고른 도착지, 없으면 운행의 도착지(화면 구독 여부와 상관없이 저장소에서 읽는다)
                val label = now.chosenDestination ?: review.review(tripId).first()?.destination.orEmpty()
                interaction.update { it.copy(saving = false, place = PlaceProposalUi(label)) }
            } else {
                leave()
            }
        }
    }

    private fun listen() {
        listening?.cancel()
        interaction.update { it.copy(voice = VoiceUi.Listening) }
        listening =
            viewModelScope.launch {
                val result = withTimeoutOrNull(VOICE_TIMEOUT_MS) { review.listenPurpose() }
                interaction.update { it.copy(voice = result.toVoiceUi()) }
            }
    }

    private fun registerPlace() {
        val place = interaction.value.place?.takeIf { it.canRegister } ?: return
        interaction.update { it.copy(place = place.copy(saving = true, error = null)) }
        viewModelScope.launch {
            when (val r = review.registerPlace(tripId, place.label.trim())) {
                Outcome.Ok -> leave()
                is Outcome.Failed -> interaction.update {
                    it.copy(place = place.copy(saving = false, error = r.reason))
                }
            }
        }
    }

    /** 들어온 화면으로 돌아간다. 딥링크로 바로 열려 뒤가 없으면 Home. */
    private fun leave() {
        if (!navigator.pop()) navigator.replaceAll(Route.Home)
    }

    private sealed interface Loaded {
        data object NotYet : Loaded

        data class Value(val review: TripReview?) : Loaded
    }
}
