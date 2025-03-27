package com.example.dhbt.presentation.util

import java.time.DayOfWeek
import java.time.Month

// Helper class to store localized month names
class MonthLocalization(
    val january: String,
    val february: String,
    val march: String,
    val april: String,
    val may: String,
    val june: String,
    val july: String,
    val august: String,
    val september: String,
    val october: String,
    val november: String,
    val december: String
) {
    fun getLocalizedMonth(month: Month): String {
        return when (month) {
            Month.JANUARY -> january
            Month.FEBRUARY -> february
            Month.MARCH -> march
            Month.APRIL -> april
            Month.MAY -> may
            Month.JUNE -> june
            Month.JULY -> july
            Month.AUGUST -> august
            Month.SEPTEMBER -> september
            Month.OCTOBER -> october
            Month.NOVEMBER -> november
            Month.DECEMBER -> december
        }
    }
}

// Helper class to store localized day of week abbreviations
class DayOfWeekLocalization(
    val monday: String,
    val tuesday: String,
    val wednesday: String,
    val thursday: String,
    val friday: String,
    val saturday: String,
    val sunday: String
) {
    fun getLocalizedDayOfWeek(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> monday
            DayOfWeek.TUESDAY -> tuesday
            DayOfWeek.WEDNESDAY -> wednesday
            DayOfWeek.THURSDAY -> thursday
            DayOfWeek.FRIDAY -> friday
            DayOfWeek.SATURDAY -> saturday
            DayOfWeek.SUNDAY -> sunday
        }
    }
}