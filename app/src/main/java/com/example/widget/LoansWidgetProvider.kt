package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.reminders.summarizeActiveLoans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoansWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, manager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        updateWidgets(context, manager, intArrayOf(appWidgetId))
    }

    private fun updateWidgets(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val summary = runCatching {
                    val db = AppDatabase.getDatabase(context.applicationContext)
                    summarizeActiveLoans(db.loanDao().getActiveLoansSnapshot())
                }.getOrNull()
                withContext(Dispatchers.Main) {
                    appWidgetIds.forEach { id ->
                        val views = RemoteViews(context.packageName, R.layout.widget_loans)
                        val title = when {
                            summary == null -> "Abra o EducLib para atualizar"
                            summary.overdue > 0 -> "${summary.overdue} empréstimos atrasados"
                            summary.dueToday > 0 -> "${summary.dueToday} vencem hoje"
                            summary.upcoming > 0 -> "${summary.upcoming} devoluções próximas"
                            else -> "Tudo em dia"
                        }
                        val detail = if (summary == null) {
                            "Toque para tentar novamente"
                        } else {
                            "${summary.dueToday} hoje · ${summary.upcoming} nos próximos 7 dias"
                        }
                        views.setTextViewText(R.id.widget_summary, title)
                        views.setTextViewText(R.id.widget_detail, detail)
                        views.setContentDescription(R.id.widget_root, "$title. $detail. Toque para abrir a biblioteca.")
                        val openApp = PendingIntent.getActivity(
                            context,
                            id,
                            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, openApp)
                        manager.updateAppWidget(id, views)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, LoansWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val update = Intent(appContext, LoansWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(update)
        }
    }
}
