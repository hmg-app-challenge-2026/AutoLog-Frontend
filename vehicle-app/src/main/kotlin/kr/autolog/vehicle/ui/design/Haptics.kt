package kr.autolog.vehicle.ui.design

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** 디자인 언어 8.3. 헤드유닛 지원 여부는 V-07 — 지원 안 하면 조용히 무시된다. */
fun View.confirmHaptic() {
    val constant = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    performHapticFeedback(constant)
}
