package com.undy.startrobot3.data.model

import java.util.Calendar

/** Decomposes a 12-hour time into the words actually spoken for it (no AM/PM —
 *  obvious from context during a single event). Hour, minute, and (when non-zero,
 *  e.g. a 30-second interval boundary) seconds are each spoken in sequence, read as
 *  clock fields rather than bare numbers — e.g. 10:01 -> "ten" + "oh" + "one",
 *  10:00:30 -> "ten" + "oh" + "thirty". "o'clock" is reserved for the exact whole hour. */
object TimeSpeech {
    val ALL_WORDS: List<String> = listOf(
        "oh", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
        "eighteen", "nineteen", "twenty", "thirty", "forty", "fifty", "o'clock"
    )

    private fun numberWord(n: Int): String = when (n) {
        1 -> "one"; 2 -> "two"; 3 -> "three"; 4 -> "four"; 5 -> "five"
        6 -> "six"; 7 -> "seven"; 8 -> "eight"; 9 -> "nine"; 10 -> "ten"
        11 -> "eleven"; 12 -> "twelve"; 13 -> "thirteen"; 14 -> "fourteen"; 15 -> "fifteen"
        16 -> "sixteen"; 17 -> "seventeen"; 18 -> "eighteen"; 19 -> "nineteen"; 20 -> "twenty"
        30 -> "thirty"; 40 -> "forty"; 50 -> "fifty"
        else -> error("No single word for $n")
    }

    /** Words for a 0-59 minute/second field, read as a clock field rather than a bare
     *  number: 0 -> "oh", 1-9 -> "oh" + digit (e.g. 1 -> "oh one"), 10+ as a normal number. */
    private fun wordsForCount(n: Int): List<String> = when {
        n == 0 -> listOf("oh")
        n < 10 -> listOf("oh", numberWord(n))
        n <= 20 -> listOf(numberWord(n))
        else -> {
            val tens = (n / 10) * 10
            val units = n % 10
            if (units == 0) listOf(numberWord(tens)) else listOf(numberWord(tens), numberWord(units))
        }
    }

    fun wordsFor(hour12: Int, minute: Int, second: Int = 0): List<String> {
        val hourWord = numberWord(hour12)
        // "o'clock" only for the exact whole hour — with non-zero seconds (e.g. a
        // 30-second-interval boundary at HH:00:30) minute=0 is just "oh", not "o'clock".
        val minuteWords = if (minute == 0 && second == 0) listOf("o'clock") else wordsForCount(minute)
        val secondWords = if (second == 0) emptyList() else wordsForCount(second)
        return listOf(hourWord) + minuteWords + secondWords
    }

    fun wordsFor(epochMs: Long): List<String> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        var hour = cal.get(Calendar.HOUR) // 0-11
        if (hour == 0) hour = 12
        return wordsFor(hour, cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
    }

    fun phraseFor(epochMs: Long): String = wordsFor(epochMs).joinToString(" ")
}
