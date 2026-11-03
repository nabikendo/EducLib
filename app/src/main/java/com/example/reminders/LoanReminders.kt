package com.example.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.LoanEntity
import com.example.widget.LoansWidgetProvider
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class LoanDueSummary(val dueToday: Int, val overdue: Int, val upcoming: Int)

fun summarizeActiveLoans(loans: List<LoanEntity>, now: Long = System.currentTimeMillis()): LoanDueSummary {
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
    val upcomingLimit = Calendar.getInstance().apply {
        timeInMillis = todayStart
        add(Calendar.DAY_OF_YEAR, 8)
    }.timeInMillis
    val activeLoans = loans.filter { it.returnDate == null }
    return LoanDueSummary(
        dueToday = activeLoans.count { it.dueDate >= now && it.dueDate < tomorrow },
        overdue = activeLoans.count { it.dueDate < now },
        upcoming = activeLoans.count { it.dueDate >= tomorrow && it.dueDate < upcomingLimit }
    )
}

object LoanReminderSettings {
    private const val PREFS = "educlib_local_preferences"
    private const val KEY_ENABLED = "return_reminders_enabled"
    private const val UNIQUE_WORK = "educlib_daily_return_reminder"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
        val workManager = WorkManager.getInstance(context.applicationContext)
        if (enabled) {
            val now = System.currentTimeMillis()
            val nextReminder = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = (nextReminder.timeInMillis - now).coerceAtLeast(0L)
            val request = PeriodicWorkRequestBuilder<DueDateReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_WORK)
                .build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.REPLACE, request)
        } else {
            workManager.cancelUniqueWork(UNIQUE_WORK)
        }
    }
}

class DueDateReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!LoanReminderSettings.isEnabled(applicationContext)) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val summary = summarizeActiveLoans(database.loanDao().getActiveLoansSnapshot())
            LoansWidgetProvider.requestUpdate(applicationContext)
            if (summary.dueToday == 0 && summary.overdue == 0) {
                NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
                return Result.success()
            }

            createNotificationChannel()
            val message = when {
                summary.overdue > 0 && summary.dueToday > 0 -> "${summary.overdue} empréstimos atrasados e ${summary.dueToday} vencem hoje."
                summary.overdue > 0 -> "Você tem ${summary.overdue} empréstimos atrasados."
                else -> "${summary.dueToday} empréstimos vencem hoje."
            }
            val openApp = PendingIntent.getActivity(
                applicationContext,
                0,
                Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Lembrete da biblioteca")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de devolução",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumo diário de empréstimos vencendo ou atrasados."
                lockscreenVisibility = NotificationManager.VISIBILITY_PRIVATE
            }
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "return_due_reminders"
        private const val NOTIFICATION_ID = 16401
    }
}
