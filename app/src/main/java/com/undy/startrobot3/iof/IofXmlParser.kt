package com.undy.startrobot3.iof

import android.util.Xml
import com.undy.startrobot3.data.model.Starter
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object IofXmlParser {

    fun parse(inputStream: InputStream): List<Starter> {
        val starters = mutableListOf<Starter>()
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, null)

        val path = mutableListOf<String>()
        var currentGiven = ""
        var currentFamily = ""
        var currentStartTime = ""

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> path.add(parser.name ?: "")

                XmlPullParser.END_TAG -> {
                    if (path.lastOrNull() == "PersonStart") {
                        val name = listOf(currentGiven, currentFamily)
                            .filter { it.isNotEmpty() }.joinToString(" ")
                        val ms = parseIsoTime(currentStartTime)
                        if (name.isNotEmpty() && ms > 0) {
                            starters.add(Starter(name, ms))
                        }
                        currentGiven = ""; currentFamily = ""; currentStartTime = ""
                    }
                    path.removeLastOrNull()
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        when {
                            path.tailMatches("PersonStart", "Person", "Name", "Given") ->
                                currentGiven = text
                            path.tailMatches("PersonStart", "Person", "Name", "Family") ->
                                currentFamily = text
                            path.tailMatches("PersonStart", "Start", "StartTime") ->
                                currentStartTime = text
                        }
                    }
                }
            }
            event = parser.next()
        }
        return starters.sortedBy { it.startTimeMs }
    }

    private fun List<String>.tailMatches(vararg elements: String): Boolean {
        if (size < elements.size) return false
        return elements.indices.all { i -> elements[i] == this[size - elements.size + i] }
    }

    private fun parseIsoTime(iso: String): Long {
        if (iso.isEmpty()) return 0L
        return try {
            val s = iso.trim()
            when {
                s.endsWith("Z") -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    sdf.parse(s)?.time ?: 0L
                }
                s.length > 19 -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    sdf.parse(s)?.time ?: 0L
                }
                else -> {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    sdf.timeZone = TimeZone.getDefault()
                    sdf.parse(s)?.time ?: 0L
                }
            }
        } catch (_: Exception) { 0L }
    }
}
