package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kr.autolog.vehicle.core.PurposeCategory
import kr.autolog.vehicle.core.SettingsApi
import kr.autolog.vehicle.data.interim.LocalStore
import kr.autolog.vehicle.data.interim.UploadState
import java.time.YearMonth

/** 이력·월 요약·확인 필요 목록(스펙 7.2·7.7·7.8). */
interface TripRepository {
    /** 목적 확인이 필요한 운행 id, 최신 순. */
    val pendingReviewIds: Flow<List<String>>

    fun history(month: YearMonth): Flow<List<HistoryItem>>

    fun summary(month: YearMonth): Flow<MonthSummary>
}

class DefaultTripRepository(private val store: LocalStore, private val settings: SettingsApi) : TripRepository {
    override val pendingReviewIds: Flow<List<String>> = store.pendingReviews.map { rows -> rows.map { it.tripId } }

    override fun history(month: YearMonth): Flow<List<HistoryItem>> =
        combine(store.trips(month), settings.users()) { rows, users ->
            val names = users.associate { it.userId to it.name }
            rows.map { r ->
                HistoryItem(
                    tripId = r.tripId,
                    startTime = r.startTime,
                    endTime = r.endTime,
                    driverName = r.driverUserId?.let { names[it] },
                    driverUnset = r.driverUserId == null,
                    origin = r.originLabel,
                    destination = r.destinationLabel,
                    distanceKm = r.distanceKm,
                    purpose = r.purpose,
                    upload =
                    when (r.upload) {
                        UploadState.PENDING -> UploadMark.PENDING
                        UploadState.DONE -> UploadMark.DONE
                        UploadState.FAILED -> UploadMark.FAILED
                        null -> null
                    },
                )
            }
        }

    override fun summary(month: YearMonth): Flow<MonthSummary> = store.trips(month).map { rows ->
        MonthSummary(
            tripCount = rows.size,
            totalKm = rows.mapNotNull { it.distanceKm }.sum(),
            businessKm = rows.filter {
                it.purpose?.category == PurposeCategory.BUSINESS
            }.mapNotNull { it.distanceKm }.sum(),
            reviewCount = rows.count { it.purpose == null || it.driverUserId == null || it.distanceKm == null },
        )
    }
}
