package kr.autolog.vehicle

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.autolog.vehicle.core.SessionState
import kr.autolog.vehicle.core.TripSessionApi
import java.util.Locale

/** 다른 앱이 전면이어도 보이는 운행 상태. 실제 기록 상태와 거리는 앱 A 세션을 그대로 따른다. */
object RecordingPill {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var app: Application? = null
    private var job: Job? = null
    private var window: WindowManager? = null
    private var pill: View? = null
    private var label: TextView? = null
    private var current: String? = null
    private var windowBlocked = false
    private var appAreaCenterX: Int? = null

    fun start(application: Application, session: TripSessionApi) {
        app = application
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                current = (session.state.value as? SessionState.Recording)?.let { state ->
                    val distance = state.liveKm?.takeIf { it.isFinite() && it >= 0.0 }
                        ?.let { String.format(Locale.KOREA, "%.1f", it) } ?: "—"
                    val minutes = ((System.currentTimeMillis() - state.trip.startTime).coerceAtLeast(0) / 60_000)
                    "$distance km · ${minutes}분"
                }
                render()
                delay(1_000)
            }
        }
    }

    /** Pleos의 오른쪽 앱 영역(내비가 차지하는 영역) 중앙을 기억한다. 창 크기 변경 때도 갱신한다. */
    fun setAppArea(view: View) {
        if (!view.isAttachedToWindow || view.width == 0) return
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val centerX = location[0] + view.width / 2
        if (appAreaCenterX == centerX) return
        appAreaCenterX = centerX
        val overlay = pill ?: return
        val params = overlay.layoutParams as? WindowManager.LayoutParams ?: return
        params.x = horizontalOffset(view.context, appAreaCenterX)
        window?.updateViewLayout(overlay, params)
    }

    /** 첫 실행에 한 번 안내한다. 거절했어도 설정의 버튼으로 다시 열 수 있다. */
    fun requestPermissionIfAvailable(activity: Activity) {
        if (app == null || Settings.canDrawOverlays(activity)) return
        val prefs = activity.getSharedPreferences("recording_pill", Activity.MODE_PRIVATE)
        if (prefs.getBoolean("permission_requested", false)) return
        if (openPermissionSettings(activity)) {
            prefs.edit().putBoolean("permission_requested", true).apply()
        }
    }

    /** 시스템이 제공하는 특수 권한 화면만 연다. 앱이 스스로 권한을 부여하지 않는다. */
    fun openPermissionSettings(activity: Activity): Boolean {
        if (Settings.canDrawOverlays(activity)) return true
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            // Android 11+는 package: URI를 무시하고 전체 앱 목록을 연다.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                data = Uri.parse("package:${activity.packageName}")
            }
        }
        try {
            activity.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            Log.w(TAG, "overlay permission screen unavailable; recording notification remains active")
        } catch (error: SecurityException) {
            Log.w(TAG, "overlay permission screen denied; recording notification remains active", error)
        }
        return false
    }

    private fun render() {
        val context = app ?: return
        val text = current
        if (text == null || !Settings.canDrawOverlays(context)) {
            remove()
            return
        }
        if (windowBlocked) return
        if (pill == null) {
            val wm = context.getSystemService(WindowManager::class.java)
            val (view, textView) = createView(context)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                alpha = 0.8f
                x = horizontalOffset(context, appAreaCenterX)
                y = context.dp(100)
            }
            try {
                wm.addView(view, params)
                window = wm
                pill = view
                label = textView
            } catch (error: RuntimeException) {
                windowBlocked = true
                Log.w(TAG, "overlay unavailable; recording notification remains active", error)
                return
            }
        }
        label?.text = text
        pill?.contentDescription = "운행 기록 중, $text"
    }

    private fun remove() {
        pill?.let { view -> window?.removeView(view) }
        pill = null
        label = null
        window = null
    }

    private fun createView(context: Application): Pair<View, TextView> {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dp(24), context.dp(16), context.dp(24), context.dp(16))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = context.dp(34).toFloat()
                setColor(Color.WHITE)
                setStroke(context.dp(1), Color.rgb(221, 228, 236))
            }
            elevation = context.dp(6).toFloat()
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        row.addView(
            View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(0, 167, 99))
                }
            },
            LinearLayout.LayoutParams(context.dp(14), context.dp(14)).apply { marginEnd = context.dp(16) },
        )
        val text = TextView(context).apply {
            setTextColor(Color.rgb(23, 32, 45))
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        row.addView(text)
        return row to text
    }

    private fun android.content.Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun horizontalOffset(context: android.content.Context, centerX: Int?): Int {
        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds.width()
        } else {
            context.resources.displayMetrics.widthPixels
        }
        return (centerX ?: screenWidth * 2 / 3) - screenWidth / 2
    }

    private const val TAG = "AutoLog/pill"
}
