package kr.autolog.vehicle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.autolog.vehicle.ui.root.AutoLogRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val graph = (application as AutoLogApp).graph
        setContent { AutoLogRoot(graph = graph, onRequestBackground = { moveTaskToBack(true) }) }
    }
}
