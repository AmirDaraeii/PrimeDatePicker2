package com.aminography.primedatepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Paint.Style
import android.graphics.Typeface
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.AttrRes
import android.support.annotation.StyleRes
import android.support.annotation.StyleableRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.aminography.primecalendar.base.BaseCalendar
import com.aminography.primecalendar.common.CalendarFactory
import com.aminography.primecalendar.common.CalendarType
import com.aminography.primedatepicker.tools.CurrentCalendarType
import com.aminography.primedatepicker.tools.PersianUtils
import com.aminography.primedatepicker.tools.Utils
import org.jetbrains.anko.dip
import java.util.*

/**
 * @author aminography
 */
@Suppress("ConstantConditionIf")
class MyMonthView @JvmOverloads constructor(
        context: Context,
        @StyleableRes attrs: AttributeSet? = null,
        @AttrRes defStyleAttr: Int = 0,
        @StyleRes defStyleRes: Int = 0
) : View(context, attrs, defStyleAttr) {

    @Suppress("PrivatePropertyName")
    private val SHOW_GUIDE_LINES = false
    private val dp = dip(1)
    private fun dp(value: Float) = dp.times(value).toInt()

    private var monthLabelTextColor: Int = 0
    private var weekLabelTextColor: Int = 0
    private var dayLabelTextColor: Int = 0
    private var todayLabelTextColor: Int = 0
    private var selectedDayLabelTextColor: Int = 0
    private var selectedDayCircleColor: Int = 0
    private var disabledDayLabelTextColor: Int = 0

    private var monthLabelTextSize: Int = 0
    private var weekLabelTextSize: Int = 0
    private var dayLabelTextSize: Int = 0
    private var monthLabelTopPadding: Int = 0
    private var monthLabelBottomPadding: Int = 0
    private var weekLabelTopPadding: Int = 0
    private var weekLabelBottomPadding: Int = 0

    private var monthLabelPaint: Paint? = null
    private var weekLabelPaint: Paint? = null
    private var dayLabelPaint: Paint? = null
    private var selectedDayBackgroundPaint: Paint? = null
    var fontTypeface: Typeface? = null
        set(value) {
            field = value
            initPaints()
            invalidate()
        }

    private var maxHeight: Float = 0f
        get() = field

    private var viewWidth = 0
    private var monthHeaderHeight = 0
    private var weekHeaderHeight = 0

    private val defaultCellHeight = dp(36f).toFloat()
    private var minCellHeight: Float = 0f
    private var cellHeight: Float = defaultCellHeight
    private var cellWidth: Float = cellHeight

    private var month = 0
    private var year = 0

    var selectType: SelectType? = null
        set(value) {
            field = value
            when (value) {
                SelectType.SINGLE -> {
                    startRangeDay = null
                    endRangeDay = null
                }
                SelectType.START_RANGE -> selectedDay = null
                SelectType.END_RANGE -> selectedDay = null
            }
            invalidate()
        }

    private var startRangeDay: Int? = null
    private var endRangeDay: Int? = null
    private var selectedDay: Int? = null

    private var hasToday = false
    private var todayDayOfMonth = -1

    private var weekStart = Calendar.SUNDAY
    private var daysInMonth = 0

    private var firstDayOfMonthDayOfWeek = 0
    private var spreadingWeeks = 6

    private val dayOfWeekLabelCalendar = CalendarFactory.newInstance(CurrentCalendarType.type)
    private val firstDayOfMonthCalendar = CalendarFactory.newInstance(CurrentCalendarType.type)

    @Suppress("MemberVisibilityCanBePrivate")
    var onDayClickListener: OnDayClickListener? = null

    var minDateCalendar: BaseCalendar? = null
        set(value) {
            field = value
            invalidate()
        }

    var maxDateCalendar: BaseCalendar? = null
        set(value) {
            field = value
            invalidate()
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.MonthView, defStyleAttr, defStyleRes).apply {
            monthLabelTextColor = getColor(R.styleable.MonthView_monthLabelTextColor, ContextCompat.getColor(context, R.color.defaultMonthLabelTextColor))
            weekLabelTextColor = getColor(R.styleable.MonthView_weekLabelTextColor, ContextCompat.getColor(context, R.color.defaultWeekLabelTextColor))
            dayLabelTextColor = getColor(R.styleable.MonthView_dayLabelTextColor, ContextCompat.getColor(context, R.color.defaultDayLabelTextColor))
            todayLabelTextColor = getColor(R.styleable.MonthView_todayLabelTextColor, ContextCompat.getColor(context, R.color.defaultTodayLabelTextColor))
            selectedDayLabelTextColor = getColor(R.styleable.MonthView_selectedDayLabelTextColor, ContextCompat.getColor(context, R.color.defaultSelectedDayLabelTextColor))
            selectedDayCircleColor = getColor(R.styleable.MonthView_selectedDayCircleColor, ContextCompat.getColor(context, R.color.defaultSelectedDayCircleColor))
            disabledDayLabelTextColor = getColor(R.styleable.MonthView_disabledDayLabelTextColor, ContextCompat.getColor(context, R.color.defaultDisabledDayLabelTextColor))

            monthLabelTextSize = getDimensionPixelSize(R.styleable.MonthView_monthLabelTextSize, resources.getDimensionPixelSize(R.dimen.defaultMonthLabelTextSize))
            weekLabelTextSize = getDimensionPixelSize(R.styleable.MonthView_weekLabelTextSize, resources.getDimensionPixelSize(R.dimen.defaultWeekLabelTextSize))
            dayLabelTextSize = getDimensionPixelSize(R.styleable.MonthView_dayLabelTextSize, resources.getDimensionPixelSize(R.dimen.defaultDayLabelTextSize))

            monthLabelTopPadding = getDimensionPixelSize(R.styleable.MonthView_monthLabelTopPadding, resources.getDimensionPixelSize(R.dimen.defaultMonthLabelTopPadding))
            monthLabelBottomPadding = getDimensionPixelSize(R.styleable.MonthView_monthLabelBottomPadding, resources.getDimensionPixelSize(R.dimen.defaultMonthLabelBottomPadding))
            weekLabelTopPadding = getDimensionPixelSize(R.styleable.MonthView_weekLabelTopPadding, resources.getDimensionPixelSize(R.dimen.defaultWeekLabelTopPadding))
            weekLabelBottomPadding = getDimensionPixelSize(R.styleable.MonthView_weekLabelBottomPadding, resources.getDimensionPixelSize(R.dimen.defaultWeekLabelBottomPadding))
            recycle()
        }

        monthHeaderHeight = monthLabelTextSize + monthLabelTopPadding + monthLabelBottomPadding
        weekHeaderHeight = weekLabelTextSize + weekLabelTopPadding + weekLabelBottomPadding

        minCellHeight = dayLabelTextSize.toFloat()
        cellHeight = 2.75f * dayLabelTextSize
        cellWidth = (viewWidth - (paddingLeft + paddingRight)) / 7.toFloat()

        initPaints()
    }

    private fun initPaints() {
        monthLabelPaint = Paint().apply {
            textSize = monthLabelTextSize.toFloat()
            color = monthLabelTextColor
            style = Style.FILL
            textAlign = Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            fontTypeface?.apply {
                typeface = this
            }
        }

        weekLabelPaint = Paint().apply {
            textSize = weekLabelTextSize.toFloat()
            color = weekLabelTextColor
            style = Style.FILL
            textAlign = Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            fontTypeface?.apply {
                typeface = this
            }
        }

        dayLabelPaint = Paint().apply {
            textSize = dayLabelTextSize.toFloat()
            color = dayLabelTextColor
            style = Style.FILL
            textAlign = Align.CENTER
            isAntiAlias = true
            isFakeBoldText = false
            fontTypeface?.apply {
                typeface = this
            }
        }

        selectedDayBackgroundPaint = Paint().apply {
            color = selectedDayCircleColor
            style = Style.FILL
            textAlign = Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        if (isInEditMode) {
            val calendar = Utils.newCalendar()
            setDate(calendar.year, calendar.month)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        maxHeight = paddingTop + monthHeaderHeight + weekHeaderHeight + cellHeight * 6 + paddingBottom
        val height = paddingTop + monthHeaderHeight + weekHeaderHeight + cellHeight * spreadingWeeks + paddingBottom
        setMeasuredDimension(View.MeasureSpec.getSize(widthMeasureSpec), height.toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        viewWidth = w
        cellWidth = (viewWidth - (paddingLeft + paddingRight)) / 7.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        drawMonthLabel(canvas)
        drawWeekLabels(canvas)
        drawDayLabels(canvas)
    }

    fun setDate(year: Int, month: Int) {
        this.year = year
        this.month = month

        firstDayOfMonthCalendar.setDate(year, month, 1)
        firstDayOfMonthDayOfWeek = firstDayOfMonthCalendar.get(Calendar.DAY_OF_WEEK)

        daysInMonth = Utils.getDaysInMonth(month, year)
        weekStart = when (CurrentCalendarType.type) {
            CalendarType.CIVIL -> Calendar.SUNDAY
            CalendarType.PERSIAN -> Calendar.SATURDAY
            CalendarType.HIJRI -> Calendar.SATURDAY
        }

        updateToday()
        updateSpreadingWeeks()

        requestLayout()
        invalidate()
    }

    private fun updateToday() {
        val todayCalendar = CalendarFactory.newInstance(CurrentCalendarType.type)
        hasToday = todayCalendar.year == year && todayCalendar.month == month
        todayDayOfMonth = if (hasToday) todayCalendar.dayOfMonth else -1
    }

    private fun updateSpreadingWeeks() {
        val offset = weekOffset(firstDayOfMonthDayOfWeek)
        val dividend = (offset + daysInMonth) / 7
        val remainder = (offset + daysInMonth) % 7
        spreadingWeeks = dividend + if (remainder > 0) 1 else 0
    }

    private fun weekOffset(dayOfWeek: Int): Int {
        val day = if (dayOfWeek < weekStart) dayOfWeek + 7 else dayOfWeek
        return (day - weekStart) % 7
    }

    private fun drawMonthLabel(canvas: Canvas) {
        val x = viewWidth / 2
        val y = paddingTop + (monthHeaderHeight - monthLabelTopPadding - monthLabelBottomPadding) / 2 + monthLabelTopPadding

        var monthAndYearString = "${firstDayOfMonthCalendar.monthName} ${firstDayOfMonthCalendar.year}"
        monthAndYearString = when (CurrentCalendarType.type) {
            CalendarType.CIVIL -> monthAndYearString
            CalendarType.PERSIAN -> PersianUtils.convertLatinDigitsToPersian(monthAndYearString)
            CalendarType.HIJRI -> PersianUtils.convertLatinDigitsToPersian(monthAndYearString)
        }

        monthLabelPaint?.apply {
            canvas.drawText(
                    monthAndYearString,
                    x.toFloat(),
                    y.toFloat() - ((descent() + ascent()) / 2),
                    this
            )
        }

        if (SHOW_GUIDE_LINES) {
            Paint().apply {
                isAntiAlias = true
                color = Color.RED
                style = Style.FILL
                alpha = 50
                canvas.drawRect(
                        paddingLeft.toFloat(),
                        paddingTop.toFloat(),
                        viewWidth - paddingRight.toFloat(),
                        paddingTop + monthHeaderHeight.toFloat(),
                        this
                )
            }
            Paint().apply {
                isAntiAlias = true
                color = Color.RED
                style = Style.FILL
                canvas.drawCircle(
                        x.toFloat(),
                        paddingTop + (monthHeaderHeight / 2).toFloat(),
                        dp(1f).toFloat(),
                        this
                )
            }
        }
    }

    private fun drawWeekLabels(canvas: Canvas) {
        val y = paddingTop + monthHeaderHeight + (weekHeaderHeight - weekLabelTopPadding - weekLabelBottomPadding) / 2 + weekLabelTopPadding

        for (i in 0 until 7) {
            val dayOfWeek = (i + weekStart) % 7

            // RTLize for Persian and Hijri Calendars
            val x = when (CurrentCalendarType.type) {
                CalendarType.CIVIL -> (2 * i + 1) * (cellWidth / 2) + paddingLeft
                CalendarType.PERSIAN -> (2 * (6 - i) + 1) * (cellWidth / 2) + paddingLeft
                CalendarType.HIJRI -> (2 * (6 - i) + 1) * (cellWidth / 2) + paddingLeft
            }

            dayOfWeekLabelCalendar.set(Calendar.DAY_OF_WEEK, dayOfWeek)
            val localWeekDisplayName = dayOfWeekLabelCalendar.weekDayName

            val weekString = when (CurrentCalendarType.type) {
                CalendarType.CIVIL -> localWeekDisplayName.substring(0, 2)
                CalendarType.PERSIAN -> localWeekDisplayName.substring(0, 1)
                CalendarType.HIJRI -> localWeekDisplayName.substring(2, 4)
            }

            weekLabelPaint?.apply {
                canvas.drawText(
                        weekString,
                        x,
                        y.toFloat() - ((descent() + ascent()) / 2),
                        this
                )
            }

            if (SHOW_GUIDE_LINES) {
                Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    style = Style.STROKE
                    canvas.drawRect(
                            (x - cellWidth / 2),
                            paddingTop + monthHeaderHeight.toFloat(),
                            (x + cellWidth / 2),
                            paddingTop + monthHeaderHeight + weekHeaderHeight.toFloat(),
                            this
                    )
                }
                Paint().apply {
                    isAntiAlias = true
                    color = Color.RED
                    style = Style.FILL
                    canvas.drawCircle(
                            x,
                            paddingTop + (monthHeaderHeight + weekHeaderHeight / 2).toFloat(),
                            dp(1f).toFloat(),
                            this
                    )
                }
            }
        }

        if (SHOW_GUIDE_LINES) {
            Paint().apply {
                isAntiAlias = true
                color = Color.GREEN
                style = Style.FILL
                alpha = 50
                canvas.drawRect(
                        paddingLeft.toFloat(),
                        paddingTop + monthHeaderHeight.toFloat(),
                        viewWidth.toFloat() - paddingRight.toFloat(),
                        paddingTop + (monthHeaderHeight + weekHeaderHeight).toFloat(),
                        this
                )
            }
        }
    }

    private fun drawDayLabels(canvas: Canvas) {
        var topY: Float = (paddingTop + monthHeaderHeight + weekHeaderHeight).toFloat()
        var offset = weekOffset(firstDayOfMonthDayOfWeek)
        for (dayNumber in 1..daysInMonth) {
            val y = topY + cellHeight / 2

            // RTLize for Persian and Hijri Calendars
            val x = when (CurrentCalendarType.type) {
                CalendarType.CIVIL -> ((2 * offset + 1) * (cellWidth / 2) + paddingLeft)
                CalendarType.PERSIAN -> ((2 * (6 - offset) + 1) * (cellWidth / 2) + paddingLeft)
                CalendarType.HIJRI -> ((2 * (6 - offset) + 1) * (cellWidth / 2) + paddingLeft)
            }

            drawDayBackground(canvas, year, month, dayNumber, x, y, cellWidth, cellHeight)
            drawDayLabel(canvas, year, month, dayNumber, x, y, cellWidth, cellHeight)

            if (SHOW_GUIDE_LINES) {
                Paint().apply {
                    isAntiAlias = true
                    color = Color.GRAY
                    style = Style.STROKE
                    canvas.drawRect(
                            x - cellWidth / 2,
                            topY,
                            x + cellWidth / 2,
                            topY + cellHeight,
                            this
                    )
                }
                Paint().apply {
                    isAntiAlias = true
                    color = Color.RED
                    style = Style.FILL
                    canvas.drawCircle(
                            x,
                            y,
                            dp(1f).toFloat(),
                            this
                    )
                }
            }

            offset++
            if (offset == 7) {
                offset = 0
                topY += cellHeight
            }
        }
    }

    private fun drawDayBackground(canvas: Canvas, year: Int, month: Int, dayOfMonth: Int, x: Float, y: Float, width: Float, height: Float) {
        val radius = Math.min(width, height) / 2 - dp(2f)
        when (selectType) {
            SelectType.SINGLE -> {
                selectedDayBackgroundPaint?.apply {
                    if (dayOfMonth == selectedDay) {
                        canvas.drawCircle(
                                x,
                                y,
                                radius,
                                this
                        )
                    }
                }
            }
            SelectType.START_RANGE, SelectType.END_RANGE -> {
                selectedDayBackgroundPaint?.apply {
                    when (dayOfMonth) {
                        startRangeDay -> {
                            if (endRangeDay == null || startRangeDay == endRangeDay) {
                                canvas.drawCircle(
                                        x,
                                        y,
                                        radius,
                                        this
                                )
                            } else {
                                canvas.drawCircle(
                                        x,
                                        y,
                                        radius,
                                        this
                                )
                                // RTLize for Persian and Hijri Calendars
                                when (CurrentCalendarType.type) {
                                    CalendarType.CIVIL -> {
                                        canvas.drawRect(
                                                x,
                                                y - radius,
                                                (x + cellWidth / 2),
                                                y + radius,
                                                this
                                        )
                                    }
                                    CalendarType.PERSIAN, CalendarType.HIJRI -> {
                                        canvas.drawRect(
                                                x - cellWidth / 2,
                                                y - radius,
                                                x,
                                                y + radius,
                                                this
                                        )
                                    }
                                }
                            }
                        }
                        endRangeDay -> {
                            canvas.drawCircle(
                                    x,
                                    y,
                                    radius,
                                    this
                            )
                            // RTLize for Persian and Hijri Calendars
                            when (CurrentCalendarType.type) {
                                CalendarType.CIVIL -> {
                                    canvas.drawRect(
                                            x - cellWidth / 2,
                                            y - radius,
                                            x,
                                            y + radius,
                                            this
                                    )
                                }
                                CalendarType.PERSIAN, CalendarType.HIJRI -> {
                                    canvas.drawRect(
                                            x,
                                            y - radius,
                                            x + cellWidth / 2,
                                            y + radius,
                                            this
                                    )
                                }
                            }
                        }
                        in ((startRangeDay ?: -1) + 1)..((endRangeDay ?: +1) - 1) -> {
                            canvas.drawRect(
                                    x - cellWidth / 2,
                                    y - radius,
                                    x + cellWidth / 2,
                                    y + radius,
                                    this
                            )
                        }
                    }
                }
            }
        }
    }

    private fun drawDayLabel(canvas: Canvas, year: Int, month: Int, dayOfMonth: Int, x: Float, y: Float, width: Float, height: Float) {
        dayLabelPaint?.apply {
            color = if (isOutOfRange(year, month, dayOfMonth)) {
                disabledDayLabelTextColor
            } else if (selectType == SelectType.SINGLE) {
                if (dayOfMonth == selectedDay) {
                    selectedDayLabelTextColor
                } else {
                    dayLabelTextColor
                }
            } else if (selectType == SelectType.START_RANGE || selectType == SelectType.END_RANGE) {
                when (dayOfMonth) {
                    startRangeDay -> {
                        selectedDayLabelTextColor
                    }
                    endRangeDay -> {
                        selectedDayLabelTextColor
                    }
                    in ((startRangeDay ?: -1) + 1)..((endRangeDay ?: +1) - 1) -> {
                        selectedDayLabelTextColor
                    }
                    else -> {
                        dayLabelTextColor
                    }
                }
            } else if (hasToday && dayOfMonth == todayDayOfMonth) {
                todayLabelTextColor
            } else {
                dayLabelTextColor
            }
        }

        val date = when (CurrentCalendarType.type) {
            CalendarType.CIVIL -> String.format(Locale.getDefault(), "%d", dayOfMonth)
            CalendarType.PERSIAN -> PersianUtils.convertLatinDigitsToPersian(String.format(Locale.getDefault(), "%d", dayOfMonth))
            CalendarType.HIJRI -> PersianUtils.convertLatinDigitsToPersian(String.format(Locale.getDefault(), "%d", dayOfMonth))
        }

        dayLabelPaint?.apply {
            canvas.drawText(
                    date,
                    x,
                    y - (descent() + ascent()) / 2,
                    this
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                findDayByCoordinates(event.x, event.y)?.let { dayOfMonth ->
                    ifInValidRange(dayOfMonth) {
                        when (selectType) {
                            SelectType.SINGLE -> {
                                selectedDay = dayOfMonth
                                invalidate()
                            }
                            SelectType.START_RANGE -> {
                                endRangeDay?.let { end ->
                                    if (dayOfMonth > end) {
                                        endRangeDay = null
                                    }
                                }
                                startRangeDay = dayOfMonth
                                invalidate()
                            }
                            SelectType.END_RANGE -> {
                                startRangeDay?.let { start ->
                                    if (dayOfMonth >= start) {
                                        endRangeDay = dayOfMonth
                                        invalidate()
                                    }
                                }
                            }
                        }

                        onDayClickListener?.apply {
                            val calendar = Utils.newCalendar()
                            calendar.setDate(year, month, dayOfMonth)
                            onDayClick(this@MyMonthView, calendar)
                        }
                    }
                }
            }
        }
        return true
    }

    private fun findDayByCoordinates(inputX: Float, inputY: Float): Int? {
        if (inputX < paddingLeft || inputX > viewWidth - paddingRight) return null

        val y = inputY - dp(12f)
        val row = ((y - (monthHeaderHeight + weekHeaderHeight)) / cellHeight).toInt()
        var column = ((inputX - paddingLeft) * 7 / (viewWidth - (paddingLeft + paddingRight))).toInt()

        // RTLize for Persian and Hijri Calendars
        column = when (CurrentCalendarType.type) {
            CalendarType.CIVIL -> column
            CalendarType.PERSIAN -> 6 - column
            CalendarType.HIJRI -> 6 - column
        }

        var day = column - weekOffset(firstDayOfMonthDayOfWeek) + 1
        day += row * 7

        return if (day < 1 || day > daysInMonth) {
            null
        } else day
    }

    private fun ifInValidRange(dayOfMonth: Int, function: () -> Unit) {
        if (!isOutOfRange(year, month, dayOfMonth)) function.invoke()
    }

    private fun isOutOfRange(year: Int, month: Int, day: Int): Boolean =
            isBeforeMin(year, month, day) || isAfterMax(year, month, day)

    private fun isBeforeMin(year: Int, month: Int, dayOfMonth: Int): Boolean =
            minDateCalendar?.let { min ->
                year < min.year || month < min.month || dayOfMonth < min.dayOfMonth
            } ?: false

    private fun isAfterMax(year: Int, month: Int, dayOfMonth: Int): Boolean =
            maxDateCalendar?.let { max ->
                year > max.year || month > max.month || dayOfMonth > max.dayOfMonth
            } ?: false

    enum class SelectType {
        SINGLE,
        START_RANGE,
        END_RANGE
    }

    interface OnDayClickListener {
        fun onDayClick(view: MyMonthView, day: BaseCalendar)
    }

    // Save/Restore States -------------------------------------------------------------------------

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.selectType = selectType?.ordinal ?: -1
        savedState.selectedDay = selectedDay ?: -1
        savedState.startRangeDay = startRangeDay ?: -1
        savedState.endRangeDay = endRangeDay ?: -1
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        selectType = if (savedState.selectType != -1) SelectType.values()[savedState.selectType] else null
        selectedDay = if (savedState.selectedDay != -1) savedState.selectedDay else null
        startRangeDay = if (savedState.startRangeDay != -1) savedState.startRangeDay else null
        endRangeDay = if (savedState.endRangeDay != -1) savedState.endRangeDay else null
        invalidate()
    }

    private class SavedState : View.BaseSavedState {

        internal var selectType: Int = -1
        internal var selectedDay: Int = -1
        internal var startRangeDay: Int = -1
        internal var endRangeDay: Int = -1

        internal constructor(superState: Parcelable?) : super(superState)

        private constructor(input: Parcel) : super(input) {
            selectType = input.readInt()
            selectedDay = input.readInt()
            startRangeDay = input.readInt()
            endRangeDay = input.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(selectType)
            out.writeInt(selectedDay)
            out.writeInt(startRangeDay)
            out.writeInt(endRangeDay)
        }

        companion object {

            @Suppress("unused")
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(input: Parcel): SavedState {
                    return SavedState(input)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

}
