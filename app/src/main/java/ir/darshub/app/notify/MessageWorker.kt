package ir.darshub.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ir.darshub.app.core.Api
import ir.darshub.app.core.Store
import java.util.concurrent.TimeUnit

/** Periodically checks unread DM count in the background and notifies on new messages. */
class MessageWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            Store.init(applicationContext)
            val token = Store.tokenFromDisk() ?: return Result.success()
            if (token.isBlank()) return Result.success()
            val count = runCatching { Api.obj(Api.get("dm_unread")).optInt("count", 0) }.getOrDefault(0)
            val seen = Store.dmSeenCount()
            if (count > seen && count > 0) {
                Notifications.notifyMessage(applicationContext, count)
            }
            Store.setDmSeenCount(count)
            // اعلان‌های اجتماع در پس‌زمینه (پوش بر اساس تنظیمات کاربر)
            runCatching { NotifCenter.refresh(applicationContext) }
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<MessageWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "dm_poll", ExistingPeriodicWorkPolicy.KEEP, req
            )
        }
    }
}
