package kr.autolog.vehicle

import android.app.Application
import kr.autolog.vehicle.di.AppGraph
import kr.autolog.vehicle.di.createAppGraph

class AutoLogApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = createAppGraph(this)
    }
}
