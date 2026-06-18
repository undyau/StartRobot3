package com.undy.startrobot3.data.model

enum class AnnouncementType {
    TIME,             // Speaks next start time ± offset in local format
    STARTER_NAMES,    // Reads starters for the upcoming interval from IOF XML
    TEXT,             // Custom text via TTS
    RECORDED_CLIP,    // Plays a recorded audio file
    COUNTDOWN_BEEPS,  // Auto-generated countdown beeps (N beeps, 1 per second)
    START_BEEP        // Auto-generated single start beep
}
