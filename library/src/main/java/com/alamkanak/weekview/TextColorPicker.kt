package com.alamkanak.weekview

import android.support.annotation.ColorInt

interface TextColorPicker {

    @ColorInt
    fun getTextColor(event: WeekViewEvent): Int

}
