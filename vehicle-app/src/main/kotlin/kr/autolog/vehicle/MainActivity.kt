package kr.autolog.vehicle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.nav.DeepLinkKeys
import kr.autolog.vehicle.nav.deepLinkRoute
import kr.autolog.vehicle.ui.root.AutoLogRoot

class MainActivity : ComponentActivity() {
    private val graph: AppGraph get() = (application as AutoLogApp).graph

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 재생성(창 크기 변경 등)에는 같은 알림 Intent를 다시 적용하지 않는다
        if (savedInstanceState == null) openDeepLink(intent)
        setContent { AutoLogRoot(graph = graph, onRequestBackground = { moveTaskToBack(true) }) }
    }

    /** 앱이 떠 있을 때 알림을 누르면 여기로 온다(스펙 6.10). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openDeepLink(intent)
    }

    private fun openDeepLink(intent: Intent?) {
        val route =
            deepLinkRoute(intent?.getStringExtra(DeepLinkKeys.ROUTE), intent?.getStringExtra(DeepLinkKeys.TRIP_ID))
        route?.let(graph.navigator::handleDeepLink)
    }
}
