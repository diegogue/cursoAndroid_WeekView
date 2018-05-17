package com.alamkanak.weekview

import android.graphics.Shader
import android.support.annotation.ColorInt
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import java.util.*

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://april-shower.com
 */
class WeekViewEvent {
    var identifier: String? = null
    var startTime: Calendar? = null
    var endTime: Calendar? = null
    var name: String? = null
    var location: String? = null
    @ColorInt
    @get:ColorInt
    var color: Int = 0
    var isAllDay: Boolean = false
    var shader: Shader? = null

    var id: Long
        @Deprecated("")
        get() = java.lang.Long.parseLong(identifier)
        @Deprecated("")
        set(id) {
            this.identifier = id.toString()
        }

    constructor()

    /**
     * Initializes the event for week view.
     *
     * @param id          The id of the event as String.
     * @param name        Name of the event.
     * @param startYear   Year when the event starts.
     * @param startMonth  Month when the event starts.
     * @param startDay    Day when the event starts.
     * @param startHour   Hour (in 24-hour format) when the event starts.
     * @param startMinute Minute when the event starts.
     * @param endYear     Year when the event ends.
     * @param endMonth    Month when the event ends.
     * @param endDay      Day when the event ends.
     * @param endHour     Hour (in 24-hour format) when the event ends.
     * @param endMinute   Minute when the event ends.
     */
    constructor(id: String, name: String, startYear: Int, startMonth: Int, startDay: Int, startHour: Int, startMinute: Int, endYear: Int, endMonth: Int, endDay: Int, endHour: Int, endMinute: Int) {
        this.identifier = id

        startTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, startYear)
            set(Calendar.MONTH, startMonth - 1)
            set(Calendar.DAY_OF_MONTH, startDay)
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
        }
        endTime = Calendar.getInstance().apply {
            set(Calendar.YEAR, endYear)
            set(Calendar.MONTH, endMonth - 1)
            set(Calendar.DAY_OF_MONTH, endDay)
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
        }
        this.name = name
    }

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event as String.
     * @param name      Name of the event.
     * @param location  The location of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     * @param allDay    Is the event an all day event.
     * @param shader    the Shader of the event rectangle
     */
    @JvmOverloads constructor(id: String, name: String?, location: String?, startTime: Calendar, endTime: Calendar, allDay: Boolean = false, shader: Shader? = null) {
        this.identifier = id
        this.name = name
        this.location = location
        this.startTime = startTime
        this.endTime = endTime
        this.isAllDay = allDay
        this.shader = shader
    }

    /**
     * Initializes the event for week view.
     *
     * @param id        The id of the event specified as String.
     * @param name      Name of the event.
     * @param startTime The time when the event starts.
     * @param endTime   The time when the event ends.
     */
    constructor(id: String, name: String, startTime: Calendar, endTime: Calendar) : this(id, name, null, startTime, endTime) {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as WeekViewEvent?

        return identifier == that!!.identifier
    }

    override fun hashCode(): Int {
        return identifier!!.hashCode()
    }

    fun splitWeekViewEvents(): MutableList<WeekViewEvent> {
        //This function splits the WeekViewEvent in WeekViewEvents by day
        val events = ArrayList<WeekViewEvent>()
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        var endTime = this.endTime!!.clone() as Calendar
        endTime.add(Calendar.MILLISECOND, -1)
        if (!isSameDay(this.startTime!!, endTime)) {
            endTime = this.startTime!!.clone() as Calendar
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)
            val event1 = WeekViewEvent(this.identifier!!, this.name, this.location, this.startTime!!, endTime, this.isAllDay)
            event1.color = this.color
            events.add(event1)

            // Add other days.
            val otherDay = this.startTime!!.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!isSameDay(otherDay, this.endTime!!)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(this.identifier!!, this.name, null, overDay, endOfOverDay, this.isAllDay)
                eventMore.color = this.color
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            // Add last day.
            val startTime = this.endTime!!.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(this.identifier!!, this.name, this.location, startTime, this.endTime!!, this.isAllDay)
            event2.color = this.color
            events.add(event2)
        } else {
            events.add(this)
        }

        return events
    }
}
