package com.undy.startrobot3.data.model

/** Shared timing constants so beep playback (BeepGenerator) and chain scheduling (ClockEngine) agree exactly. */
object BeepTiming {
    const val START_BEEP_MS = 1000L
    const val COUNTDOWN_TONE_MS = 150L
    const val COUNTDOWN_GAP_MS = 850L

    fun countdownDurationMs(count: Int): Long =
        if (count <= 0) 0L else count * COUNTDOWN_TONE_MS + (count - 1) * COUNTDOWN_GAP_MS
}
