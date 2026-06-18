package com.undy.startrobot3.data.model

import java.util.Calendar

/** Asset paths for the pre-recorded time announcement clips (assets/time_sounds/), used as
 *  an alternative to TTS. Each hourNN.wav is a complete "N o'clock" phrase (hour00/hour12 are
 *  the idiomatic "midnight"/"noon" instead) — only correct exactly on the whole hour. Off the
 *  hour, the bare numNN.wav recording is used for the hour value instead (numNN covers 00-59,
 *  e.g. num13 -> "thirteen"; hour00/12 fall back to num12 -> "twelve"), followed by the minute
 *  and (if non-zero) second clips. */
object RecordedTimeSpeech {
    private const val DIR = "time_sounds"

    fun hourAsset(hour24: Int): String = "$DIR/hour%02d.wav".format(hour24)
    fun numberAsset(n: Int): String = "$DIR/num%02d.wav".format(n)

    fun assetsFor(hour24: Int, minute: Int, second: Int = 0): List<String> {
        val onTheHour = minute == 0 && second == 0
        if (onTheHour) return listOf(hourAsset(hour24))

        val hourClip = if (hour24 == 0 || hour24 == 12) numberAsset(12) else numberAsset(hour24)
        val assets = mutableListOf(hourClip, numberAsset(minute))
        if (second != 0) assets.add(numberAsset(second))
        return assets
    }

    fun assetsFor(epochMs: Long): List<String> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        return assetsFor(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
    }
}
