package com.weatherwidget.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.weatherwidget.app.data.DayForecast
import com.weatherwidget.app.utils.WeatherIconMapper
import com.weatherwidget.app.worker.WeatherUpdateWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            val loading = showLoading(context)
            for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, loading)
            schedulePeriodicUpdate(context)
            triggerImmediateUpdate(context)
        } catch (_: Exception) {
            val err = showError(context, "Error")
            for (id in appWidgetIds) runCatching { appWidgetManager.updateAppWidget(id, err) }
        }
    }

    override fun onEnabled(context: Context) {
        schedulePeriodicUpdate(context)
        triggerImmediateUpdate(context)
    }

    override fun onDisabled(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WeatherUpdateWorker.PERIODIC_WORK_NAME)
    }

    private fun schedulePeriodicUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeatherUpdateWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun triggerImmediateUpdate(context: Context) {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<WeatherUpdateWorker>().build())
    }

    companion object {

        private val DAY_VIEW_IDS = listOf(R.id.tv_day_0, R.id.tv_day_1, R.id.tv_day_2, R.id.tv_day_3, R.id.tv_day_4, R.id.tv_day_5, R.id.tv_day_6)

        fun buildViews(context: Context, cityName: String, days: List<DayForecast>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            val now = Calendar.getInstance()
            val cityShort = if (cityName.length > 12) cityName.take(11) + "…" else cityName
            val dateStr = SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(now.time)
            views.setTextViewText(R.id.tv_location, cityShort)
            views.setTextViewText(R.id.tv_date, dateStr)
            views.setOnClickPendingIntent(R.id.layout_header, alarmPendingIntent(context))

            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayFmt  = SimpleDateFormat("EEE", Locale.getDefault())
            DAY_VIEW_IDS.forEachIndexed { i, viewId ->
                val day = days.getOrNull(i) ?: return@forEachIndexed
                val label = if (i == 0) "Today"
                            else runCatching { dayFmt.format(dateFmt.parse(day.date)!!) }.getOrDefault("---")
                val emoji = WeatherIconMapper.getIconEmoji(day.weatherCode)
                views.setTextViewText(viewId, "$label\n$emoji\n${day.maxTempC.toInt()}°/${day.minTempC.toInt()}°")
            }
            return views
        }

        fun showLoading(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            views.setTextViewText(R.id.tv_day_2, "…")
            views.setOnClickPendingIntent(R.id.layout_header, alarmPendingIntent(context))
            return views
        }

        fun showError(context: Context, message: String): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            views.setTextViewText(R.id.tv_day_2, message)
            views.setOnClickPendingIntent(R.id.layout_header, alarmPendingIntent(context))
            return views
        }

        private fun alarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getAllWidgetIds(context: Context): IntArray =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))

    }

}
