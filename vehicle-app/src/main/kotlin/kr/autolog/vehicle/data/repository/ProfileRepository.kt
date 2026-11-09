package kr.autolog.vehicle.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.PurposeEngine
import kr.autolog.vehicle.core.SettingsApi
import kr.autolog.vehicle.core.UserBadge

enum class ProfileBadge { SCHEDULED, RECENT }

data class DriverProfile(val userId: String, val name: String, val dept: String?, val badge: ProfileBadge? = null)

/** 운전자 미확정 운행 1건과 1탭으로 고를 후보(최대 3명, 배차·직전 후보가 먼저). */
data class UnassignedTripItem(val tripId: String, val label: String, val candidates: List<DriverProfile>)

interface ProfileRepository {
    fun orderedProfiles(): Flow<List<DriverProfile>>

    fun currentDriver(): Flow<DriverProfile?>

    suspend fun unassignedTrip(): UnassignedTripItem?
}

class DefaultProfileRepository(
    private val purpose: PurposeEngine,
    private val settings: SettingsApi,
    private val currentDriverId: StateFlow<String?>,
    private val vehicleId: () -> String,
    private val clock: AppClock,
) : ProfileRepository {
    override fun orderedProfiles(): Flow<List<DriverProfile>> =
        purpose.orderedUsers(vehicleId(), clock.nowMs()).map { cards ->
            cards.map { card ->
                DriverProfile(
                    userId = card.userId,
                    name = card.name,
                    dept = card.dept,
                    badge =
                        when (card.badge) {
                            UserBadge.SCHEDULED -> ProfileBadge.SCHEDULED
                            UserBadge.RECENT -> ProfileBadge.RECENT
                            null -> null
                        },
                )
            }
        }

    override fun currentDriver(): Flow<DriverProfile?> =
        combine(currentDriverId, settings.users()) { id, users ->
            users.firstOrNull { it.userId == id }?.let { DriverProfile(it.userId, it.name, it.dept) }
        }.distinctUntilChanged()

    override suspend fun unassignedTrip(): UnassignedTripItem? {
        val trip = purpose.unassignedTrips(vehicleId(), clock.nowMs()).firstOrNull() ?: return null
        val others = orderedProfiles().first()
        val first = trip.candidate?.let { DriverProfile(it.userId, it.name, null, ProfileBadge.SCHEDULED) }
        val candidates = (listOfNotNull(first) + others.filter { it.userId != first?.userId }).take(3)
        return UnassignedTripItem(trip.tripId, trip.label, candidates)
    }
}
