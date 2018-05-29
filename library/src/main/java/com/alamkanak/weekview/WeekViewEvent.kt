package com.alamkanak.weekview

import android.graphics.Shader
import android.support.annotation.ColorInt
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import java.util.*

class WeekViewEvent {
    var id: String? = null
    var startTime: Calendar? = null
        set(value) {
            field = value
            resetDatesForAllDayIfNeeded()
        }
    var endTime: Calendar? = null
        set(value) {
            field = value
            resetDatesForAllDayIfNeeded()
        }
    var name: String? = null
    var location: String? = null
    @ColorInt
    @get:ColorInt
    var color: Int = 0
    var isAllDay: Boolean = false
        set(value) {
            field = value
            resetDatesForAllDayIfNeeded()
        }
    var shader: Shader? = null

    constructor()

    private fun resetDatesForAllDayIfNeeded() {
        if (!isAllDay)
            return
        when {
            startTime == null && endTime != null -> {
                WeekViewUtil.resetTime(endTime!!)
                startTime = endTime
            }
            startTime != null && endTime == null -> {
                WeekViewUtil.resetTime(startTime!!)
                endTime = startTime
            }
            startTime != null && endTime != null -> {
                WeekViewUtil.resetTime(startTime!!)
                if (!WeekViewUtil.isSameDay(startTime!!, endTime!!))
                    WeekViewUtil.resetTime(endTime!!)
                else if (endTime !== startTime)
                    endTime = startTime
            }
        }
    }

    /**CTOR for a single, all day event*/
    constructor(id: String?, name: String?, location: String? = null, allDayTime: Calendar, shader: Shader? = null) : this(id, name, location, allDayTime, allDayTime, true, shader)

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
    @JvmOverloads constructor(id: String?, name: String?, location: String?, startTime: Calendar, endTime: Calendar, allDay: Boolean = false, shader: Shader? = null) {
        this.id = id
        this.name = name
        this.location = location
        this.isAllDay = allDay
        this.startTime = startTime
        this.endTime = endTime
        this.startTime = startTime
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
    constructor(id: String?, name: String, startTime: Calendar, endTime: Calendar) : this(id, name, null, startTime, endTime)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as WeekViewEvent?
        return id == that!!.id
    }

    override fun hashCode(): Int {
        return id!!.hashCode()
    }

    fun splitWeekViewEvents(): MutableList<WeekViewEvent> {
        //This function splits the WeekViewEvent in WeekViewEvents by day
        if (this.endTime == null || isSameDay(this.startTime!!, this.endTime!!)) {
            val events = ArrayList<WeekViewEvent>(1)
            events.add(this)
            return events
        }
        val events = ArrayList<WeekViewEvent>()
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        var endTime = this.endTime!!.clone() as Calendar
        endTime.add(Calendar.MILLISECOND, -1)
        endTime = this.startTime!!.clone() as Calendar
        endTime.set(Calendar.HOUR_OF_DAY, 23)
        endTime.set(Calendar.MINUTE, 59)
        val event1 = WeekViewEvent(this.id!!, this.name, this.location, this.startTime!!, endTime, this.isAllDay)
        event1.color = this.color
        events.add(event1)

        // Add other days.
        if (!isSameDay(this.startTime!!, this.endTime!!)) {
            val otherDay = this.startTime!!.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!isSameDay(otherDay, this.endTime!!)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(this.id!!, this.name, null, overDay, endOfOverDay, this.isAllDay)
                eventMore.color = this.color
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }
            // Add last day.
            val startTime = this.endTime!!.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(this.id!!, this.name, this.location, startTime, this.endTime!!, this.isAllDay)
            event2.color = this.color
            events.add(event2)
        }

        return events
    }

    override fun toString(): String {
        val colorStr = "#${Integer.toHexString(color)}"
        val startTimeStr = WeekViewUtil.calendarToString(startTime, !isAllDay)
        if (isAllDay) {
            if (endTime != null || WeekViewUtil.isSameDay(startTime!!, endTime!!))
                return "allDayEvent(id=$id, time=$startTimeStr..${WeekViewUtil.calendarToString(startTime, false)}, name=$name, location=$location, color=$colorStr ,shader=$shader)"
            return "allDayEvent(id=$id, time=$startTimeStr, name=$name, location=$location, color=$colorStr ,shader=$shader)"
        }
        if (endTime != null)
            return "normalEvent(id=$id, startTime=$startTimeStr, name=$name, location=$location, color=$colorStr , shader=$shader)"
        val endTimeStr = WeekViewUtil.calendarToString(endTime, true)
        return "normalEvent(id=$id, startTime=$colorStr, endTime=$endTimeStr, name=$name, location=$location, color=$colorStr , shader=$shader)"
    }
}
