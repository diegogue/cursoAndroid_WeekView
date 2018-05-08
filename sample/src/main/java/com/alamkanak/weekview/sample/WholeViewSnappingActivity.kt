package com.alamkanak.weekview.sample

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.alamkanak.weekview.WeekView
import kotlinx.android.synthetic.main.activity_base.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity to demonstrate snapping of the whole view, for example week-by-week.
 */
class WholeViewSnappingActivity : BasicActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weekView.isShowNowLine = true
//        weekView.setAutoLimitTime(true)
        weekView.setLimitTime(0, 24)
        weekView.isScrollNumberOfVisibleDays = true
        setDayViewType(TYPE_WEEK_VIEW)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        weekView.goToDate(cal)
//        weekView.enableDrawHeaderBackgroundOnlyOnWeekDays = true
//        weekView.addEventClickListener = null
//        weekView.eventClickListener = object : WeekView.EventClickListener {
//            override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
//            }
//        }
//        weekView.emptyViewClickListener = null
//        weekView.emptyViewLongPressListener = null
        weekView.scrollListener = object : WeekView.ScrollListener {
            val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())

            override fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?) {
                weekView.sideTitleText = monthFormatter.format(newFirstVisibleDay.time)
            }

        }

        draggable_view.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_today) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            weekView.goToDate(cal)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
