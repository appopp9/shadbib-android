package ir.shadbib.app.notify

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.shadbib.app.R
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.Pomodoro
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Shows an ongoing (non-dismissible) notification with a live study time while studying. */
object StudyNotifier {
    private const val ID = 1003
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val faDigits = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
    private fun fa(s: String) = buildString { for (c in s) append(if (c in '0'..'9') faDigits[c - '0'] else c) }

    fun sync(context: Context) {
        val ctx = context.applicationContext
        val running = Chrono.running || Pomodoro.state.value.running
        if (running) {
            if (job == null) job = scope.launch { loop(ctx) }
        } else {
            job?.cancel(); job = null
            runCatching { NotificationManagerCompat.from(ctx).cancel(ID) }
        }
    }

    private suspend fun loop(ctx: Context) {
        Notifications.ensureChannels(ctx)
        while (Chrono.running || Pomodoro.state.value.running) {
            val pomo = Pomodoro.state.value
            val (title, text) = if (pomo.running) {
                val m = pomo.remainingSec / 60; val s = pomo.remainingSec % 60
                val phase = when (pomo.phase) { Pomodoro.Phase.WORK -> "تمرکز"; Pomodoro.Phase.BREAK -> "استراحت"; Pomodoro.Phase.LONG_BREAK -> "استراحت بلند"; else -> "پومودورو" }
                "در حال مطالعه — $phase" to fa(String.format(Locale.US, "%02d:%02d", m, s)) + " مانده"
            } else {
                val sec = Chrono.elapsedMs() / 1000
                "در حال مطالعه ⏱" to fa(String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60))
            }
            val n = NotificationCompat.Builder(ctx, Notifications.CH_STUDY)
                .setSmallIcon(R.drawable.ic_splash)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            runCatching { NotificationManagerCompat.from(ctx).notify(ID, n) }
            delay(1000)
        }
        runCatching { NotificationManagerCompat.from(ctx).cancel(ID) }
        job = null
    }
}
