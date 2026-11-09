package kr.autolog.vehicle.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.autolog.vehicle.data.repository.TripResult
import kr.autolog.vehicle.data.repository.TripResultRepository
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route

/**
 * 운행 결과 카드(스펙 7.3, D-27). 열리면 5초 카운트다운 → 지금 선택(기본 일반 업무)으로 저장하고 들어온 화면으로 돌아간다.
 * 화면을 그냥 떠나면(주행 시작 등) 저장하지 않는다 — 그 운행은 "목적 미입력"으로 남아 운행 기록 조회에서 고친다.
 */
class TripResultViewModel(
    private val tripId: String,
    private val results: TripResultRepository,
    private val navigator: Navigator,
) : ViewModel() {
    private val interaction = MutableStateFlow(TripResultInteraction())
    private val loaded = MutableStateFlow<Loaded>(Loaded.NotYet)
    private var countdown: Job? = null

    val uiState: StateFlow<TripResultUiState> =
        combine(loaded, interaction) { l, i ->
            reduceTripResult((l as? Loaded.Value)?.result, l is Loaded.Value, i)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripResultUiState())

    init {
        viewModelScope.launch {
            val result = results.result(tripId).first()
            loaded.value = Loaded.Value(result)
            if (result != null) startCountdown()
        }
    }

    fun onAction(action: TripResultAction) {
        when (action) {
            is TripResultAction.Select ->
                interaction.update { if (it.editing) it.copy(selected = action.category) else it }
            TripResultAction.Edit -> {
                countdown?.cancel()
                interaction.update { it.copy(editing = true, autoCloseSec = null) }
            }
            TripResultAction.Save -> save()
            TripResultAction.GoHome -> finish()
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown =
            viewModelScope.launch {
                for (sec in AUTO_CLOSE_SEC downTo 1) {
                    interaction.update { it.copy(autoCloseSec = sec) }
                    delay(1_000)
                }
                interaction.update { it.copy(autoCloseSec = 0) }
                finish()
            }
    }

    private fun save() {
        if (!interaction.value.editing || interaction.value.saving) return
        interaction.update { it.copy(saving = true) }
        viewModelScope.launch {
            results.classify(tripId, selectedNow())
            interaction.update { it.copy(editing = false, saving = false) }
            startCountdown()
        }
    }

    /** 지금 선택으로 저장하고 들어온 화면으로(뒤가 없으면 Home). */
    private fun finish() {
        if (interaction.value.saving) return
        countdown?.cancel()
        interaction.update { it.copy(saving = true, autoCloseSec = null) }
        viewModelScope.launch {
            results.classify(tripId, selectedNow())
            if (!navigator.pop()) navigator.replaceAll(Route.Home)
        }
    }

    /** 화면 구독 여부와 상관없이 지금 선택: 고른 값, 없으면 저장된 분류·기본값. */
    private fun selectedNow() =
        interaction.value.selected ?: (loaded.value as? Loaded.Value)?.result?.category ?: uiState.value.selected

    private sealed interface Loaded {
        data object NotYet : Loaded

        data class Value(val result: TripResult?) : Loaded
    }
}
