package kr.autolog.vehicle.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeSettingsApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRepositoryTest {
    private val driverId = MutableStateFlow<String?>(null)
    private val repo =
        DefaultProfileRepository(FakePurposeEngine(), FakeSettingsApi(), driverId, { "veh-1" }, AppClock { 0L })

    @Test
    fun `추천 엔진의 순서와 배지를 그대로 옮긴다`() =
        runTest {
            val profiles = repo.orderedProfiles().first()

            assertEquals(listOf("u-1", "u-2", "u-3"), profiles.map { it.userId })
            assertEquals(ProfileBadge.SCHEDULED, profiles[0].badge)
            assertEquals(ProfileBadge.RECENT, profiles[1].badge)
            assertNull(profiles[2].badge)
        }

    @Test
    fun `현재 운전자는 직원 목록에서 이름을 찾는다`() =
        runTest {
            repo.currentDriver().test {
                assertNull(awaitItem())
                driverId.value = "u-2"
                assertEquals("이현장", awaitItem()?.name)
            }
        }

    @Test
    fun `운전자 미확정 운행은 후보를 먼저 두고 최대 3명`() =
        runTest {
            val item = repo.unassignedTrip()!!

            assertEquals("t-9", item.tripId)
            assertEquals("u-1", item.candidates.first().userId)
            assertEquals(3, item.candidates.size)
        }
}
