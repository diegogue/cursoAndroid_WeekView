package com.alamkanak.weekview.sample

import android.graphics.RectF
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.alamkanak.weekview.WeekView
import com.alamkanak.weekview.WeekViewEvent
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
        val currentHour = cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0
        weekView.goToHour(Math.max(currentHour - 1, 0.0))
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        weekView.goToDate(cal)
//        weekView.enableDrawHeaderBackgroundOnlyOnWeekDays = true
//        weekView.addEventClickListener = null
//        weekView.eventClickListener = object : WeekView.EventClickListener {
//            override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
//            }
//        }
//        weekView.columnGap
//        weekView.dayBackgroundColor=0xffffffff.toInt()
        weekView.emptyViewClickListener = null
        weekView.emptyViewLongPressListener = null
        weekView.scrollListener = object : WeekView.ScrollListener {
            val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())

            override fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?) {
                weekView.sideTitleText = monthFormatter.format(newFirstVisibleDay.time)
            }

        }

        draggable_view.visibility = View.GONE


        weekView.addEventClickListener = null
        weekView.headerRowBackgroundColor = 0xff242b3e.toInt()
        weekView.headerColumnTextColor = 0xc2c3c3c3.toInt()
        weekView.nowLineColor = 0xff112d71.toInt()
        weekView.eventClickListener = object : WeekView.EventClickListener {
            override fun onEventClick(event: WeekViewEvent, eventRect: RectF) {
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_today) {
            val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
            weekView.goToDate(cal)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        menu.findItem(R.id.action_day_view).isChecked = false
        menu.findItem(R.id.action_three_day_view).isChecked = false
        menu.findItem(R.id.action_week_view).isChecked = true
        return result
    }
}
