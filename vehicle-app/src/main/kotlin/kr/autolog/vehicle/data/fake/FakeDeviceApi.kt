package kr.autolog.vehicle.data.fake

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kr.autolog.vehicle.common.AppClock
import kr.autolog.vehicle.core.ConnectionState
import kr.autolog.vehicle.core.DeviceApi
import kr.autolog.vehicle.core.RegisterResult

/** 앱 A DeviceApi 가짜. [registered]면 사내 서버에 연결된 상태로 시작한다. */
class FakeDeviceApi(private val clock: AppClock, registered: Boolean = true) : DeviceApi {
    private val _connection =
        MutableStateFlow<ConnectionState>(
            if (registered) {
                ConnectionState.Registered(VEHICLE, "https://autolog.acme.co.kr", clock.nowMs() - 600_000)
            } else {
                ConnectionState.NotRegistered
            },
        )
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    override suspend fun register(serverUrl: String, code: String): RegisterResult {
        if (!serverUrl.startsWith("https://") && !serverUrl.startsWith("http://")) {
            return RegisterResult.Invalid("서버 주소는 https:// 로 시작해야 해요")
        }
        if (!CODE.matches(code)) return RegisterResult.Invalid("연결 코드는 숫자 6자리예요")
        _connection.value = ConnectionState.Registered(VEHICLE, serverUrl.trimEnd('/'), clock.nowMs())
        return RegisterResult.Ok
    }

    private companion object {
        const val VEHICLE = "12가3456 · 아이오닉 6"
        val CODE = Regex("\\d{6}")
    }
}
