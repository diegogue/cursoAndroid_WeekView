package com.alamkanak.weekview

import java.util.*

interface DateTimeInterpreter {
    fun interpretDate(date: Calendar): String

    fun interpretTime(hour: Int, minutes: Int): String
}
