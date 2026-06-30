package com.weatherwidget.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.weatherwidget.app.data.DayForecast
import com.weatherwidget.app.databinding.ItemDayForecastBinding
import com.weatherwidget.app.utils.WeatherIconMapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DayForecastAdapter(private val days: List<DayForecast>) :
    RecyclerView.Adapter<DayForecastAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDayForecastBinding) : RecyclerView.ViewHolder(binding.root)

    // DateTimeFormatter is thread-safe (unlike SimpleDateFormat)
    private val dateFmt      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val dayNameFmt   = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
    private val dateLabelFmt = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDayForecastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val day = days[position]
        val b = holder.binding
        val ctx = b.root.context
        val localDate = runCatching { LocalDate.parse(day.date, dateFmt) }.getOrNull()

        val dayName = if (position == 0) ctx.getString(R.string.today)
                      else localDate?.format(dayNameFmt) ?: ctx.getString(R.string.day_unknown)
        val dateLabel = localDate?.format(dateLabelFmt) ?: day.date
        val description = WeatherIconMapper.getDescription(day.weatherCode)

        b.tvEmoji.text = WeatherIconMapper.getIconEmoji(day.weatherCode)
        b.tvEmoji.contentDescription = description
        b.tvDayName.text = dayName
        b.tvDate.text = dateLabel
        b.tvDescription.text = description
        b.tvHigh.text = "${day.maxTempC.toInt()}°"
        b.tvHigh.contentDescription = ctx.getString(R.string.cd_high_temp, day.maxTempC.toInt())
        b.tvLow.text = "${day.minTempC.toInt()}°"
        b.tvLow.contentDescription = ctx.getString(R.string.cd_low_temp, day.minTempC.toInt())
        b.tvRain.text = "☂ ${day.precipitationProbability}%  ${if (day.precipitationMm > 0) "· ${"%.1f".format(day.precipitationMm)}mm" else ""}"
        b.tvRain.contentDescription = ctx.getString(R.string.cd_rain, day.precipitationProbability)
        b.tvWind.text = "💨 ${day.windspeedKph.toInt()} km/h"
        b.tvWind.contentDescription = ctx.getString(R.string.cd_wind, day.windspeedKph.toInt())
        b.tvUv.text = "☀ UV ${day.uvIndexMax.toInt()}"
        b.tvUv.contentDescription = ctx.getString(R.string.cd_uv, day.uvIndexMax.toInt())

        val isToday = position == 0
        b.cardRoot.setCardBackgroundColor(
            ContextCompat.getColor(ctx, if (isToday) R.color.color_card_today else R.color.color_card_regular)
        )
        b.divider.setBackgroundColor(
            ContextCompat.getColor(ctx, if (isToday) R.color.color_divider_today else R.color.color_divider_regular)
        )
        // Text colours are the same for both card types; set once
        val textPrimary   = ContextCompat.getColor(ctx, R.color.color_text_primary)
        val textSecondary = ContextCompat.getColor(ctx, R.color.color_text_secondary)
        val textStats     = ContextCompat.getColor(ctx, R.color.color_text_stats)
        val tempHigh      = ContextCompat.getColor(ctx, R.color.color_temp_high)
        val tempLow       = ContextCompat.getColor(ctx, R.color.color_temp_low)
        b.tvDayName.setTextColor(textPrimary)
        b.tvDate.setTextColor(textSecondary)
        b.tvDescription.setTextColor(textSecondary)
        b.tvHigh.setTextColor(tempHigh)
        b.tvLow.setTextColor(tempLow)
        b.tvRain.setTextColor(textStats)
        b.tvWind.setTextColor(textStats)
        b.tvUv.setTextColor(textStats)
    }

    override fun getItemCount() = days.size
}
