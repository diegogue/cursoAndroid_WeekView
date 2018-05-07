package com.alamkanak.weekview.sample

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_base.*
import java.util.*

/**
 * A basic example of how to use week view library.
 * Created by Raquib-ul-Alam Kanak on 1/3/2014.
 * Website: http://alamkanak.github.io
 */
class WholeViewSnappingActivity : BasicActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        weekView.isShowNowLine = true
//        weekView.setAutoLimitTime(true)
        weekView.setMinTime(0)
        weekView.setMaxTime(24)
        weekView.isScrollNumberOfVisibleDays = true
        setDayViewType(TYPE_WEEK_VIEW)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        weekView.goToDate(cal)
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
