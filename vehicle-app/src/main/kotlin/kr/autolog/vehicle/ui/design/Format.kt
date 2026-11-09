package kr.autolog.vehicle.ui.design

import java.util.Locale
import kotlin.math.ceil

/** 표시용 숫자 포맷은 여기 한 곳(스펙 6.3). */
fun formatKm(km: Double?): String? = km?.let { String.format(Locale.US, "%,.1f", it) }

fun remainingSeconds(deadlineAt: Long, now: Long): Int = ceil((deadlineAt - now) / 1000.0).toInt().coerceAtLeast(0)

fun progressOf(since: Long, deadlineAt: Long, now: Long): Float {
    val total = (deadlineAt - since).coerceAtLeast(1)
    return ((deadlineAt - now).toFloat() / total).coerceIn(0f, 1f)
}
