package com.undy.startrobot3.data.model

enum class StartInterval(val seconds: Int, val label: String) {
    THIRTY_SECONDS(30, "30 seconds"),
    ONE_MINUTE(60, "1 minute"),
    TWO_MINUTES(120, "2 minutes");

    companion object {
        fun fromSeconds(s: Int) = entries.firstOrNull { it.seconds == s } ?: ONE_MINUTE
    }
}
