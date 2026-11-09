package kr.autolog.vehicle.ui.review

import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.core.PurposeDetail

/** 상단 요약(스펙 7.3): `09:00 → 09:35 · 23.4 km` + 출발 → 도착. [end]·[km] null = 미확정. */
data class ReviewSummaryUi(
    val start: String,
    val end: String?,
    val km: String?,
    val origin: String?,
    val destination: String?,
)

/** 선택지 한 묶음 — 일반 업무용 · 출퇴근용 · 비업무용. */
data class PurposeGroupUi(val category: PurposeCategory, val options: List<PurposeDetail>)

/** 말로 입력(스펙 7.3 음성). 듣는 중에는 파형 대신 정적 문구. */
sealed interface VoiceUi {
    data object Idle : VoiceUi

    data object Listening : VoiceUi

    /** 알아들었다 — 원문을 보여 주고 [detail]을 고른 상태로 [저장]을 기다린다. */
    data class Heard(val text: String, val detail: PurposeDetail, val note: String?) : VoiceUi

    /** 말은 받았지만 목적을 못 뽑았다 — 원문 + 다시 말하기 + 탭 선택. */
    data class NotUnderstood(val text: String) : VoiceUi

    /** 실패. [reason] null = 8초 동안 말이 없었다. */
    data class Failed(val reason: String?) : VoiceUi
}

/** 확정 뒤 "이곳을 거래처로 등록할까요?"(F-17). [label] 프리셋 = 도착지 이름. */
data class PlaceProposalUi(val label: String, val saving: Boolean = false, val error: String? = null) {
    val canRegister: Boolean get() = label.isNotBlank() && !saving
}

/** 사용자 조작으로 바뀌는 값 — ViewModel이 들고 있고 [reduceReview]가 운행 정보와 합친다. */
data class ReviewInteraction(
    val showOptions: Boolean = false,
    val voice: VoiceUi = VoiceUi.Idle,
    val chosenDestination: String? = null,
    val saving: Boolean = false,
    val place: PlaceProposalUi? = null,
)

data class ReviewUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val summary: ReviewSummaryUi? = null,
    /** 확신(≥ 0.8) 추천만. null이면 추천 없이 선택지를 먼저 보여 준다(확신이 낮으면 반드시 묻는다). */
    val suggestion: PurposeDetail? = null,
    /** 이미 확정된 목적 — 운행 기록 조회에서 고치러 들어왔을 때. 선택지에 체크로 보이고 추천 버튼은 없다. */
    val currentPurpose: PurposeDetail? = null,
    val showOptions: Boolean = true,
    val groups: List<PurposeGroupUi> = PURPOSE_GROUPS,
    /** 도착지를 모를 때만: 최근 목적지(최대 3). */
    val destinationCandidates: List<String> = emptyList(),
    val chosenDestination: String? = null,
    val voiceAvailable: Boolean = false,
    val voice: VoiceUi = VoiceUi.Idle,
    val saving: Boolean = false,
    val place: PlaceProposalUi? = null,
)

sealed interface ReviewAction {
    /** 추천 버튼·선택지 1탭 = 바로 확정(조작 예산 1탭, 스펙 1.2). */
    data class Confirm(val detail: PurposeDetail) : ReviewAction

    data object ShowOptions : ReviewAction

    data object Later : ReviewAction

    data object Listen : ReviewAction

    /** 말로 고른 목적을 확정(원문은 메모로). */
    data object SaveVoice : ReviewAction

    data class ChooseDestination(val label: String) : ReviewAction

    data class PlaceLabelChanged(val value: String) : ReviewAction

    data object RegisterPlace : ReviewAction

    data object SkipPlace : ReviewAction
}

/** 목적 선택지 3묶음(스펙 7.3). "확인 필요"는 고를 수 있는 목적이 아니라 빠진다. */
val PURPOSE_GROUPS: List<PurposeGroupUi> =
    listOf(PurposeCategory.BUSINESS, PurposeCategory.COMMUTE, PurposeCategory.NON_BUSINESS).map { category ->
        PurposeGroupUi(category, PurposeDetail.entries.filter { it.category == category })
    }
