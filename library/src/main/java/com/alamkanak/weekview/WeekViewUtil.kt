package com.alamkanak.weekview

import java.util.*

/**
 * Created by jesse on 6/02/2016.
 */
object WeekViewUtil {


    /////////////////////////////////////////////////////////////////
    //
    //      Helper methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Checks if two dates are on the same day.
     *
     * @param dateOne The first date.
     * @param dateTwo The second date.     *
     * @return Whether the dates are on the same day.
     */
    @JvmStatic
    fun isSameDay(dateOne: Calendar, dateTwo: Calendar): Boolean {
        return dateOne.get(Calendar.YEAR) == dateTwo.get(Calendar.YEAR) && dateOne.get(Calendar.DAY_OF_YEAR) == dateTwo.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Returns a calendar instance at the start of today
     *
     * @return the calendar instance
     */
    @JvmStatic
    fun today(): Calendar {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }

    /**
     * Checks if two dates are on the same day and hour.
     *
     * @param dateOne The first day.
     * @param dateTwo The second day.
     * @return Whether the dates are on the same day and hour.
     */
    @JvmStatic
    fun isSameDayAndHour(dateOne: Calendar, dateTwo: Calendar?): Boolean {

        return if (dateTwo != null) {
            isSameDay(dateOne, dateTwo) && dateOne.get(Calendar.HOUR_OF_DAY) == dateTwo.get(Calendar.HOUR_OF_DAY)
        } else false
    }

    /**
     * Returns the amount of days between the second date and the first date
     *
     * @param dateOne the first date
     * @param dateTwo the second date
     * @return the amount of days between dateTwo and dateOne
     */
    @JvmStatic
    fun daysBetween(dateOne: Calendar, dateTwo: Calendar): Int {
        return ((dateTwo.timeInMillis + dateTwo.timeZone.getOffset(dateTwo.timeInMillis)) / (1000 * 60 * 60 * 24) - (dateOne.timeInMillis + dateOne.timeZone.getOffset(dateOne.timeInMillis)) / (1000 * 60 * 60 * 24)).toInt()
    }

    /*
    * Returns the amount of minutes passed in the day before the time in the given date
    * @param date
    * @return amount of minutes in day before time
    */
    @JvmStatic
    fun getPassedMinutesInDay(date: Calendar): Int {
        return getPassedMinutesInDay(date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE))
    }

    /**
     * Returns the amount of minutes in the given hours and minutes
     *
     * @param hour
     * @param minute
     * @return amount of minutes in the given hours and minutes
     */
    @JvmStatic
    fun getPassedMinutesInDay(hour: Int, minute: Int): Int {
        return hour * 60 + minute
    }
}
