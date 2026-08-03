package ir.shadbib.app

import android.app.Application
import ir.shadbib.app.core.Store
import ir.shadbib.app.notify.MessageWorker
import ir.shadbib.app.notify.Notifications

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Store.init(this)
        Notifications.ensureChannels(this)
        ir.shadbib.app.notify.NotifCenter.start(this)
        MessageWorker.schedule(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
