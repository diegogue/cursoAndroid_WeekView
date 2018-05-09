package com.alamkanak.weekview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v7.content.res.AppCompatResources
import android.text.*
import android.text.format.DateFormat
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.OverScroller
import com.alamkanak.weekview.WeekViewUtil.daysBetween
import com.alamkanak.weekview.WeekViewUtil.getPassedMinutesInDay
import com.alamkanak.weekview.WeekViewUtil.isSameDay
import com.alamkanak.weekview.WeekViewUtil.today
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://alamkanak.github.io/
 */
class WeekView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {
    //region fields and properties
    private var mHomeDate: Calendar? = null
    /**
     * Get the earliest day that can be displayed. Will return null if no minimum date is set.
     *
     * @return the earliest day that can be displayed, null if no minimum date set
     */
    /**
     * Set the earliest day that can be displayed. This will determine the left horizontal scroll
     * limit. The default value is null (allow unlimited scrolling into the past).
     *
     * @param minDate The new minimum date (pass null for no minimum)
     */
    var minDate: Calendar? = null
        set(minDate) {
            if (minDate != null) {
                minDate.set(Calendar.HOUR_OF_DAY, 0)
                minDate.set(Calendar.MINUTE, 0)
                minDate.set(Calendar.SECOND, 0)
                minDate.set(Calendar.MILLISECOND, 0)
                if (maxDate != null && minDate.after(maxDate)) {
                    throw IllegalArgumentException("minDate cannot be later than maxDate")
                }
            }

            field = minDate
            resetHomeDate()
            mCurrentOrigin.x = 0f
            invalidate()
        }
    /**
     * Get the latest day that can be displayed. Will return null if no maximum date is set.
     *
     * @return the latest day the can be displayed, null if no max date set
     */
    /**
     * Set the latest day that can be displayed. This will determine the right horizontal scroll
     * limit. The default value is null (allow unlimited scrolling in to the future).
     *
     * @param maxDate The new maximum date (pass null for no maximum)
     */
    var maxDate: Calendar? = null
        set(maxDate) {
            if (maxDate != null) {
                maxDate.set(Calendar.HOUR_OF_DAY, 0)
                maxDate.set(Calendar.MINUTE, 0)
                maxDate.set(Calendar.SECOND, 0)
                maxDate.set(Calendar.MILLISECOND, 0)
                if (minDate != null && maxDate.before(minDate)) {
                    throw IllegalArgumentException("maxDate has to be after minDate")
                }
            }

            field = maxDate
            resetHomeDate()
            mCurrentOrigin.x = 0f
            invalidate()
        }
    private val mTimeTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mTimeTextWidth: Float = 0f
    private var mTimeTextHeight: Float = 0f
    private val mHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var mHeaderTextHeight: Float = 0f
    private var mHeaderHeight: Float = 0f
    private var mGestureDetector: GestureDetectorCompat? = null
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private val mEmptyEventPaint = Paint()
    private val mHeaderBackgroundPaint: Paint = Paint()
    private var mWidthPerDay: Float = 0f
    private val mDayBackgroundPaint: Paint = Paint()
    private val mHourSeparatorPaint = Paint()
    private var mHeaderMarginBottom: Float = 0f
    private val mTodayBackgroundPaint: Paint = Paint()
    private val mFutureBackgroundPaint: Paint = Paint()
    private val mPastBackgroundPaint = Paint()
    private val mFutureWeekendBackgroundPaint = Paint()
    private val mPastWeekendBackgroundPaint = Paint()
    private val mNowLinePaint = Paint()
    private val mTodayHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mEventBackgroundPaint = Paint()
    private val mNewEventBackgroundPaint = Paint()
    private var mHeaderColumnWidth: Float = 0f
    private var mEventRects: MutableList<EventRect>? = null
    private var mEvents: MutableList<WeekViewEvent>? = null
    private val mEventTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
    private val mHeaderColumnBackgroundPaint: Paint = Paint()
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.
    private var mRefreshEvents = false
    private var mCurrentFlingDirection = Direction.NONE
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mIsZooming: Boolean = false
    /**
     * Returns the first visible day in the week view.
     *
     * @return The first visible day in the week view.
     */
    var firstVisibleDay: Calendar? = null
        private set
    /**
     * Returns the last visible day in the week view.
     *
     * @return The last visible day in the week view.
     */
    var lastVisibleDay: Calendar? = null
        private set
    private var mMinimumFlingVelocity = 0
    private var mScaledTouchSlop = 0
    private var mNewEventRect: EventRect? = null
    var textColorPicker: TextColorPicker? = null
    private var mSizeOfWeekView: Float = 0f
    private var mDistanceDone = 0f
    private var mDistanceMin: Float = 0f
    private var mOffsetValueToSecureScreen = 9
    private var mStartOriginForScroll = 0f

    // Attributes and their default values.
    private var mHourHeight =TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, resources.displayMetrics).toInt()
    private var mNewHourHeight = -1
    var minHourHeight = 0
    //no minimum specified (will be dynamic, based on screen)
    private var mEffectiveMinHourHeight = minHourHeight
    //compensates for the fact that you can't keep zooming out.
    var maxHourHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 125f, resources.displayMetrics).toInt()
    private var mColumnGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
    private var mFirstDayOfWeek = Calendar.getInstance().firstDayOfWeek
    private var mTextSize = 12
    private var mHeaderColumnPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
    private var mHeaderColumnTextColor = Color.BLACK
    private var mNumberOfVisibleDays = 3
    private var mHeaderRowPadding =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()
    private var mHeaderRowBackgroundColor = Color.WHITE
    private var mDayBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastBackgroundColor = Color.rgb(227, 227, 227)
    private var mFutureBackgroundColor = Color.rgb(245, 245, 245)
    private var mPastWeekendBackgroundColor = 0
    private var mFutureWeekendBackgroundColor = 0
    private var mNowLineColor = Color.rgb(102, 102, 102)
    private var mNowLineThickness =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics).toInt()
    private var mHourSeparatorColor = Color.rgb(230, 230, 230)
    private var mTodayBackgroundColor = Color.rgb(239, 247, 254)
    private var mHourSeparatorHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, resources.displayMetrics).toInt()
    private var mTodayHeaderTextColor = Color.rgb(39, 137, 228)
    private var mEventTextSize = 12
    private var mEventTextColor = Color.BLACK
    private var mEventPadding =  TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics).toInt()
    private var mHeaderColumnBackgroundColor = Color.WHITE
    private var mDefaultEventColor: Int = 0
    private var mNewEventColor: Int = 0
    var newEventIdentifier: String? = "-100"
    var newEventIconDrawable: Drawable? = null
    var newEventLengthInMinutes = 60
    var newEventTimeResolutionInMinutes = 15
    var isShowFirstDayOfWeekFirst = false

    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    @Deprecated("")
    private var mDayNameLength = LENGTH_LONG
    private var mOverlappingEventGap = 0
    private var mEventMarginVertical = 0
    /**
     * Get the scrolling speed factor in horizontal direction.
     *
     * @return The speed factor in horizontal direction.
     */
    /**
     * Sets the speed for horizontal scrolling.
     *
     * @param xScrollingSpeed The new horizontal scrolling speed.
     */
    var xScrollingSpeed = 1f
    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0
    /**
     * Set corner radius for event rect.
     *
     * @param eventCornerRadius the radius in px.
     */
    var eventCornerRadius = 0
    private var mShowDistinctWeekendColor = false
    private var mShowNowLine = false
    private var mShowDistinctPastFutureColor = false
    /**
     * Get whether the week view should fling horizontally.
     *
     * @return True if the week view has horizontal fling enabled.
     */
    /**
     * Set whether the week view should fling horizontally.
     *
     * @param enabled whether the week view should fling horizontally
     */
    var isHorizontalFlingEnabled = true
    /**
     * Get whether the week view should fling vertically.
     *
     * @return True if the week view has vertical fling enabled.
     */
    /**
     * Set whether the week view should fling vertically.
     *
     * @param enabled whether the week view should fling vertically
     */
    var isVerticalFlingEnabled = true
    /**
     * Get the height of AllDay-events.
     *
     * @return Height of AllDay-events.
     */
    /**
     * Set the height of AllDay-events.
     *
     * @param height the new height of AllDay-events
     */
    var allDayEventHeight = 100
    private var mZoomFocusPoint = 0f
    /*
     * Is focus point enabled
     * @return fixed focus point enabled?
     */
    /**
     * Enable zoom focus point
     * If you set this to false the `zoomFocusPoint` won't take effect any more while zooming.
     * The zoom will always be focused at the center of your gesture.
     *
     * @param zoomFocusPointEnabled whether the zoomFocusPoint is enabled
     */
    var isZoomFocusPointEnabled = true
    /**
     * Get scroll duration
     *
     * @return scroll duration
     */
    /**
     * Set the scroll duration
     *
     * @param scrollDuration the new scrollDuraction
     */
    var scrollDuration = 250
    var timeColumnResolution = 60
    var typeface: Typeface? = Typeface.DEFAULT_BOLD
        set(value) {
            if (value != null) {
                field = value
                init()
            }
        }

    private var mMinTime = 0
    private var mMaxTime = 24

    /**
     * auto calculate limit time on events in visible days.
     */
    var autoLimitTime = false
        set(value) {
            field = value
            invalidate()
        }

    var isDropListenerEnabled = false
        set(value) {
            field = value
            setOnDragListener(if (value) DragListener() else null)
        }
    var minOverlappingMinutes = 0
    private var mIsScrollNumberOfVisibleDays = false

    // Listeners.
    var eventClickListener: EventClickListener? = null
    var eventLongPressListener: EventLongPressListener? = null
    /**
     * Get event loader in the week view. Event loaders define the  interval after which the events
     * are loaded in week view. For a MonthLoader events are loaded for every month. You can define
     * your custom event loader by extending WeekViewLoader.
     *
     * @return The event loader.
     */
    /**
     * Set event loader in the week view. For example, a MonthLoader. Event loaders define the
     * interval after which the events are loaded in week view. For a MonthLoader events are loaded
     * for every month. You can define your custom event loader by extending WeekViewLoader.
     *
     * @param loader The event loader.
     */
    var weekViewLoader: WeekViewLoader? = null
    var emptyViewClickListener: EmptyViewClickListener? = null
    var emptyViewLongPressListener: EmptyViewLongPressListener? = null
    private var mDateTimeInterpreter: DateTimeInterpreter? = null
    var scrollListener: ScrollListener? = null
    var addEventClickListener: AddEventClickListener? = null
    var weekViewDropListener: DropListener? = null
    var enableDrawHeaderBackgroundOnlyOnWeekDays = false
        set(value) {
            field = value
            invalidate()
        }
    var sideTitleText: String? = null
        set(value) {
            field = value
            invalidate()
        }
    private val sideTitleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            mStartOriginForScroll = mCurrentOrigin.x
            goToNearestOrigin()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // Check if view is zoomed.
            if (mIsZooming)
                return true

            when (mCurrentScrollDirection) {
                WeekView.Direction.NONE -> {
                    // Allow scrolling only in one direction.
                    mCurrentScrollDirection = if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            Direction.LEFT
                        } else {
                            Direction.RIGHT
                        }
                    } else {
                        Direction.VERTICAL
                    }
                }
                WeekView.Direction.LEFT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX < -mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.RIGHT
                    }
                }
                WeekView.Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX > mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.LEFT
                    }
                }
                else -> {
                }
            }

            // Calculate the new origin after scroll.
            when (mCurrentScrollDirection) {
                WeekView.Direction.LEFT, WeekView.Direction.RIGHT -> {
                    val minX = xMinLimit
                    val maxX = xMaxLimit

                    mDistanceDone = if (e2.x < 0) {
                        e2.x - e1.x
                    } else {
                        e1.x - e2.x
                    }

                    when {
                        mCurrentOrigin.x - distanceX * xScrollingSpeed > maxX -> mCurrentOrigin.x = maxX
                        mCurrentOrigin.x - distanceX * xScrollingSpeed < minX -> mCurrentOrigin.x = minX
                        else -> mCurrentOrigin.x -= distanceX * xScrollingSpeed
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                WeekView.Direction.VERTICAL -> {
                    val minY = yMinLimit
                    val maxY = yMaxLimit
                    when {
                        mCurrentOrigin.y - distanceY > maxY -> mCurrentOrigin.y = maxY
                        mCurrentOrigin.y - distanceY < minY -> mCurrentOrigin.y = minY
                        else -> mCurrentOrigin.y -= distanceY
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                else -> {
                }
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (mIsZooming)
                return true

            if (mCurrentFlingDirection == Direction.LEFT && !isHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.RIGHT && !isHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.VERTICAL && !isVerticalFlingEnabled) {
                return true
            }

            mScroller!!.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                WeekView.Direction.LEFT, WeekView.Direction.RIGHT -> if (!mIsScrollNumberOfVisibleDays) {
                    mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * xScrollingSpeed).toInt(), 0, xMinLimit.toInt(), xMaxLimit.toInt(), yMinLimit.toInt(), yMaxLimit.toInt())
                }
                WeekView.Direction.VERTICAL -> mScroller!!.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), xMinLimit.toInt(), xMaxLimit.toInt(), yMinLimit.toInt(), yMaxLimit.toInt())
                else -> {
                }
            }

            ViewCompat.postInvalidateOnAnimation(this@WeekView)
            return true
        }


        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

            // If the tap was on an event then trigger the callback.
            if (mEventRects != null && eventClickListener != null) {
                val reversedEventRects = mEventRects
                Collections.reverse(reversedEventRects!!)
                for (eventRect in reversedEventRects) {
                    if (newEventIdentifier != eventRect.event.identifier && eventRect.rectF != null && e.x > eventRect.rectF!!.left && e.x < eventRect.rectF!!.right && e.y > eventRect.rectF!!.top && e.y < eventRect.rectF!!.bottom) {
                        eventClickListener!!.onEventClick(eventRect.originalEvent, eventRect.rectF!!)
                        playSoundEffect(SoundEffectConstants.CLICK)
                        return super.onSingleTapConfirmed(e)
                    }
                }
            }

            val xOffset = xStartPixel

            val x = e.x - xOffset
            val y = e.y - mCurrentOrigin.y
            // If the tap was on add new Event space, then trigger the callback
            if (addEventClickListener != null && mNewEventRect != null && mNewEventRect!!.rectF != null &&
                    mNewEventRect!!.rectF!!.contains(x, y)) {
                addEventClickListener!!.onAddEventClicked(mNewEventRect!!.event.startTime!!, mNewEventRect!!.event.endTime!!)
                return super.onSingleTapConfirmed(e)
            }

            // If the tap was on an empty space, then trigger the callback.
            if ((emptyViewClickListener != null || addEventClickListener != null) && e.x > mHeaderColumnWidth && e.y > mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom) {
                val selectedTime = getTimeFromPoint(e.x, e.y)

                if (selectedTime != null) {
                    val tempEvents = ArrayList(mEvents!!)
                    if (mNewEventRect != null) {
                        tempEvents.remove(mNewEventRect!!.event)
                        mNewEventRect = null
                    }

                    playSoundEffect(SoundEffectConstants.CLICK)

                    if (emptyViewClickListener != null)
                        emptyViewClickListener!!.onEmptyViewClicked(selectedTime.clone() as Calendar)

                    if (addEventClickListener != null) {
                        //round selectedTime to resolution
                        selectedTime.add(Calendar.MINUTE, -(newEventLengthInMinutes / 2))
                        //Fix selected time if before the minimum hour
                        if (selectedTime.get(Calendar.HOUR_OF_DAY) < mMinTime) {
                            selectedTime.set(Calendar.HOUR_OF_DAY, mMinTime)
                            selectedTime.set(Calendar.MINUTE, 0)
                        }
                        val unroundedMinutes = selectedTime.get(Calendar.MINUTE)
                        val mod = unroundedMinutes % newEventTimeResolutionInMinutes
                        selectedTime.add(Calendar.MINUTE, if (mod < Math.ceil((newEventTimeResolutionInMinutes / 2).toDouble())) -mod else newEventTimeResolutionInMinutes - mod)

                        val endTime = selectedTime.clone() as Calendar

                        //Minus one to ensure it is the same day and not midnight (next day)
                        val maxMinutes = (mMaxTime - selectedTime.get(Calendar.HOUR_OF_DAY)) * 60 - selectedTime.get(Calendar.MINUTE) - 1
                        endTime.add(Calendar.MINUTE, Math.min(maxMinutes, newEventLengthInMinutes))
                        //If clicked at end of the day, fix selected startTime
                        if (maxMinutes < newEventLengthInMinutes) {
                            selectedTime.add(Calendar.MINUTE, maxMinutes - newEventLengthInMinutes)
                        }

                        val newEvent = WeekViewEvent(newEventIdentifier!!, "", null, selectedTime, endTime)

                        val top = mHourHeight * getPassedMinutesInDay(selectedTime) / 60 + eventsTop
                        val bottom = mHourHeight * getPassedMinutesInDay(endTime) / 60 + eventsTop

                        // Calculate left and right.
                        val left = mWidthPerDay * WeekViewUtil.daysBetween(firstVisibleDay!!, selectedTime)
                        val right = left + mWidthPerDay

                        // Add the new event if its bounds are valid
                        if (left < right &&
                                left < width &&
                                top < height &&
                                right > mHeaderColumnWidth &&
                                bottom > 0) {
                            val dayRectF = RectF(left, top, right, bottom - mCurrentOrigin.y)
                            newEvent.color = mNewEventColor
                            mNewEventRect = EventRect(newEvent, newEvent, dayRectF)
                            tempEvents.add(newEvent)
                            this@WeekView.clearEvents()
                            cacheAndSortEvents(tempEvents)
                            computePositionOfEvents(mEventRects!!)
                            invalidate()
                        }

                    }
                }

            }
            return super.onSingleTapConfirmed(e)
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)

            if (eventLongPressListener != null && mEventRects != null) {
                val reversedEventRects = mEventRects
                Collections.reverse(reversedEventRects!!)
                for (event in reversedEventRects) {
                    if (event.rectF != null && e.x > event.rectF!!.left && e.x < event.rectF!!.right && e.y > event.rectF!!.top && e.y < event.rectF!!.bottom) {
                        eventLongPressListener!!.onEventLongPress(event.originalEvent, event.rectF!!)
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        return
                    }
                }
            }

            // If the tap was on in an empty space, then trigger the callback.
            if (emptyViewLongPressListener != null && e.x > mHeaderColumnWidth && e.y > mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom) {
                val selectedTime = getTimeFromPoint(e.x, e.y)
                if (selectedTime != null) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    emptyViewLongPressListener!!.onEmptyViewLongPress(selectedTime)
                }
            }
        }
    }

    private val numberOfPeriods: Int
        get() = ((mMaxTime - mMinTime) * (60.0 / timeColumnResolution)).toInt()

    private val yMinLimit: Float
        get() = -(((mHourHeight * (mMaxTime - mMinTime)).toFloat()
                + mHeaderHeight
                + (mHeaderRowPadding * 2).toFloat()
                + mHeaderMarginBottom
                + mTimeTextHeight / 2) - height)

    private val yMaxLimit: Float
        get() = 0f

    private val xMinLimit: Float
        get() {
            if (maxDate == null) {
                return Integer.MIN_VALUE.toFloat()
            } else {
                val date = maxDate!!.clone() as Calendar
                date.add(Calendar.DATE, 1 - realNumberOfVisibleDays)
                while (date.before(minDate)) {
                    date.add(Calendar.DATE, 1)
                }

                return getXOriginForDate(date)
            }
        }

    private val xMaxLimit: Float
        get() = if (minDate == null) {
            Integer.MAX_VALUE.toFloat()
        } else {
            getXOriginForDate(minDate!!)
        }

    private val minHourOffset: Int
        get() = mHourHeight * mMinTime

    private// Calculate top.
    val eventsTop: Float
        get() = mCurrentOrigin.y + mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 + mEventMarginVertical.toFloat() - minHourOffset

    private val leftDaysWithGaps: Int
        get() = (-Math.ceil((mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).toDouble())).toInt()

    private val xStartPixel: Float
        get() = mCurrentOrigin.x + (mWidthPerDay + mColumnGap) * leftDaysWithGaps +
                mHeaderColumnWidth

    var monthChangeListener: MonthLoader.MonthChangeListener?
        get() = if (weekViewLoader is MonthLoader) (weekViewLoader as MonthLoader).onMonthChangeListener else null
        set(monthChangeListener) {
            this.weekViewLoader = MonthLoader(monthChangeListener)
        }

    /**
     * Get the interpreter which provides the text to show in the header column and the header row.
     *
     * @return The date, time interpreter.
     */
    /**
     * Set the interpreter which provides the text to show in the header column and the header row.
     *
     * @param dateTimeInterpreter The date, time interpreter.
     */
    // Refresh time column width.
    var dateTimeInterpreter: DateTimeInterpreter
        get() {
            if (mDateTimeInterpreter == null) {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val timeFormat = DateFormat.getTimeFormat(context)
                        ?: SimpleDateFormat("HH:mm", Locale.getDefault())
                val shortDateFormat = WeekViewUtil.getWeekdayWithNumericDayAndMonthFormat(context, true)
                val normalDateFormat = WeekViewUtil.getWeekdayWithNumericDayAndMonthFormat(context, false)
                mDateTimeInterpreter = object : DateTimeInterpreter {
                    override fun interpretTime(hour: Int, minutes: Int): String {
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minutes)
                        return timeFormat.format(calendar.time)
                    }

                    override fun interpretDate(date: Calendar): String {
                        val shortDate = mDayNameLength == LENGTH_SHORT
                        return if (shortDate) shortDateFormat.format(date.time) else normalDateFormat.format(date.time)
                    }
                }
            }
            return mDateTimeInterpreter!!
        }
        set(dateTimeInterpreter) {
            this.mDateTimeInterpreter = dateTimeInterpreter
            initTextTimeWidth()
        }


    /**
     * Get the real number of visible days
     * If the amount of days between max date and min date is smaller, that value is returned
     *
     * @return The real number of visible days
     */
    val realNumberOfVisibleDays: Int
        get() = if (minDate == null || maxDate == null) numberOfVisibleDays else Math.min(mNumberOfVisibleDays, daysBetween(minDate!!, maxDate!!) + 1)

    /**
     * Get the number of visible days
     *
     * @return The set number of visible days.
     */
    /**
     * Set the number of visible days in a week.
     *
     * @param numberOfVisibleDays The number of visible days in a week.
     */
    var numberOfVisibleDays: Int
        get() = mNumberOfVisibleDays
        set(numberOfVisibleDays) {
            this.mNumberOfVisibleDays = numberOfVisibleDays
            resetHomeDate()
            mCurrentOrigin.x = 0f
            mCurrentOrigin.y = 0f
            invalidate()
        }

    var hourHeight: Int
        get() = mHourHeight
        set(hourHeight) {
            mNewHourHeight = hourHeight
            invalidate()
        }

    var columnGap: Int
        get() = mColumnGap
        set(columnGap) {
            mColumnGap = columnGap
            invalidate()
        }

    /**
     * Set the first day of the week. First day of the week is used only when the week view is first
     * drawn. It does not of any effect after user starts scrolling horizontally.
     *
     *
     * **Note:** This method will only work if the week view is set to display more than 6 days at
     * once.
     *
     *
     * @param firstDayOfWeek The supported values are [java.util.Calendar.SUNDAY],
     * [java.util.Calendar.MONDAY], [java.util.Calendar.TUESDAY],
     * [java.util.Calendar.WEDNESDAY], [java.util.Calendar.THURSDAY],
     * [java.util.Calendar.FRIDAY].
     */
    var firstDayOfWeek: Int
        get() = mFirstDayOfWeek
        set(firstDayOfWeek) {
            mFirstDayOfWeek = firstDayOfWeek
            invalidate()
        }

    var textSize: Int
        get() = mTextSize
        set(textSize) {
            mTextSize = textSize
            mTodayHeaderTextPaint.textSize = mTextSize.toFloat()
            mHeaderTextPaint.textSize = mTextSize.toFloat()
            mTimeTextPaint.textSize = mTextSize.toFloat()
            sideTitleTextPaint.textSize = mTextSize.toFloat()
            invalidate()
        }

    var headerColumnPadding: Int
        get() = mHeaderColumnPadding
        set(headerColumnPadding) {
            mHeaderColumnPadding = headerColumnPadding
            invalidate()
        }

    var headerColumnTextColor: Int
        get() = mHeaderColumnTextColor
        set(headerColumnTextColor) {
            mHeaderColumnTextColor = headerColumnTextColor
            mHeaderTextPaint.color = mHeaderColumnTextColor
            mTimeTextPaint.color = mHeaderColumnTextColor
            sideTitleTextPaint.color = mHeaderColumnTextColor
            invalidate()
        }

    var headerRowPadding: Int
        get() = mHeaderRowPadding
        set(headerRowPadding) {
            mHeaderRowPadding = headerRowPadding
            invalidate()
        }

    var headerRowBackgroundColor: Int
        get() = mHeaderRowBackgroundColor
        set(headerRowBackgroundColor) {
            mHeaderRowBackgroundColor = headerRowBackgroundColor
            mHeaderBackgroundPaint.color = mHeaderRowBackgroundColor
            invalidate()
        }

    var dayBackgroundColor: Int
        get() = mDayBackgroundColor
        set(dayBackgroundColor) {
            mDayBackgroundColor = dayBackgroundColor
            mDayBackgroundPaint.color = mDayBackgroundColor
            invalidate()
        }

    var hourSeparatorColor: Int
        get() = mHourSeparatorColor
        set(hourSeparatorColor) {
            mHourSeparatorColor = hourSeparatorColor
            mHourSeparatorPaint.color = mHourSeparatorColor
            invalidate()
        }

    var todayBackgroundColor: Int
        get() = mTodayBackgroundColor
        set(todayBackgroundColor) {
            mTodayBackgroundColor = todayBackgroundColor
            mTodayBackgroundPaint.color = mTodayBackgroundColor
            invalidate()
        }

    var hourSeparatorHeight: Int
        get() = mHourSeparatorHeight
        set(hourSeparatorHeight) {
            mHourSeparatorHeight = hourSeparatorHeight
            mHourSeparatorPaint.strokeWidth = mHourSeparatorHeight.toFloat()
            invalidate()
        }

    var todayHeaderTextColor: Int
        get() = mTodayHeaderTextColor
        set(todayHeaderTextColor) {
            mTodayHeaderTextColor = todayHeaderTextColor
            mTodayHeaderTextPaint.color = mTodayHeaderTextColor
            invalidate()
        }

    var eventTextSize: Int
        get() = mEventTextSize
        set(eventTextSize) {
            mEventTextSize = eventTextSize
            mEventTextPaint.textSize = mEventTextSize.toFloat()
            invalidate()
        }

    var eventTextColor: Int
        get() = mEventTextColor
        set(eventTextColor) {
            mEventTextColor = eventTextColor
            mEventTextPaint.color = mEventTextColor
            invalidate()
        }

    var eventPadding: Int
        get() = mEventPadding
        set(eventPadding) {
            mEventPadding = eventPadding
            invalidate()
        }

    var headerColumnBackgroundColor: Int
        get() = mHeaderColumnBackgroundColor
        set(headerColumnBackgroundColor) {
            mHeaderColumnBackgroundColor = headerColumnBackgroundColor
            mHeaderColumnBackgroundPaint.color = mHeaderColumnBackgroundColor
            invalidate()
        }

    var defaultEventColor: Int
        get() = mDefaultEventColor
        set(defaultEventColor) {
            mDefaultEventColor = defaultEventColor
            invalidate()
        }

    var newEventColor: Int
        get() = mNewEventColor
        set(defaultNewEventColor) {
            mNewEventColor = defaultNewEventColor
            invalidate()
        }

    var newEventId: Int
        @Deprecated("")
        get() = Integer.parseInt(newEventIdentifier)
        @Deprecated("")
        set(newEventId) {
            this.newEventIdentifier = newEventId.toString()
        }

    /**
     * **Note:** Use [.setDateTimeInterpreter] and
     * [.getDateTimeInterpreter] instead.
     *
     * @return Either long or short day name is being used.
     */
    /**
     * Set the length of the day name displayed in the header row. Example of short day names is
     * 'M' for 'Monday' and example of long day names is 'Mon' for 'Monday'.
     *
     *
     * **Note:** Use [.setDateTimeInterpreter] instead.
     *
     *
     * @param length Supported values are [com.alamkanak.weekview.WeekView.LENGTH_SHORT] and
     * [com.alamkanak.weekview.WeekView.LENGTH_LONG].
     */
    var dayNameLength: Int
        @Deprecated("")
        get() = mDayNameLength
        @Deprecated("")
        set(length) {
            if (length != LENGTH_LONG && length != LENGTH_SHORT) {
                throw IllegalArgumentException("length parameter must be either LENGTH_LONG or LENGTH_SHORT")
            }
            this.mDayNameLength = length
        }

    /**
     * Set the gap between overlapping events.
     *
     * @param overlappingEventGap The gap between overlapping events.
     */
    var overlappingEventGap: Int
        get() = mOverlappingEventGap
        set(overlappingEventGap) {
            this.mOverlappingEventGap = overlappingEventGap
            invalidate()
        }

    /**
     * Set the top and bottom margin of the event. The event will release this margin from the top
     * and bottom edge. This margin is useful for differentiation consecutive events.
     *
     * @param eventMarginVertical The top and bottom margin.
     */
    var eventMarginVertical: Int
        get() = mEventMarginVertical
        set(eventMarginVertical) {
            this.mEventMarginVertical = eventMarginVertical
            invalidate()
        }

    /**
     * Whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     *
     * @return True if weekends should have different background colors.
     */
    /**
     * Set whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     *
     * @param showDistinctWeekendColor True if weekends should have different background colors.
     */
    var isShowDistinctWeekendColor: Boolean
        get() = mShowDistinctWeekendColor
        set(showDistinctWeekendColor) {
            this.mShowDistinctWeekendColor = showDistinctWeekendColor
            invalidate()
        }

    /**
     * Whether past and future days should have two different background colors. The past and
     * future day colors are defined by the attributes `futureBackgroundColor` and
     * `pastBackgroundColor`.
     *
     * @return True if past and future days should have two different background colors.
     */
    /**
     * Set whether weekends should have a background color different from the normal day background
     * color. The past and future day colors are defined by the attributes `futureBackgroundColor`
     * and `pastBackgroundColor`.
     *
     * @param showDistinctPastFutureColor True if past and future should have two different
     * background colors.
     */
    var isShowDistinctPastFutureColor: Boolean
        get() = mShowDistinctPastFutureColor
        set(showDistinctPastFutureColor) {
            this.mShowDistinctPastFutureColor = showDistinctPastFutureColor
            invalidate()
        }

    /**
     * Get whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     *
     * @return True if "now" line should be displayed.
     */
    /**
     * Set whether "now" line should be displayed. "Now" line is defined by the attributes
     * `nowLineColor` and `nowLineThickness`.
     *
     * @param showNowLine True if "now" line should be displayed.
     */
    var isShowNowLine: Boolean
        get() = mShowNowLine
        set(showNowLine) {
            this.mShowNowLine = showNowLine
            invalidate()
        }

    /**
     * Get the "now" line color.
     *
     * @return The color of the "now" line.
     */
    /**
     * Set the "now" line color.
     *
     * @param nowLineColor The color of the "now" line.
     */
    var nowLineColor: Int
        get() = mNowLineColor
        set(nowLineColor) {
            this.mNowLineColor = nowLineColor
            invalidate()
        }

    /**
     * Get the "now" line thickness.
     *
     * @return The thickness of the "now" line.
     */
    /**
     * Set the "now" line thickness.
     *
     * @param nowLineThickness The thickness of the "now" line.
     */
    var nowLineThickness: Int
        get() = mNowLineThickness
        set(nowLineThickness) {
            this.mNowLineThickness = nowLineThickness
            invalidate()
        }

    /*
     * Get focus point
     * 0 = top of view, 1 = bottom of view
     * The focused point (multiplier of the view height) where the week view is zoomed around.
     * This point will not move while zooming.
     * @return focus point
     */
    /**
     * Set focus point
     * 0 = top of view, 1 = bottom of view
     * The focused point (multiplier of the view height) where the week view is zoomed around.
     * This point will not move while zooming.
     *
     * @param zoomFocusPoint the new zoomFocusPoint
     */
    var zoomFocusPoint: Float
        get() = mZoomFocusPoint
        set(zoomFocusPoint) {
            if (0 > zoomFocusPoint || zoomFocusPoint > 1)
                throw IllegalStateException("The zoom focus point percentage has to be between 0 and 1")
            mZoomFocusPoint = zoomFocusPoint
        }

    var pastBackgroundColor: Int
        get() = mPastBackgroundColor
        set(pastBackgroundColor) {
            this.mPastBackgroundColor = pastBackgroundColor
            mPastBackgroundPaint.color = mPastBackgroundColor
        }

    var futureBackgroundColor: Int
        get() = mFutureBackgroundColor
        set(futureBackgroundColor) {
            this.mFutureBackgroundColor = futureBackgroundColor
            mFutureBackgroundPaint.color = mFutureBackgroundColor
        }

    var pastWeekendBackgroundColor: Int
        get() = mPastWeekendBackgroundColor
        set(pastWeekendBackgroundColor) {
            this.mPastWeekendBackgroundColor = pastWeekendBackgroundColor
            this.mPastWeekendBackgroundPaint.color = mPastWeekendBackgroundColor
        }

    var futureWeekendBackgroundColor: Int
        get() = mFutureWeekendBackgroundColor
        set(futureWeekendBackgroundColor) {
            this.mFutureWeekendBackgroundColor = futureWeekendBackgroundColor
            this.mFutureWeekendBackgroundPaint.color = mFutureWeekendBackgroundColor
        }

    var isScrollNumberOfVisibleDays: Boolean
        get() = this.mIsScrollNumberOfVisibleDays
        set(scrollNumberOfVisibleDays) {
            this.mIsScrollNumberOfVisibleDays = scrollNumberOfVisibleDays
            invalidate()
        }

    /**
     * Get the first hour that is visible on the screen.
     *
     * @return The first hour that is visible.
     */
    val firstVisibleHour: Double
        get() = (-mCurrentOrigin.y / mHourHeight).toDouble()

    private val refreshRunnable: Runnable
    private val uiHandler = Handler()

    //endregion fields and properties

    private enum class Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    init {
        refreshRunnable = object : Runnable {
            override fun run() {
                invalidate()
                uiHandler.postDelayed(this, 60L * 1000L)
            }
        }
        uiHandler.postDelayed(refreshRunnable, 60L * 1000L)
        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek)
            mHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, mHourHeight)
            minHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_minHourHeight, minHourHeight)
            mEffectiveMinHourHeight = minHourHeight
            maxHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_maxHourHeight, maxHourHeight)
            mTextSize = a.getDimensionPixelSize(R.styleable.WeekView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mHeaderColumnPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding)
            mColumnGap = a.getDimensionPixelSize(R.styleable.WeekView_columnGap, mColumnGap)
            mHeaderColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor)
            mNumberOfVisibleDays = a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays)
            isShowFirstDayOfWeekFirst = a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, isShowFirstDayOfWeekFirst)
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, mHeaderRowPadding)
            mHeaderRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor)
            mDayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
            mFutureBackgroundColor = a.getColor(R.styleable.WeekView_futureBackgroundColor, mFutureBackgroundColor)
            mPastBackgroundColor = a.getColor(R.styleable.WeekView_pastBackgroundColor, mPastBackgroundColor)
            mFutureWeekendBackgroundColor = a.getColor(R.styleable.WeekView_futureWeekendBackgroundColor, mFutureBackgroundColor) // If not set, use the same color as in the week
            mPastWeekendBackgroundColor = a.getColor(R.styleable.WeekView_pastWeekendBackgroundColor, mPastBackgroundColor)
            mNowLineColor = a.getColor(R.styleable.WeekView_nowLineColor, mNowLineColor)
            mNowLineThickness = a.getDimensionPixelSize(R.styleable.WeekView_nowLineThickness, mNowLineThickness)
            mHourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor)
            mTodayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor)
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight)
            mTodayHeaderTextColor = a.getColor(R.styleable.WeekView_todayHeaderTextColor, mTodayHeaderTextColor)
            mEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor)
            mNewEventColor = a.getColor(R.styleable.WeekView_newEventColor, mNewEventColor)
            newEventIconDrawable = a.getDrawable(R.styleable.WeekView_newEventIconResource)
            // For backward compatibility : Set "mNewEventIdentifier" if the attribute is "WeekView_newEventId" of type int
            newEventId = a.getInt(R.styleable.WeekView_newEventId, Integer.parseInt(newEventIdentifier))
            newEventIdentifier = if (a.getString(R.styleable.WeekView_newEventIdentifier) != null) a.getString(R.styleable.WeekView_newEventIdentifier) else newEventIdentifier
            newEventLengthInMinutes = a.getInt(R.styleable.WeekView_newEventLengthInMinutes, newEventLengthInMinutes)
            newEventTimeResolutionInMinutes = a.getInt(R.styleable.WeekView_newEventTimeResolutionInMinutes, newEventTimeResolutionInMinutes)
            mEventPadding = a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, mEventPadding)
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor)
            mDayNameLength = a.getInteger(R.styleable.WeekView_dayNameLength, mDayNameLength)
            mOverlappingEventGap = a.getDimensionPixelSize(R.styleable.WeekView_overlappingEventGap, mOverlappingEventGap)
            mEventMarginVertical = a.getDimensionPixelSize(R.styleable.WeekView_eventMarginVertical, mEventMarginVertical)
            xScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, xScrollingSpeed)
            eventCornerRadius = a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, eventCornerRadius)
            mShowDistinctPastFutureColor = a.getBoolean(R.styleable.WeekView_showDistinctPastFutureColor, mShowDistinctPastFutureColor)
            mShowDistinctWeekendColor = a.getBoolean(R.styleable.WeekView_showDistinctWeekendColor, mShowDistinctWeekendColor)
            mShowNowLine = a.getBoolean(R.styleable.WeekView_showNowLine, mShowNowLine)
            isHorizontalFlingEnabled = a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, isHorizontalFlingEnabled)
            isVerticalFlingEnabled = a.getBoolean(R.styleable.WeekView_verticalFlingEnabled, isVerticalFlingEnabled)
            allDayEventHeight = a.getDimensionPixelSize(R.styleable.WeekView_allDayEventHeight, allDayEventHeight)
            mZoomFocusPoint = a.getFraction(R.styleable.WeekView_zoomFocusPoint, 1, 1, mZoomFocusPoint)
            isZoomFocusPointEnabled = a.getBoolean(R.styleable.WeekView_zoomFocusPointEnabled, isZoomFocusPointEnabled)
            scrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, scrollDuration)
            timeColumnResolution = a.getInt(R.styleable.WeekView_timeColumnResolution, timeColumnResolution)
            autoLimitTime = a.getBoolean(R.styleable.WeekView_autoLimitTime, autoLimitTime)
            mMinTime = a.getInt(R.styleable.WeekView_minTime, mMinTime)
            mMaxTime = a.getInt(R.styleable.WeekView_maxTime, mMaxTime)
            if (a.getBoolean(R.styleable.WeekView_dropListenerEnabled, false))
                this.isDropListenerEnabled = true
            minOverlappingMinutes = a.getInt(R.styleable.WeekView_minOverlappingMinutes, 0)
            mIsScrollNumberOfVisibleDays = a.getBoolean(R.styleable.WeekView_isScrollNumberOfVisibleDays, false)
            enableDrawHeaderBackgroundOnlyOnWeekDays = a.getBoolean(R.styleable.WeekView_enableDrawHeaderBackgroundOnlyOnWeekDays, false)
        } finally {
            a.recycle()
        }

        init()
    }

    private fun init() {
        resetHomeDate()

        // Scrolling initialization.
        mGestureDetector = GestureDetectorCompat(context, mGestureListener)
        mScroller = OverScroller(context, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        // Measure settings for time column.
        mTimeTextPaint.textAlign = Paint.Align.RIGHT
        mTimeTextPaint.textSize = mTextSize.toFloat()
        mTimeTextPaint.color = mHeaderColumnTextColor
        mTimeTextPaint.typeface = typeface

        val rect = Rect()
        val exampleTime = if (timeColumnResolution % 60 != 0) "00:00 PM" else "00 PM"
        mTimeTextPaint.getTextBounds(exampleTime, 0, exampleTime.length, rect)
        mTimeTextWidth = mTimeTextPaint.measureText(exampleTime)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = mTimeTextHeight / 2
        initTextTimeWidth()

        //handle sideTitleTextPaint
        sideTitleTextPaint.textAlign = Paint.Align.CENTER
        sideTitleTextPaint.textSize = mTextSize.toFloat()
        sideTitleTextPaint.color = mHeaderColumnTextColor
        sideTitleTextPaint.typeface = typeface

        // Measure settings for header row.
        mHeaderTextPaint.color = mHeaderColumnTextColor
        mHeaderTextPaint.textAlign = Paint.Align.CENTER
        mHeaderTextPaint.textSize = mTextSize.toFloat()
        mHeaderTextPaint.getTextBounds(exampleTime, 0, exampleTime.length, rect)
        mHeaderTextHeight = rect.height().toFloat()
        mHeaderTextPaint.typeface = typeface


        // Prepare header background paint.
        mHeaderBackgroundPaint.color = mHeaderRowBackgroundColor

        // Prepare day background color paint.
        mDayBackgroundPaint.color = mDayBackgroundColor
        mFutureBackgroundPaint.color = mFutureBackgroundColor
        mPastBackgroundPaint.color = mPastBackgroundColor
        mFutureWeekendBackgroundPaint.color = mFutureWeekendBackgroundColor
        mPastWeekendBackgroundPaint.color = mPastWeekendBackgroundColor

        // Prepare hour separator color paint.
        mHourSeparatorPaint.style = Paint.Style.STROKE
        mHourSeparatorPaint.strokeWidth = mHourSeparatorHeight.toFloat()
        mHourSeparatorPaint.color = mHourSeparatorColor

        // Prepare the "now" line color paint
        mNowLinePaint.strokeWidth = mNowLineThickness.toFloat()
        mNowLinePaint.color = mNowLineColor

        // Prepare today background color paint.
        mTodayBackgroundPaint.color = mTodayBackgroundColor

        // Prepare today header text color paint.
        mTodayHeaderTextPaint.textAlign = Paint.Align.CENTER
        mTodayHeaderTextPaint.textSize = mTextSize.toFloat()
        mTodayHeaderTextPaint.typeface = typeface
        mTodayHeaderTextPaint.color = mTodayHeaderTextColor

        // Prepare event background color.
        mEventBackgroundPaint.color = Color.rgb(174, 208, 238)
        // Prepare empty event background color.
        mNewEventBackgroundPaint.color = Color.rgb(60, 147, 217)

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint.color = mHeaderColumnBackgroundColor

        // Prepare event text size and color.
        mEventTextPaint.style = Paint.Style.FILL
        mEventTextPaint.color = mEventTextColor
        mEventTextPaint.textSize = mEventTextSize.toFloat()
        mEventTextPaint.typeface = typeface


        // Set default event color.
        mDefaultEventColor = Color.parseColor("#9fc6e7")
        // Set default empty event color.
        mNewEventColor = Color.parseColor("#3c93d9")

        mScaleDetector = ScaleGestureDetector(context, WeekViewGestureListener())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        uiHandler.removeCallbacks(refreshRunnable)
    }

    private fun resetHomeDate() {
        var newHomeDate = today()

        if (minDate != null && newHomeDate.before(minDate)) {
            newHomeDate = minDate!!.clone() as Calendar
        }
        if (maxDate != null && newHomeDate.after(maxDate)) {
            newHomeDate = maxDate!!.clone() as Calendar
        }

        if (maxDate != null) {
            val date = maxDate!!.clone() as Calendar
            date.add(Calendar.DATE, 1 - realNumberOfVisibleDays)
            while (date.before(minDate)) {
                date.add(Calendar.DATE, 1)
            }

            if (newHomeDate.after(date)) {
                newHomeDate = date
            }
        }

        mHomeDate = newHomeDate
    }

    private fun getXOriginForDate(date: Calendar): Float {
        return -daysBetween(mHomeDate!!, date) * (mWidthPerDay + mColumnGap)
    }

    // fix rotation changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        mTimeTextWidth = 0f
        for (i in 0 until numberOfPeriods) {
            // Measure time string and get max width.
            val time = dateTimeInterpreter.interpretTime(i, i % 2 * 30)
            mTimeTextWidth = Math.max(mTimeTextWidth, mTimeTextPaint.measureText(time))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the header row.
        drawHeaderRowAndEvents(canvas)

        // Draw the time column and all the axes/separators.
        drawTimeColumnAndAxes(canvas)
    }

    private fun calculateHeaderHeight() {
        //Make sure the header is the right size (depends on AllDay events)
        var containsAllDayEvent = false
        if (mEventRects != null && mEventRects!!.size > 0) {
            for (dayNumber in 0 until realNumberOfVisibleDays) {
                val day = firstVisibleDay!!.clone() as Calendar
                day.add(Calendar.DATE, dayNumber)
                for (i in mEventRects!!.indices) {

                    if (isSameDay(mEventRects!![i].event.startTime!!, day) && mEventRects!![i].event.isAllDay) {
                        containsAllDayEvent = true
                        break
                    }
                }
                if (containsAllDayEvent) {
                    break
                }
            }
        }
        mHeaderHeight = if (containsAllDayEvent) {
            mHeaderTextHeight + (allDayEventHeight + mHeaderMarginBottom)
        } else {
            mHeaderTextHeight
        }
    }

    private fun drawTimeColumnAndAxes(canvas: Canvas) {
        // Draw the background color for the header column.
        canvas.drawRect(0f, mHeaderHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, height.toFloat(), mHeaderColumnBackgroundPaint)

        // Clip to paint in left column only.
        canvas.clipRect(0f, mHeaderHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, height.toFloat(), Region.Op.REPLACE)

        for (i in 0 until numberOfPeriods) {
            // If we are showing half hours (eg. 5:30am), space the times out by half the hour height
            // and need to provide 30 minutes on each odd period, otherwise, minutes is always 0.
            val timeSpacing: Float
            val minutes: Int
            val hour: Int

            val timesPerHour = 60.0f / timeColumnResolution

            timeSpacing = mHourHeight / timesPerHour
            hour = mMinTime + i / timesPerHour.toInt()
            minutes = i % timesPerHour.toInt() * (60 / timesPerHour.toInt())


            // Calculate the top of the rectangle where the time text will go
            val top = mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mCurrentOrigin.y + timeSpacing * i + mHeaderMarginBottom

            // Get the time to be displayed, as a String.
            val time = dateTimeInterpreter.interpretTime(hour, minutes)
            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            if (top < height)
                canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint)
        }
    }

    private fun drawHeaderRowAndEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2
        mWidthPerDay = width.toFloat() - mHeaderColumnWidth - (mColumnGap * (realNumberOfVisibleDays - 1)).toFloat()
        mWidthPerDay /= realNumberOfVisibleDays

        calculateHeaderHeight() //Make sure the header is the right size (depends on AllDay events)

        val today = today()

        if (mAreDimensionsInvalid) {
            mEffectiveMinHourHeight = Math.max(minHourHeight, ((height.toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom) / (mMaxTime - mMinTime)).toInt())

            mAreDimensionsInvalid = false
            if (mScrollToDay != null)
                goToDate(mScrollToDay!!)

            mAreDimensionsInvalid = false
            if (mScrollToHour >= 0)
                goToHour(mScrollToHour)

            mScrollToDay = null
            mScrollToHour = -1.0
            mAreDimensionsInvalid = false
        }
        if (mIsFirstDraw) {
            mIsFirstDraw = false

            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (realNumberOfVisibleDays >= 7 && mHomeDate!!.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek && isShowFirstDayOfWeekFirst) {
                val difference = mHomeDate!!.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek
                mCurrentOrigin.x += (mWidthPerDay + mColumnGap) * difference
            }
            setLimitTime(mMinTime, mMaxTime)
        }

        // Calculate the new height due to the zooming.
        if (mNewHourHeight > 0) {
            if (mNewHourHeight < mEffectiveMinHourHeight)
                mNewHourHeight = mEffectiveMinHourHeight
            else if (mNewHourHeight > maxHourHeight)
                mNewHourHeight = maxHourHeight

            mHourHeight = mNewHourHeight
            mNewHourHeight = -1
        }

        // If the new mCurrentOrigin.y is invalid, make it valid.
        if (mCurrentOrigin.y < height.toFloat() - (mHourHeight * (mMaxTime - mMinTime)).toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2)
            mCurrentOrigin.y = height.toFloat() - (mHourHeight * (mMaxTime - mMinTime)).toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2

        // Don't put an "else if" because it will trigger a glitch when completely zoomed out and
        // scrolling vertically.
        if (mCurrentOrigin.y > 0) {
            mCurrentOrigin.y = 0f
        }

        val leftDaysWithGaps = leftDaysWithGaps
        // Consider scroll offset.
        val startFromPixel = xStartPixel
        var startPixel = startFromPixel

        // Prepare to iterate for each day.
        var day = today.clone() as Calendar
        day.add(Calendar.HOUR_OF_DAY, 6)

        // Prepare to iterate for each hour to draw the hour lines.
        var lineCount = ((height.toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() -
                mHeaderMarginBottom) / mHourHeight).toInt() + 1

        lineCount *= (realNumberOfVisibleDays + 1)

        val hourLines = FloatArray(lineCount * 4)

        // Clear the cache for event rectangles.
        if (mEventRects != null) {
            for (eventRect in mEventRects!!) {
                eventRect.rectF = null
            }
        }

        // Clip to paint events only.
        canvas.clipRect(mHeaderColumnWidth, mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2, width.toFloat(), height.toFloat(), Region.Op.REPLACE)

        // Iterate through each day.
        val oldFirstVisibleDay = firstVisibleDay
        firstVisibleDay = mHomeDate!!.clone() as Calendar
        firstVisibleDay!!.add(Calendar.DATE, -Math.round(mCurrentOrigin.x / (mWidthPerDay + mColumnGap)))
        if (firstVisibleDay != oldFirstVisibleDay && scrollListener != null) {
            scrollListener!!.onFirstVisibleDayChanged(firstVisibleDay!!, oldFirstVisibleDay)
        }

        if (autoLimitTime) {
            val days = ArrayList<Calendar>()
            for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays) {
                day = mHomeDate!!.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                days.add(day)
            }
            limitEventTime(days)
        }

        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day = mHomeDate!!.clone() as Calendar
            lastVisibleDay = day.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            lastVisibleDay!!.add(Calendar.DATE, dayNumber - 2)
            val isToday = isSameDay(day, today)

            // Don't draw days which are outside requested range
            if (!dateIsValid(day)) {
                continue
            }

            // Get more events if necessary. We want to store the events 3 months beforehand. Get
            // events only when it is the first iteration of the loop.
            if (mEventRects == null || mRefreshEvents ||
                    dayNumber == leftDaysWithGaps + 1 && mFetchedPeriod != weekViewLoader!!.toWeekViewPeriodIndex(day).toInt() &&
                    Math.abs(mFetchedPeriod - weekViewLoader!!.toWeekViewPeriodIndex(day)) > 0.5) {
                getMoreEvents(day)
                mRefreshEvents = false
            }

            // Draw background color for each day.
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0) {
                if (mShowDistinctPastFutureColor) {
                    val isWeekend = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    val pastPaint = if (isWeekend && mShowDistinctWeekendColor) mPastWeekendBackgroundPaint else mPastBackgroundPaint
                    val futurePaint = if (isWeekend && mShowDistinctWeekendColor) mFutureWeekendBackgroundPaint else mFutureBackgroundPaint
                    val startY = mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom + mCurrentOrigin.y
                    when {
                        isToday -> {
                            val now = Calendar.getInstance()
                            val beforeNow = (now.get(Calendar.HOUR_OF_DAY) - mMinTime + now.get(Calendar.MINUTE) / 60f) * mHourHeight
                            canvas.drawRect(start, startY, startPixel + mWidthPerDay, startY + beforeNow, pastPaint)
                            canvas.drawRect(start, startY + beforeNow, startPixel + mWidthPerDay, height.toFloat(), futurePaint)
                        }
                        day.before(today) -> canvas.drawRect(start, startY, startPixel + mWidthPerDay, height.toFloat(), pastPaint)
                        else -> canvas.drawRect(start, startY, startPixel + mWidthPerDay, height.toFloat(), futurePaint)
                    }
                } else {
                    canvas.drawRect(start, mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel + mWidthPerDay, height.toFloat(), if (isToday) mTodayBackgroundPaint else mDayBackgroundPaint)
                }
            }

            // Prepare the separator lines for hours.
            var i = 0
            for (hourNumber in mMinTime until mMaxTime) {
                val top = mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mCurrentOrigin.y + (mHourHeight * (hourNumber - mMinTime)).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom
                if (top > mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom - mHourSeparatorHeight && top < height && startPixel + mWidthPerDay - start > 0) {
                    hourLines[i * 4] = start
                    hourLines[i * 4 + 1] = top
                    hourLines[i * 4 + 2] = startPixel + mWidthPerDay
                    hourLines[i * 4 + 3] = top
                    i++
                }
            }

            // Draw the lines for hours.
            canvas.drawLines(hourLines, mHourSeparatorPaint)

            // Draw the events.
            drawEvents(day, startPixel, canvas)

            // Draw the line at the current time.
            if (mShowNowLine && isToday) {
                val startY = mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom + mCurrentOrigin.y
                val now = Calendar.getInstance()
                val beforeNow = (now.get(Calendar.HOUR_OF_DAY) - mMinTime + now.get(Calendar.MINUTE) / 60f) * mHourHeight
                val top = startY + beforeNow
                canvas.drawLine(start, top, startPixel + mWidthPerDay, top, mNowLinePaint)
            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay + mColumnGap
        }

        // Hide everything in the first cell (top left corner).

        canvas.clipRect(0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderHeight + mHeaderRowPadding * 2, Region.Op.REPLACE)
        if (enableDrawHeaderBackgroundOnlyOnWeekDays)
            canvas.drawRect(0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint)
        else
            canvas.drawRect(0f, 0f, mTimeTextWidth + mHeaderColumnPadding * 2, mHeaderHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint)


        // draw text on the left of the week days
        if (!TextUtils.isEmpty(sideTitleText))
            canvas.drawText(sideTitleText, mHeaderColumnWidth / 2, mHeaderTextHeight + mHeaderRowPadding, sideTitleTextPaint)

        // Clip to paint header row only.
        canvas.clipRect(mHeaderColumnWidth, 0f, width.toFloat(), mHeaderHeight + mHeaderRowPadding * 2, Region.Op.REPLACE)

        // Draw the header background.
        if (enableDrawHeaderBackgroundOnlyOnWeekDays)
            canvas.drawRect(0f, 0f, width.toFloat(), mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint)
        else
            canvas.drawRect(0f, 0f, width.toFloat(), mHeaderHeight + mHeaderRowPadding * 2, mHeaderBackgroundPaint)

        // Draw the header row texts.
        startPixel = startFromPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day = mHomeDate!!.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            val isToday = isSameDay(day, today)

            // Don't draw days which are outside requested range
            if (!dateIsValid(day))
                continue

            // Draw the day labels.
            val dayLabel = dateTimeInterpreter.interpretDate(day)
            canvas.drawText(dayLabel, startPixel + mWidthPerDay / 2, mHeaderTextHeight + mHeaderRowPadding, if (isToday) mTodayHeaderTextPaint else mHeaderTextPaint)
            drawAllDayEvents(day, startPixel, canvas)
            startPixel += mWidthPerDay + mColumnGap
        }

    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeFromPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = leftDaysWithGaps
        var startPixel = xStartPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + realNumberOfVisibleDays + 1) {
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0 && x > start && x < startPixel + mWidthPerDay) {
                val day = mHomeDate!!.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                val pixelsFromZero = (y - mCurrentOrigin.y - mHeaderHeight
                        - (mHeaderRowPadding * 2).toFloat() - mTimeTextHeight / 2 - mHeaderMarginBottom)
                val hour = (pixelsFromZero / mHourHeight).toInt()
                val minute = (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight).toInt()
                day.add(Calendar.HOUR_OF_DAY, hour + mMinTime)
                day.set(Calendar.MINUTE, minute)
                return day
            }
            startPixel += mWidthPerDay + mColumnGap
        }
        return null
    }

    /**
     * limit current time of event by update mMinTime & mMaxTime
     * find smallest of start time & latest of end time
     */
    private fun limitEventTime(dates: MutableList<Calendar>) {
        if (mEventRects != null && mEventRects!!.size > 0) {
            var startTime: Calendar? = null
            var endTime: Calendar? = null

            for (eventRect in mEventRects!!) {
                for (date in dates) {
                    if (isSameDay(eventRect.event.startTime!!, date) && !eventRect.event.isAllDay) {

                        if (startTime == null || getPassedMinutesInDay(startTime) > getPassedMinutesInDay(eventRect.event.startTime!!)) {
                            startTime = eventRect.event.startTime
                        }

                        if (endTime == null || getPassedMinutesInDay(endTime) < getPassedMinutesInDay(eventRect.event.endTime!!)) {
                            endTime = eventRect.event.endTime
                        }
                    }
                }
            }

            if (startTime != null && endTime != null && startTime.before(endTime)) {
                setLimitTime(Math.max(0, startTime.get(Calendar.HOUR_OF_DAY)),
                        Math.min(24, endTime.get(Calendar.HOUR_OF_DAY) + 1))
                return
            }
        }
    }

    /**
     * Draw all the events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     */
    private fun drawEvents(date: Calendar, startFromPixel: Float, canvas: Canvas) {
        if (mEventRects != null && mEventRects!!.size > 0) {
            for (i in mEventRects!!.indices) {
                if (isSameDay(mEventRects!![i].event.startTime!!, date) && !mEventRects!![i].event.isAllDay) {
                    val top = mHourHeight * mEventRects!![i].top / 60 + eventsTop
                    val bottom = mHourHeight * mEventRects!![i].bottom / 60 + eventsTop

                    // Calculate left and right.
                    var left = startFromPixel + mEventRects!![i].left * mWidthPerDay
                    if (left < startFromPixel)
                        left += mOverlappingEventGap.toFloat()
                    var right = left + mEventRects!![i].width * mWidthPerDay
                    if (right < startFromPixel + mWidthPerDay)
                        right -= mOverlappingEventGap.toFloat()

                    // Draw the event and the event name on top of it.
                    if (left < right &&
                            left < width &&
                            top < height &&
                            right > mHeaderColumnWidth &&
                            bottom > mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom) {
                        mEventRects!![i].rectF = RectF(left, top, right, bottom)
                        mEventBackgroundPaint.color = if (mEventRects!![i].event.color == 0) mDefaultEventColor else mEventRects!![i].event.color
                        mEventBackgroundPaint.shader = mEventRects!![i].event.shader
                        canvas.drawRoundRect(mEventRects!![i].rectF!!, eventCornerRadius.toFloat(), eventCornerRadius.toFloat(), mEventBackgroundPaint)
                        var topToUse = top
                        if (mEventRects!![i].event.startTime!!.get(Calendar.HOUR_OF_DAY) < mMinTime)
                            topToUse = mHourHeight * getPassedMinutesInDay(mMinTime, 0) / 60 + eventsTop

                        if (newEventIdentifier != mEventRects!![i].event.identifier)
                            drawEventTitle(mEventRects!![i].event, mEventRects!![i].rectF!!, canvas, topToUse, left)
                        else
                            drawEmptyImage(mEventRects!![i].event, mEventRects!![i].rectF!!, canvas, topToUse, left)

                    } else
                        mEventRects!![i].rectF = null
                }
            }
        }
    }

    /**
     * Draw all the Allday-events of a particular day.
     *
     * @param date           The day.
     * @param startFromPixel The left position of the day area. The events will never go any left from this value.
     * @param canvas         The canvas to draw upon.
     */
    private fun drawAllDayEvents(date: Calendar, startFromPixel: Float, canvas: Canvas) {
        if (mEventRects != null && mEventRects!!.size > 0) {
            for (i in mEventRects!!.indices) {
                if (isSameDay(mEventRects!![i].event.startTime!!, date) && mEventRects!![i].event.isAllDay) {

                    // Calculate top.
                    val top = (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom + +mTimeTextHeight / 2 + mEventMarginVertical.toFloat()

                    // Calculate bottom.
                    val bottom = top + mEventRects!![i].bottom

                    // Calculate left and right.
                    var left = startFromPixel + mEventRects!![i].left * mWidthPerDay
                    if (left < startFromPixel)
                        left += mOverlappingEventGap.toFloat()
                    var right = left + mEventRects!![i].width * mWidthPerDay
                    if (right < startFromPixel + mWidthPerDay)
                        right -= mOverlappingEventGap.toFloat()

                    // Draw the event and the event name on top of it.
                    if (left < right &&
                            left < width &&
                            top < height &&
                            right > mHeaderColumnWidth &&
                            bottom > 0) {
                        mEventRects!![i].rectF = RectF(left, top, right, bottom)
                        mEventBackgroundPaint.color = if (mEventRects!![i].event.color == 0) mDefaultEventColor else mEventRects!![i].event.color
                        mEventBackgroundPaint.shader = mEventRects!![i].event.shader
                        canvas.drawRoundRect(mEventRects!![i].rectF!!, eventCornerRadius.toFloat(), eventCornerRadius.toFloat(), mEventBackgroundPaint)
                        drawEventTitle(mEventRects!![i].event, mEventRects!![i].rectF!!, canvas, top, left)
                    } else
                        mEventRects!![i].rectF = null
                }
            }
        }
    }

    /**
     * Draw the name of the event on top of the event rectangle.
     *
     * @param event        The event of which the title (and location) should be drawn.
     * @param rect         The rectangle on which the text is to be drawn.
     * @param canvas       The canvas to draw upon.
     * @param originalTop  The original top position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     * @param originalLeft The original left position of the rectangle. The rectangle may have some of its portion outside of the visible area.
     */
    private fun drawEventTitle(event: WeekViewEvent, rect: RectF, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        if (rect.right - rect.left - (mEventPadding * 2).toFloat() < 0) return
        if (rect.bottom - rect.top - (mEventPadding * 2).toFloat() < 0) return

        // Prepare the name of the event.
        val bob = SpannableStringBuilder()
        if (!TextUtils.isEmpty(event.name)) {
            bob.append(event.name)
            bob.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, bob.length, 0)
        }
        // Prepare the location of the event.
        if (!TextUtils.isEmpty(event.location)) {
            if (bob.length > 0)
                bob.append(' ')
            bob.append(event.location)
        }

        val availableHeight = (rect.bottom - originalTop - (mEventPadding * 2).toFloat()).toInt()
        val availableWidth = (rect.right - originalLeft - (mEventPadding * 2).toFloat()).toInt()

        // Get text color if necessary
        if (textColorPicker != null) {
            mEventTextPaint.color = textColorPicker!!.getTextColor(event)
        }
        // Get text dimensions.
        var textLayout = StaticLayout(bob, mEventTextPaint, availableWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)
        if (textLayout.lineCount > 0) {
            val lineHeight = textLayout.height / textLayout.lineCount

            if (availableHeight >= lineHeight) {
                // Calculate available number of line counts.
                var availableLineCount = availableHeight / lineHeight
                do {
                    // Ellipsize text to fit into event rect.
                    if (newEventIdentifier != event.identifier)
                        textLayout = StaticLayout(TextUtils.ellipsize(bob, mEventTextPaint, (availableLineCount * availableWidth).toFloat(), TextUtils.TruncateAt.END), mEventTextPaint, (rect.right - originalLeft - (mEventPadding * 2).toFloat()).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)

                    // Reduce line count.
                    availableLineCount--

                    // Repeat until text is short enough.
                } while (textLayout.height > availableHeight)

                // Draw text.
                canvas.save()
                canvas.translate(originalLeft + mEventPadding, originalTop + mEventPadding)
                textLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    /**
     * Draw the text on top of the rectangle in the empty event.
     */
    private fun drawEmptyImage(event: WeekViewEvent, rect: RectF, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        val size = Math.max(1, Math.floor(Math.min(0.8 * rect.height(), 0.8 * rect.width())).toInt())
        if (newEventIconDrawable == null)
            newEventIconDrawable = AppCompatResources.getDrawable(context, android.R.drawable.ic_input_add)
        var icon = (newEventIconDrawable as BitmapDrawable).bitmap
        icon = Bitmap.createScaledBitmap(icon, size, size, false)
        canvas.drawBitmap(icon, originalLeft + (rect.width() - icon.width) / 2, originalTop + (rect.height() - icon.height) / 2, mEmptyEventPaint)
    }

    /**
     * A class to hold reference to the events and their visual representation. An EventRect is
     * actually the rectangle that is drawn on the calendar for a given event. There may be more
     * than one rectangle for a single event (an event that expands more than one day). In that
     * case two instances of the EventRect will be used for a single event. The given event will be
     * stored in "originalEvent". But the event that corresponds to rectangle the rectangle
     * instance will be stored in "event".
     */
    private inner class EventRect
    /**
     * Create a new instance of event rect. An EventRect is actually the rectangle that is drawn
     * on the calendar for a given event. There may be more than one rectangle for a single
     * event (an event that expands more than one day). In that case two instances of the
     * EventRect will be used for a single event. The given event will be stored in
     * "originalEvent". But the event that corresponds to rectangle the rectangle instance will
     * be stored in "event".
     *
     * @param event         Represents the event which this instance of rectangle represents.
     * @param originalEvent The original event that was passed by the user.
     * @param rectF         The rectangle.
     */
    (var event: WeekViewEvent, var originalEvent: WeekViewEvent, var rectF: RectF?) {
        var left: Float = 0f
        var width: Float = 0f
        var top: Float = 0f
        var bottom: Float = 0f
    }


    /**
     * Gets more events of one/more month(s) if necessary. This method is called when the user is
     * scrolling the week view. The week view stores the events of three months: the visible month,
     * the previous month, the next month.
     *
     * @param day The day where the user is currently is.
     */
    private fun getMoreEvents(day: Calendar) {

        // Get more events if the month is changed.
        if (mEventRects == null)
            mEventRects = ArrayList()

        if (mEvents == null)
            mEvents = ArrayList()

        if (weekViewLoader == null && !isInEditMode)
            throw IllegalStateException("You must provide a MonthChangeListener")

        // If a refresh was requested then reset some variables.
        if (mRefreshEvents) {
            this.clearEvents()
            mFetchedPeriod = -1
        }

        if (weekViewLoader != null) {
            val periodToFetch = weekViewLoader!!.toWeekViewPeriodIndex(day).toInt()
            if (!isInEditMode && (mFetchedPeriod < 0 || mFetchedPeriod != periodToFetch || mRefreshEvents)) {
                val newEvents = weekViewLoader!!.onLoad(periodToFetch)

                // Clear events.
                this.clearEvents()
                cacheAndSortEvents(newEvents)
                calculateHeaderHeight()

                mFetchedPeriod = periodToFetch
            }
        }

        // Prepare to calculate positions of each events.
        val tempEvents = mEventRects
        mEventRects = ArrayList()

        // Iterate through each day with events to calculate the position of the events.
        while (tempEvents!!.size > 0) {
            val eventRects = ArrayList<EventRect>(tempEvents.size)

            // Get first event for a day.
            val eventRect1 = tempEvents.removeAt(0)
            eventRects.add(eventRect1)

            var i = 0
            while (i < tempEvents.size) {
                // Collect all other events for same day.
                val eventRect2 = tempEvents[i]
                if (isSameDay(eventRect1.event.startTime!!, eventRect2.event.startTime!!)) {
                    tempEvents.removeAt(i)
                    eventRects.add(eventRect2)
                } else {
                    i++
                }
            }
            computePositionOfEvents(eventRects)
        }
    }

    private fun clearEvents() {
        mEventRects!!.clear()
        mEvents!!.clear()
    }

    /**
     * Cache the event for smooth scrolling functionality.
     *
     * @param event The event to cache.
     */
    private fun cacheEvent(event: WeekViewEvent) {
        if (event.startTime!! >= event.endTime)
            return
        val splitedEvents = event.splitWeekViewEvents()
        for (splitedEvent in splitedEvents) {
            mEventRects!!.add(EventRect(splitedEvent, event, null))
        }

        mEvents!!.add(event)
    }

    /**
     * Cache and sort events.
     *
     * @param events The events to be cached and sorted.
     */
    private fun cacheAndSortEvents(events: MutableList<WeekViewEvent>?) {
        if (events != null)
            for (event in events)
                cacheEvent(event)
        sortEventRects(mEventRects)
    }

    /**
     * Sorts the events in ascending order.
     *
     * @param eventRects The events to be sorted.
     */
    private fun sortEventRects(eventRects: MutableList<EventRect>?) {
        eventRects?.sortWith(Comparator { left, right ->
            val start1 = left.event.startTime!!.timeInMillis
            val start2 = right.event.startTime!!.timeInMillis
            var comparator = if (start1 > start2) 1 else if (start1 < start2) -1 else 0
            if (comparator == 0) {
                val end1 = left.event.endTime!!.timeInMillis
                val end2 = right.event.endTime!!.timeInMillis
                comparator = if (end1 > end2) 1 else if (end1 < end2) -1 else 0
            }
            comparator
        })
    }

    /**
     * Calculates the left and right positions of each events. This comes handy specially if events
     * are overlapping.
     *
     * @param eventRects The events along with their wrapper class.
     */
    private fun computePositionOfEvents(eventRects: MutableList<EventRect>) {
        // Make "collision groups" for all events that collide with others.
        val collisionGroups = ArrayList<ArrayList<EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false

            outerLoop@ for (collisionGroup in collisionGroups) {
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event) && groupEvent.event.isAllDay == eventRect.event.isAllDay) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            }

            if (!isPlaced) {
                val newGroup = ArrayList<EventRect>()
                newGroup.add(eventRect)
                collisionGroups.add(newGroup)
            }
        }

        for (collisionGroup in collisionGroups) {
            expandEventsToMaxWidth(collisionGroup)
        }
    }

    /**
     * Expands all the events to maximum possible width. The events will try to occupy maximum
     * space available horizontally.
     *
     * @param collisionGroup The group of events which overlap with each other.
     */
    private fun expandEventsToMaxWidth(collisionGroup: MutableList<EventRect>) {
        // Expand the events to maximum possible width.
        val columns = ArrayList<ArrayList<EventRect>>()
        columns.add(ArrayList())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.size == 0) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column[column.size - 1].event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = ArrayList<EventRect>()
                newColumn.add(eventRect)
                columns.add(newColumn)
            }
        }

        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        var maxRowCount = 0
        for (column in columns) {
            maxRowCount = Math.max(maxRowCount, column.size)
        }
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            var j = 0f
            for (column in columns) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.width = 1f / columns.size
                    eventRect.left = j / columns.size
                    if (!eventRect.event.isAllDay) {
                        eventRect.top = getPassedMinutesInDay(eventRect.event.startTime!!).toFloat()
                        eventRect.bottom = getPassedMinutesInDay(eventRect.event.endTime!!).toFloat()
                    } else {
                        eventRect.top = 0f
                        eventRect.bottom = allDayEventHeight.toFloat()
                    }
                    mEventRects!!.add(eventRect)
                }
                j++
            }
        }
    }

    /**
     * Checks if two events overlap.
     *
     * @param event1 The first event.
     * @param event2 The second event.
     * @return true if the events overlap.
     */
    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        val start1 = event1.startTime!!.timeInMillis
        val end1 = event1.endTime!!.timeInMillis
        val start2 = event2.startTime!!.timeInMillis
        val end2 = event2.endTime!!.timeInMillis

        val minOverlappingMillis = (minOverlappingMinutes * 60 * 1000).toLong()

        return !(start1 + minOverlappingMillis >= end2 || end1 <= start2 + minOverlappingMillis)
    }


    /**
     * Checks if time1 occurs after (or at the same time) time2.
     *
     * @param time1 The time to check.
     * @param time2 The time to check against.
     * @return true if time1 and time2 are equal or if time1 is after time2. Otherwise false.
     */
    private fun isTimeAfterOrEquals(time1: Calendar?, time2: Calendar?): Boolean {
        return !(time1 == null || time2 == null) && time1.timeInMillis >= time2.timeInMillis
    }

    override fun invalidate() {
        super.invalidate()
        mAreDimensionsInvalid = true
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to setting and getting the properties.
    //
    /////////////////////////////////////////////////////////////////

    private fun recalculateHourHeight() {
        val height = ((height - (mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom)) / (this.mMaxTime - this.mMinTime)).toInt()
        if (height > mHourHeight) {
            if (height > maxHourHeight)
                maxHourHeight = height
            mNewHourHeight = height
        }
    }

    /**
     * Set visible time span.
     *
     * @param startHour limit time display on top (between 0~24)
     * @param endHour   limit time display at bottom (between 0~24 and larger than startHour)
     */
    fun setLimitTime(startHour: Int, endHour: Int) {
        when {
            endHour <= startHour -> throw IllegalArgumentException("endHour must larger startHour.")
            startHour < 0 -> throw IllegalArgumentException("startHour must be at least 0.")
            endHour > 24 -> throw IllegalArgumentException("endHour can't be higher than 24.")
            else -> {
                this.mMinTime = startHour
                this.mMaxTime = endHour
                recalculateHourHeight()
                invalidate()
            }
        }
    }

    /**
     * Set minimal shown time
     *
     * @param startHour limit time display on top (between 0~24) and smaller than endHour
     */
    fun setMinTime(startHour: Int) {
        if (mMaxTime <= startHour) {
            throw IllegalArgumentException("startHour must smaller than endHour")
        } else if (startHour < 0) {
            throw IllegalArgumentException("startHour must be at least 0.")
        }
        this.mMinTime = startHour
        recalculateHourHeight()
    }

    /**
     * Set highest shown time
     *
     * @param endHour limit time display at bottom (between 0~24 and larger than startHour)
     */
    fun setMaxTime(endHour: Int) {
        if (endHour <= mMinTime) {
            throw IllegalArgumentException("endHour must be larger than startHour.")
        } else if (endHour > 24) {
            throw IllegalArgumentException("endHour can't be higher than 24.")
        }
        this.mMaxTime = endHour
        recalculateHourHeight()
        invalidate()
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    override fun onTouchEvent(event: MotionEvent): Boolean {

        mSizeOfWeekView = (mWidthPerDay + mColumnGap) * numberOfVisibleDays
        mDistanceMin = mSizeOfWeekView / mOffsetValueToSecureScreen

        mScaleDetector!!.onTouchEvent(event)
        val value = mGestureDetector!!.onTouchEvent(event)

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && !mIsZooming && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestOrigin()
            }
            mCurrentScrollDirection = Direction.NONE
        }

        return value
    }

    private fun goToNearestOrigin() {
        var leftDays = (mCurrentOrigin.x / (mWidthPerDay + mColumnGap)).toDouble()

        val beforeScroll = mStartOriginForScroll
        var isPassed = false

        if (mDistanceDone > mDistanceMin || mDistanceDone < -mDistanceMin || !mIsScrollNumberOfVisibleDays) {

            when {
                !mIsScrollNumberOfVisibleDays && mCurrentFlingDirection != Direction.NONE -> // snap to nearest day
                    leftDays = Math.round(leftDays).toDouble()
                mCurrentScrollDirection == Direction.LEFT -> {
                    // snap to last day
                    leftDays = Math.floor(leftDays)
                    mStartOriginForScroll -= mSizeOfWeekView
                    isPassed = true
                }
                mCurrentScrollDirection == Direction.RIGHT -> {
                    // snap to next day
                    leftDays = Math.floor(leftDays)
                    mStartOriginForScroll += mSizeOfWeekView
                    isPassed = true
                }
                else -> // snap to nearest day
                    leftDays = Math.round(leftDays).toDouble()
            }


            if (mIsScrollNumberOfVisibleDays) {
                val mayScrollHorizontal = beforeScroll - mStartOriginForScroll < xMaxLimit && mCurrentOrigin.x - mStartOriginForScroll > xMinLimit
                if (isPassed && mayScrollHorizontal) {
                    // Stop current animation.
                    mScroller!!.forceFinished(true)
                    // Snap to date.
                    if (mCurrentScrollDirection == Direction.LEFT) {
                        mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (beforeScroll - mCurrentOrigin.x - mSizeOfWeekView).toInt(), 0, 200)
                    } else if (mCurrentScrollDirection == Direction.RIGHT) {
                        mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (mSizeOfWeekView - (mCurrentOrigin.x - beforeScroll)).toInt(), 0, 200)
                    }
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
            } else {
                val nearestOrigin = (mCurrentOrigin.x - leftDays * (mWidthPerDay + mColumnGap)).toInt()
                val mayScrollHorizontal = mCurrentOrigin.x - nearestOrigin < xMaxLimit && mCurrentOrigin.x - nearestOrigin > xMinLimit
                if (mayScrollHorizontal) {
                    mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0)
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }

                if (nearestOrigin != 0 && mayScrollHorizontal) {
                    // Stop current animation.
                    mScroller!!.forceFinished(true)
                    // Snap to date.
                    mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerDay * scrollDuration).toInt())
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
            }

            // Reset scrolling and fling direction.
            mCurrentFlingDirection = Direction.NONE
            mCurrentScrollDirection = mCurrentFlingDirection


        } else {
            mScroller!!.forceFinished(true)
            if (mCurrentScrollDirection == Direction.LEFT) {
                mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), beforeScroll.toInt() - mCurrentOrigin.x.toInt(), 0, 200)
            } else if (mCurrentScrollDirection == Direction.RIGHT) {
                mScroller!!.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), beforeScroll.toInt() - mCurrentOrigin.x.toInt(), 0, 200)
            }
            ViewCompat.postInvalidateOnAnimation(this@WeekView)

            // Reset scrolling and fling direction.
            mCurrentFlingDirection = Direction.NONE
            mCurrentScrollDirection = mCurrentFlingDirection
        }
    }

    override fun computeScroll() {
        super.computeScroll()

        if (mScroller!!.isFinished) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin()
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin()
            } else if (mScroller!!.computeScrollOffset()) {
                mCurrentOrigin.y = mScroller!!.currY.toFloat()
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     *
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private fun forceFinishScroll(): Boolean {
        return mScroller!!.currVelocity <= mMinimumFlingVelocity
    }


    /////////////////////////////////////////////////////////////////
    //
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Show today on the week view.
     */
    fun goToToday() {
        val today = Calendar.getInstance()
        goToDate(today)
    }

    /**
     * Show a specific day on the week view.
     *
     * @param date The date to show.
     */
    fun goToDate(date: Calendar) {
        mScroller!!.forceFinished(true)
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        date.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if (mAreDimensionsInvalid) {
            mScrollToDay = date
            return
        }

        mRefreshEvents = true

        mCurrentOrigin.x = -daysBetween(mHomeDate!!, date) * (mWidthPerDay + mColumnGap)
        invalidate()
    }

    /**
     * Refreshes the view and loads the events again.
     */
    fun notifyDataSetChanged() {
        mRefreshEvents = true
        invalidate()
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     *
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    fun goToHour(hour: Double) {
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour
            return
        }

        var verticalOffset = 0
        if (hour > mMaxTime)
            verticalOffset = mHourHeight * (mMaxTime - mMinTime)
        else if (hour > mMinTime)
            verticalOffset = (mHourHeight * hour).toInt()

        if (verticalOffset > (mHourHeight * (mMaxTime - mMinTime) - height).toFloat() + mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom)
            verticalOffset = ((mHourHeight * (mMaxTime - mMinTime) - height).toFloat() + mHeaderHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom).toInt()

        mCurrentOrigin.y = (-verticalOffset).toFloat()
        invalidate()
    }

    /**
     * Determine whether a given calendar day falls within the scroll limits set for this view.
     *
     * @param day the day to check
     * @return True if there are no limit or the date is within the limits.
     * @see .setMinDate
     * @see .setMaxDate
     */
    fun dateIsValid(day: Calendar): Boolean {
        if (minDate != null && day.before(minDate)) {
            return false
        }
        return !(maxDate != null && day.after(maxDate))
    }

    //region interfaces

    interface DropListener {
        /**
         * Triggered when view dropped
         *
         * @param view: dropped view.
         * @param date: object set with the date and time of the dropped coordinates on the view.
         */
        fun onDrop(view: View, date: Calendar)
    }

    interface EventClickListener {
        /**
         * Triggered when clicked on one existing event
         *
         * @param event:     event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventClick(event: WeekViewEvent, eventRect: RectF)
    }

    interface EventLongPressListener {
        /**
         * Similar to [com.alamkanak.weekview.WeekView.EventClickListener] but with a long press.
         *
         * @param event:     event clicked.
         * @param eventRect: view containing the clicked event.
         */
        fun onEventLongPress(event: WeekViewEvent, eventRect: RectF)
    }

    interface EmptyViewClickListener {
        /**
         * Triggered when the users clicks on a empty space of the calendar.
         *
         * @param date: [Calendar] object set with the date and time of the clicked position on the view.
         */
        fun onEmptyViewClicked(date: Calendar)

    }

    interface EmptyViewLongPressListener {
        /**
         * Similar to [com.alamkanak.weekview.WeekView.EmptyViewClickListener] but with long press.
         *
         * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
         */
        fun onEmptyViewLongPress(time: Calendar)
    }

    interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         *
         * (this will also be called during the first draw of the weekview)
         *
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar?)
    }

    interface AddEventClickListener {
        /**
         * Triggered when the users clicks to create a new event.
         *
         * @param startTime The startTime of a new event
         * @param endTime   The endTime of a new event
         */
        fun onAddEventClicked(startTime: Calendar, endTime: Calendar)
    }

    /**
     * A simple GestureListener that holds the focused hour while scaling.
     */
    private inner class WeekViewGestureListener : ScaleGestureDetector.OnScaleGestureListener {
        internal var mFocusedPointY: Float = 0f

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mIsZooming = false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mIsZooming = true
            goToNearestOrigin()

            // Calculate focused point for scale action
            if (isZoomFocusPointEnabled) {
                // Use fractional focus, percentage of height
                mFocusedPointY = (height.toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom) * mZoomFocusPoint
            } else {
                // Grab focus
                mFocusedPointY = detector.focusY
            }

            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor

            mNewHourHeight = Math.round(mHourHeight * scale)

            // Calculating difference
            var diffY = mFocusedPointY - mCurrentOrigin.y
            // Scaling difference
            diffY = diffY * scale - diffY
            // Updating week view origin
            mCurrentOrigin.y -= diffY

            invalidate()
            return true
        }

    }

    private inner class DragListener : View.OnDragListener {
        override fun onDrag(v: View, e: DragEvent): Boolean {
            when (e.action) {
                DragEvent.ACTION_DROP -> if (e.x > mHeaderColumnWidth && e.y > mHeaderTextHeight + (mHeaderRowPadding * 2).toFloat() + mHeaderMarginBottom) {
                    val selectedTime = getTimeFromPoint(e.x, e.y)
                    if (selectedTime != null) {
                        weekViewDropListener!!.onDrop(v, selectedTime)
                    }
                }
            }
            return true
        }
    }

    //endregion interfaces

    companion object {
        @Deprecated("")
        val LENGTH_SHORT = 1
        @Deprecated("")
        val LENGTH_LONG = 2
    }
}
