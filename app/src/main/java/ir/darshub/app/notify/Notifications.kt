package ir.darshub.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.darshub.app.MainActivity
import ir.darshub.app.R

object Notifications {
    const val CH_MESSAGES = "messages"
    const val CH_REMINDER = "reminder"
    const val CH_STUDY = "study"
    const val CH_SOCIAL = "social"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_MESSAGES, "پیام‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "پیام‌های خصوصی و گروهی"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SOCIAL, "اجتماع", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "فالو، لایک، پاسخ و منشن‌ها"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "یادآوری مطالعه", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "یادآوری روزانه درس خواندن"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_STUDY, "در حال مطالعه", NotificationManager.IMPORTANCE_LOW).apply {
                description = "نمایش زمان مطالعه هنگام تمرکز"
                setShowBadge(false)
            }
        )
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    private fun post(context: Context, id: Int, channel: String, title: String, text: String) {
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_splash)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }

    fun notifyMessage(context: Context, count: Int) {
        ensureChannels(context)
        val text = if (count > 1) "$count پیام جدید داری" else "یک پیام جدید داری"
        post(context, 1001, CH_MESSAGES, "درس هاب", text)
    }

    fun notifyReminder(context: Context) {
        ensureChannels(context)
        post(context, 1002, CH_REMINDER, "وقت مطالعه‌ست! 📚", "بزن بریم، امروز هم استریکت رو حفظ کن 🔥")
    }
}

fun Notifications.notifySocial(context: Context, id: Int, title: String, text: String) {
    runCatching {
        ensureChannels(context)
        val n = NotificationCompat.Builder(context, Notifications.CH_SOCIAL)
            .setSmallIcon(R.drawable.ic_splash)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(run {
                val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0))
            })
            .build()
        NotificationManagerCompat.from(context).notify(200000 + id % 100000, n)
    }
}
