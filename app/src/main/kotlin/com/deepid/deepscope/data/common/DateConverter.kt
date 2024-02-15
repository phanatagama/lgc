package com.deepid.deepscope.data.common

import android.util.Log
import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DateConverter {

    @TypeConverter
    fun toDate(date: String?): LocalDateTime? {
        if (date == null) return null

        return date.toDate()
    }

    @TypeConverter
    fun toDateString(date: LocalDateTime?): String? {
        if (date == null) return null

        return date.toDateString()
    }

}

fun LocalDateTime.toDateString(): String {
    return this.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
}
fun String.toDate(): LocalDateTime {
    Log.d(null, "toDate: $this 00:00")
    return LocalDateTime.parse("$this 00:00", DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"))
}