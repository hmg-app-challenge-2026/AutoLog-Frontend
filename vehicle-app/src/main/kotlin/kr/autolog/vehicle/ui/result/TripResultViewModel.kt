package kr.autolog.vehicle.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.common.tickerFlow
import kr.autolog.vehicle.data.repository.SessionRepository
import kr.autolog.vehicle.data.repository.TripResult
import kr.autolog.vehicle.data.repository.TripResultRepository
import kr.autolog.vehicle.nav.Navigator
import kr.autolog.vehicle.nav.Route
import kr.autolog.vehicle.nav.TripResultMode
import kr.autolog.vehicle.ui.design.remainingSeconds

/**
 * 운행 결과 카드(스펙 7.3, D-27·D-28). 두 단계를 한 카드가 이어서 맡는다:
 * 1. **종료 대기**(종료 조건은 됐지만 앱 A가 아직 끝내기 전): 남은 시간 = 앱 A 종료 시각. [계속 기록하기]로 되돌릴 수 있다.
 *    시간이 다 되면(또는 [메인 화면으로]) 운행이 끝나고 → 지금 선택으로 저장 → 들어온 화면으로.
 * 2. **끝난 운행**(직접 [기록 종료]·시동 끔·알림·이력에서 연 경우): 자체 5초 카운트다운 → 저장 → 돌아간다.
 * [수정하기] 중엔 돌아가지 않는다(운행 자체는 앱 A 시각에 끝나고, 고른 목적은 그때·[저장] 때 저장).
 * 저장하지 않고 화면이 닫히면(주행 시작 등) 그 운행은 "목적 미입력"으로 남는다.
 *
 * [mode] = REVIEW(운행 기록 조회에서 연 사후 확인, D-29): 카운트다운도 자동 저장도 없다. 목적 미입력 운행은 수정 모드로 열리고,
 * [저장]을 눌러야만 저장한 뒤 목록으로 돌아간다. [닫기]는 저장하지 않고 돌아간다.
 */
class TripResultViewModel(
    private val tripId: String,
    private val results: TripResultRepository,
    private val session: SessionRepository,
    private val navigator: Navigator,
    private val clock: AppClock,
    private val mode: TripResultMode = TripResultMode.AFTER_TRIP,
    ticker: Flow<Long> = tickerFlow(clock),
) : ViewModel() {
    private val review = mode == TripResultMode.REVIEW
    private val interaction = MutableStateFlow(TripResultInteraction())
    private val loaded = MutableStateFlow<Loaded>(Loaded.NotYet)
    private var current: TripResult? = null
    private var countdown: Job? = null
    private var finishing = false
    private var left = false

    val uiState: StateFlow<TripResultUiState> =
        combine(loaded, interaction, ticker) { l, i, now ->
            reduceTripResult((l as? Loaded.Value)?.result, l is Loaded.Value, i, now, mode)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripResultUiState())

    init {
        viewModelScope.launch {
            results.result(tripId).collect { r ->
                val first = loaded.value is Loaded.NotYet
                val wasEnding = current?.endsAt != null
                current = r
                loaded.value = Loaded.Value(r)
                when {
                    // 사후 확인 — 시계 없음. 목적을 아직 안 정한 운행은 바로 고를 수 있게 수정 모드로
                    first && review -> if (r != null && !r.classified) interaction.update { it.copy(editing = true) }
                    // 종료 대기로 열렸다 — 막대 전체 길이 = 지금부터 종료 시각까지
                    first && r?.endsAt != null ->
                        interaction.update { it.copy(autoCloseTotalSec = remainingSeconds(r.endsAt, clock.nowMs())) }
                    // 이미 끝난 운행으로 열렸다 — 자체 카운트다운
                    first && r != null && r.endsAt == null -> startCountdown()
                    // 종료 대기 → 끝남(시간 만료·[메인 화면으로]) 또는 사라짐(다시 출발해 앱 A가 종료를 취소)
                    wasEnding && r?.endsAt == null -> onEnded(r)
                }
            }
        }
    }

    fun onAction(action: TripResultAction) {
        when (action) {
            TripResultAction.Continue -> keepRecording()
            is TripResultAction.Select ->
                interaction.update { if (it.editing) it.copy(selected = action.category) else it }
            TripResultAction.Edit -> {
                countdown?.cancel()
                interaction.update { it.copy(editing = true, autoCloseSec = null) }
            }
            TripResultAction.Save -> save()
            // 사후 확인의 [닫기]는 저장하지 않고 돌아간다
            TripResultAction.GoHome -> if (review) leave() else finish()
        }
    }

    private fun keepRecording() {
        if (current?.endsAt == null || finishing) return
        viewModelScope.launch {
            session.keepRecording()
            leave()
        }
    }

    private fun onEnded(result: TripResult?) {
        if (result == null) {
            leave()
            return
        }
        viewModelScope.launch {
            results.classify(tripId, selectedNow())
            if (finishing || !interaction.value.editing) leave()
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown =
            viewModelScope.launch {
                interaction.update { it.copy(autoCloseTotalSec = AUTO_CLOSE_SEC) }
                for (sec in AUTO_CLOSE_SEC downTo 1) {
                    interaction.update { it.copy(autoCloseSec = sec) }
                    delay(1_000)
                }
                finish()
            }
    }

    private fun save() {
        val now = interaction.value
        if (!now.editing || now.saving) return
        if (current?.endsAt != null) {
            // 아직 끝나기 전 — 고른 목적은 운행이 끝날 때 저장된다(onEnded). 카운트다운은 앱 A 시각을 다시 따른다
            interaction.update { it.copy(editing = false) }
            return
        }
        interaction.update { it.copy(saving = true) }
        viewModelScope.launch {
            results.classify(tripId, selectedNow())
            if (review) {
                leave() // 사후 확인: 저장했으면 목록으로
            } else {
                interaction.update { it.copy(editing = false, saving = false) }
                startCountdown()
            }
        }
    }

    /** 지금 선택으로 저장하고 들어온 화면으로. 종료 대기 중이면 먼저 바로 끝낸다(저장은 onEnded가). */
    private fun finish() {
        if (finishing) return
        finishing = true
        countdown?.cancel()
        interaction.update { it.copy(saving = true, autoCloseSec = null) }
        viewModelScope.launch {
            if (current?.endsAt != null) {
                session.endNow()
            } else {
                results.classify(tripId, selectedNow())
                leave()
            }
        }
    }

    /** 들어온 화면으로(뒤가 없으면 Home). 한 번만. */
    private fun leave() {
        if (left) return
        left = true
        if (!navigator.pop()) navigator.replaceAll(Route.Home)
    }

    /** 지금 선택: 고른 값, 없으면 저장된 분류·기본값. */
    private fun selectedNow() = interaction.value.selected ?: current?.category ?: uiState.value.selected

    private sealed interface Loaded {
        data object NotYet : Loaded

        data class Value(val result: TripResult?) : Loaded
    }
}
