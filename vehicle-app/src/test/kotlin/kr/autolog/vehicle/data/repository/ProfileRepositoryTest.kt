package kr.autolog.vehicle.data.repository

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.fake.FakePurposeEngine
import kr.autolog.vehicle.fake.FakeSettingsApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileRepositoryTest {
    private val driverId = MutableStateFlow<String?>(null)
    private val repo =
        DefaultProfileRepository(FakePurposeEngine(), FakeSettingsApi(), driverId, { "veh-1" }, AppClock { 0L })

    @Test
    fun `추천 엔진의 순서와 배지를 그대로 옮긴다`() = runTest {
        val profiles = repo.orderedProfiles().first()

        assertEquals(listOf("u-1", "u-2", "u-3"), profiles.map { it.userId })
        assertEquals(ProfileBadge.SCHEDULED, profiles[0].badge)
        assertEquals(ProfileBadge.RECENT, profiles[1].badge)
        assertNull(profiles[2].badge)
    }

    @Test
    fun `현재 운전자는 직원 목록에서 이름을 찾는다`() = runTest {
        repo.currentDriver().test {
            assertNull(awaitItem())
            driverId.value = "u-2"
            assertEquals("이현장", awaitItem()?.name)
        }
    }

    @Test
    fun `새 운전자를 추가하면 id를 돌려주고 목록 끝에 보인다`() = runTest {
        val added = repo.addDriver("  최신입 ", "영업2팀") as AddDriverResult.Added

        val profiles = repo.orderedProfiles().first()

        assertEquals(added.userId, profiles.last().userId)
        assertEquals("최신입", profiles.last().name)
    }

    @Test
    fun `동명이인을 추가해도 새로 생긴 사람을 돌려준다`() = runTest {
        val first = repo.addDriver("김영업", null) as AddDriverResult.Added
        val second = repo.addDriver("김영업", null) as AddDriverResult.Added

        assertNotEquals(first.userId, second.userId)
    }

    @Test
    fun `이름이 비면 추가하지 않는다`() = runTest {
        assertEquals(AddDriverResult.EmptyName, repo.addDriver(" ", null))
    }
}
