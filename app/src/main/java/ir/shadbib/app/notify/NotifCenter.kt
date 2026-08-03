package ir.shadbib.app.notify

import android.content.Context
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Store
import ir.shadbib.app.data.Notif
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** مرکز اعلان‌ها: هر ۲۰ ثانیه سرور را چک می‌کند، لیست را برای صفحه خانه نگه می‌دارد
 *  و بر اساس تنظیمات کاربر، موارد جدید را به‌صورت پوش نوتیفیکیشن نشان می‌دهد. */
object NotifCenter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _notifs = MutableStateFlow<List<Notif>>(emptyList())
    val notifs: StateFlow<List<Notif>> get() = _notifs
    private var started = false

    fun start(appContext: Context) {
        if (started) return
        started = true
        scope.launch {
            while (true) {
                if (!Store.token.isNullOrBlank()) refresh(appContext)
                delay(20000)
            }
        }
    }

    suspend fun refresh(appContext: Context, markRead: Boolean = false) {
        runCatching {
            val raw = if (markRead) Api.post("notif_list", org.json.JSONObject()) else Api.get("notif_list")
            val list = Notif.list(Api.arr(raw))
            val lastSeen = Store.lastNotifId()
            val enabled = Store.prefs.value.pushTypes
            val fresh = list.filter { it.id > lastSeen && !it.read }
            if (fresh.isNotEmpty()) {
                fresh.take(4).forEach { n ->
                    if (enabled.contains(n.type)) {
                        Notifications.notifySocial(appContext, n.id, n.title, n.text ?: "")
                    }
                }
                Store.setLastNotifId(list.maxOf { it.id })
            }
            _notifs.value = list
        }
    }

    fun markAllRead(appContext: Context) {
        // فوری در UI خوانده شود؛ سرور هم در پس‌زمینه آپدیت می‌شود
        _notifs.value = _notifs.value.map { it.copy(read = true) }
        scope.launch {
            runCatching { Api.post("notif_list", org.json.JSONObject()) }
            refresh(appContext)
        }
    }
}
