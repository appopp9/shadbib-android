package ir.shadbib.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import ir.shadbib.app.core.Store
import java.util.Calendar

object Reminder {
    private const val REQ = 7001

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction("ir.shadbib.app.REMIND")
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQ, intent, flags)
    }

    fun apply(context: Context) {
        val p = Store.prefs.value
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!p.reminderEnabled) {
            am.cancel(pending(context)); return
        }
        schedule(context, p.reminderHour, p.reminderMinute)
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = pending(context)
        runCatching {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Notifications.notifyReminder(context)
        // reschedule for the next day
        runCatching {
            Store.init(context)
            val p = Store.prefs.value
            if (p.reminderEnabled) Reminder.schedule(context, p.reminderHour, p.reminderMinute)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        runCatching {
            Store.init(context)
            Reminder.apply(context)
            MessageWorker.schedule(context)
        }
    }
}
