package com.undy.startrobot3.data.model

/** Shared timing constants so beep playback (BeepGenerator) and chain scheduling (ClockEngine) agree exactly. */
object BeepTiming {
    const val START_BEEP_MS = 1000L
    const val COUNTDOWN_TONE_MS = 150L
    const val COUNTDOWN_GAP_MS = 850L

    // BeepGenerator builds and tears down a fresh AudioTrack for every beep (Builder().build(),
    // then stop()/release()), which has its own real setup/teardown latency the tone+gap timing
    // above doesn't capture. Without this, the last beep in a countdown that lands right before
    // a hard anchor cutoff gets clipped, since the schedule has no slack for that overhead.
    const val BEEP_CALL_OVERHEAD_MS = 150L

    fun countdownDurationMs(count: Int): Long =
        if (count <= 0) 0L
        else count * (COUNTDOWN_TONE_MS + BEEP_CALL_OVERHEAD_MS) + (count - 1) * COUNTDOWN_GAP_MS
}
