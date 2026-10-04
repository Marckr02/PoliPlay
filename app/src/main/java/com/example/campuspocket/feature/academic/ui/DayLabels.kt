package com.example.campuspocket.feature.academic.ui

import androidx.annotation.StringRes
import com.example.campuspocket.R
import java.time.DayOfWeek

@StringRes
fun DayOfWeek.labelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday
    DayOfWeek.TUESDAY -> R.string.day_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday
    DayOfWeek.THURSDAY -> R.string.day_thursday
    DayOfWeek.FRIDAY -> R.string.day_friday
    DayOfWeek.SATURDAY -> R.string.day_saturday
    DayOfWeek.SUNDAY -> R.string.day_sunday
}

@StringRes
fun DayOfWeek.shortLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_mon_short
    DayOfWeek.TUESDAY -> R.string.day_tue_short
    DayOfWeek.WEDNESDAY -> R.string.day_wed_short
    DayOfWeek.THURSDAY -> R.string.day_thu_short
    DayOfWeek.FRIDAY -> R.string.day_fri_short
    DayOfWeek.SATURDAY -> R.string.day_sat_short
    DayOfWeek.SUNDAY -> R.string.day_sun_short
}
