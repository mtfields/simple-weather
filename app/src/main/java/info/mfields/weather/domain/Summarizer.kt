package info.mfields.weather.domain

import info.mfields.weather.DailySummary
import info.mfields.weather.network.HourlyPeriod
import java.time.ZonedDateTime

object Summarizer {
    fun daily(periods: List<HourlyPeriod>): List<DailySummary> {
        return periods.groupBy { ZonedDateTime.parse(it.startTime).toLocalDate() }.entries.take(5).map { (d, hrs) ->
            val high = hrs.maxOf { it.temperature }
            val low = hrs.minOf { it.temperature }
            val top = hrs.groupingBy { it.shortForecast }.eachCount().maxByOrNull { it.value }?.key ?: "Unknown"
            DailySummary(d.dayOfWeek.name.take(3), high, low, top)
        }
    }
}
