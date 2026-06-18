package com.undy.startrobot3.data.model

data class Announcement(
    val id: Long = 0,
    val chainId: Long = 0,
    val sortOrder: Int = 0,
    val isAnchor: Boolean = false,
    // Only used when isAnchor=true:
    // Seconds from interval start (0 = right after prev beep; intervalSeconds = at the start beep)
    val anchorOffsetSeconds: Int = 0,
    val type: AnnouncementType = AnnouncementType.START_BEEP,
    val timeOffsetSeconds: Int = 0,       // TIME type: offset added to next start time (can be negative)
    val text: String = "",                // TEXT type: TTS text
    val audioFilePath: String = "",       // RECORDED_CLIP type: path relative to filesDir/recordings/
    val beepCount: Int = 5,              // COUNTDOWN_BEEPS type: number of beeps
    val estimatedDurationMs: Long = 2000  // used for backward-chain scheduling
) {
    /** Duration used for chain scheduling. Beep types are computed exactly from BeepTiming
     *  (their real playback time is deterministic); other types rely on the stored estimate. */
    fun effectiveDurationMs(): Long = when (type) {
        AnnouncementType.START_BEEP -> BeepTiming.START_BEEP_MS
        AnnouncementType.COUNTDOWN_BEEPS -> BeepTiming.countdownDurationMs(beepCount)
        else -> estimatedDurationMs
    }

    fun anchorLabel(): String =
        if (isAnchor) "anchor at ${anchorOffsetSeconds}s from interval start" else ""

    fun displayLabel(): String = when (type) {
        AnnouncementType.TIME -> if (timeOffsetSeconds == 0) "Next start time"
            else "Start time ${if (timeOffsetSeconds > 0) "+" else ""}${timeOffsetSeconds}s"
        AnnouncementType.STARTER_NAMES -> "Starter names"
        AnnouncementType.TEXT -> text.take(40).ifEmpty { "(empty text)" }
        AnnouncementType.RECORDED_CLIP -> text.ifEmpty {
            audioFilePath.substringAfterLast("/").ifEmpty { "Recording" }
        }
        AnnouncementType.COUNTDOWN_BEEPS -> "$beepCount-beep countdown"
        AnnouncementType.START_BEEP -> "Start beep"
    }
}
