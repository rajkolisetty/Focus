package com.focusblock.app

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** All settings and today's usage count, stored on the device only. */
object Prefs {
    private const val FILE = "focus_block_prefs"
    private const val KEY_ALLOWED = "allowed_packages"
    private const val KEY_SCHEDULE_ON = "schedule_on"
    private const val KEY_START_MIN = "start_min"
    private const val KEY_END_MIN = "end_min"
    private const val KEY_LIMIT_MIN = "limit_min"
    private const val KEY_BLOCKING_ON = "blocking_on"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getAllowed(ctx: Context): Set<String> =
        sp(ctx).getStringSet(KEY_ALLOWED, emptySet()) ?: emptySet()

    fun setAllowed(ctx: Context, packages: Set<String>) {
        sp(ctx).edit().putStringSet(KEY_ALLOWED, packages).apply()
    }

    fun isBlockingOn(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_BLOCKING_ON, false)
    fun setBlockingOn(ctx: Context, on: Boolean) { sp(ctx).edit().putBoolean(KEY_BLOCKING_ON, on).apply() }

    fun isScheduleOn(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_SCHEDULE_ON, false)
    fun setScheduleOn(ctx: Context, on: Boolean) { sp(ctx).edit().putBoolean(KEY_SCHEDULE_ON, on).apply() }

    fun getStartMinute(ctx: Context): Int = sp(ctx).getInt(KEY_START_MIN, 9 * 60)
    fun getEndMinute(ctx: Context): Int = sp(ctx).getInt(KEY_END_MIN, 21 * 60)
    fun setWindow(ctx: Context, startMin: Int, endMin: Int) {
        sp(ctx).edit().putInt(KEY_START_MIN, startMin).putInt(KEY_END_MIN, endMin).apply()
    }

    fun getDailyLimitMinutes(ctx: Context): Int = sp(ctx).getInt(KEY_LIMIT_MIN, 120)
    fun setDailyLimitMinutes(ctx: Context, minutes: Int) {
        sp(ctx).edit().putInt(KEY_LIMIT_MIN, minutes).apply()
    }

    private fun todayKey(): String =
        "usage_" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun getUsedSecondsToday(ctx: Context): Int = sp(ctx).getInt(todayKey(), 0)

    fun addUsedSeconds(ctx: Context, seconds: Int) {
        val key = todayKey()
        val cur = sp(ctx).getInt(key, 0)
        sp(ctx).edit().putInt(key, cur + seconds).apply()
    }

    /** True when there is no schedule, or the current time falls inside it.
     *  A start time later than the end time is treated as a window that crosses midnight. */
    fun isWithinWindow(ctx: Context): Boolean {
        if (!isScheduleOn(ctx)) return true
        val cal = Calendar.getInstance()
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = getStartMinute(ctx)
        val end = getEndMinute(ctx)
        return if (start <= end) nowMin in start until end else (nowMin >= start || nowMin < end)
    }

    fun isOverDailyLimit(ctx: Context): Boolean =
        getUsedSecondsToday(ctx) >= getDailyLimitMinutes(ctx) * 60
}
