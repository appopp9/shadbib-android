package ir.darshub.app

import android.app.Application
import ir.darshub.app.core.Store
import ir.darshub.app.notify.MessageWorker
import ir.darshub.app.notify.Notifications

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Store.init(this)
        Notifications.ensureChannels(this)
        ir.darshub.app.notify.NotifCenter.start(this)
        MessageWorker.schedule(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
