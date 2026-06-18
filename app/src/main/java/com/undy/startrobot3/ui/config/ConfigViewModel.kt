package com.undy.startrobot3.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undy.startrobot3.StartRobotApplication
import com.undy.startrobot3.data.model.Announcement
import com.undy.startrobot3.data.model.AnnouncementChain
import com.undy.startrobot3.data.model.AnnouncementType
import com.undy.startrobot3.data.model.RecordedTimeSpeech
import com.undy.startrobot3.data.model.StartInterval
import com.undy.startrobot3.data.model.TimeSpeech
import com.undy.startrobot3.engine.AudioEngine
import com.undy.startrobot3.engine.BeepGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StartRobotApplication
    private val repo = app.eventRepository
    private val prefs = app.eventPreferences
    private val audio = app.audioEngine

    val chains: StateFlow<List<AnnouncementChain>> = repo.chains
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val intervalSeconds: StateFlow<Int> = prefs.intervalSeconds
        .stateIn(viewModelScope, SharingStarted.Eagerly, 60)

    val intervalLabel: StateFlow<String> = prefs.intervalSeconds.map { s ->
        StartInterval.fromSeconds(s).label
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "1 minute")

    val useRecordedTimeSounds: StateFlow<Boolean> = prefs.useRecordedTimeSounds
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setUseRecordedTimeSounds(value: Boolean) {
        viewModelScope.launch { prefs.setUseRecordedTimeSounds(value) }
    }

    fun setInterval(interval: StartInterval) {
        viewModelScope.launch { prefs.setIntervalSeconds(interval.seconds) }
    }

    fun addChain() {
        viewModelScope.launch {
            val count = chains.value.size
            val chainId = repo.insertChain(sortOrder = count)
            val intervalSecs = intervalSeconds.value
            // Default: single START_BEEP anchor at the interval boundary
            repo.insertAnnouncement(
                Announcement(
                    chainId = chainId,
                    sortOrder = 0,
                    isAnchor = true,
                    anchorOffsetSeconds = intervalSecs,
                    type = AnnouncementType.START_BEEP,
                    estimatedDurationMs = 1000
                )
            )
        }
    }

    fun deleteChain(chain: AnnouncementChain) {
        viewModelScope.launch { repo.deleteChain(chain) }
    }

    fun addAnnouncement(chainId: Long, type: AnnouncementType, afterSortOrder: Int) {
        viewModelScope.launch {
            val chain = chains.value.firstOrNull { it.id == chainId } ?: return@launch
            // Insert after the given sort order
            val newSortOrder = afterSortOrder + 1
            // Shift existing announcements
            for (a in chain.announcements.filter { it.sortOrder >= newSortOrder }) {
                repo.updateAnnouncement(a.copy(sortOrder = a.sortOrder + 1))
            }
            val durationEstimate = when (type) {
                AnnouncementType.START_BEEP -> 1000L
                AnnouncementType.COUNTDOWN_BEEPS -> 5_000L
                AnnouncementType.STARTER_NAMES -> 3_000L
                AnnouncementType.TIME -> 2_000L
                AnnouncementType.TEXT -> 2_000L
                AnnouncementType.RECORDED_CLIP -> 2_000L
            }
            repo.insertAnnouncement(
                Announcement(
                    chainId = chainId,
                    sortOrder = newSortOrder,
                    isAnchor = false,
                    type = type,
                    estimatedDurationMs = durationEstimate
                )
            )
        }
    }

    fun saveAnnouncement(chain: AnnouncementChain, announcement: Announcement) {
        viewModelScope.launch {
            // If this announcement is becoming the anchor, unset any existing anchor in the chain
            if (announcement.isAnchor) {
                chain.announcements
                    .filter { it.isAnchor && it.id != announcement.id }
                    .forEach { repo.updateAnnouncement(it.copy(isAnchor = false)) }
            }
            repo.updateAnnouncement(announcement)
        }
    }

    fun deleteAnnouncement(announcement: Announcement) {
        viewModelScope.launch { repo.deleteAnnouncement(announcement) }
    }

    fun moveAnnouncementUp(chain: AnnouncementChain, announcement: Announcement) {
        viewModelScope.launch {
            val sorted = chain.announcements.sortedBy { it.sortOrder }
            val idx = sorted.indexOfFirst { it.id == announcement.id }
            if (idx <= 0) return@launch
            val other = sorted[idx - 1]
            repo.updateAnnouncement(announcement.copy(sortOrder = other.sortOrder))
            repo.updateAnnouncement(other.copy(sortOrder = announcement.sortOrder))
        }
    }

    fun moveAnnouncementDown(chain: AnnouncementChain, announcement: Announcement) {
        viewModelScope.launch {
            val sorted = chain.announcements.sortedBy { it.sortOrder }
            val idx = sorted.indexOfFirst { it.id == announcement.id }
            if (idx < 0 || idx >= sorted.size - 1) return@launch
            val other = sorted[idx + 1]
            repo.updateAnnouncement(announcement.copy(sortOrder = other.sortOrder))
            repo.updateAnnouncement(other.copy(sortOrder = announcement.sortOrder))
        }
    }

    fun startRecording(announcementId: Long): String {
        val filename = "clip_${announcementId}_${System.currentTimeMillis()}.m4a"
        return audio.startRecording(filename)
    }

    /** Returns actual recorded duration in ms so the caller can update the draft immediately. */
    fun stopRecording(announcement: Announcement, filename: String): Long {
        val durationMs = audio.stopRecording()
        viewModelScope.launch {
            repo.updateAnnouncement(
                announcement.copy(
                    audioFilePath = filename,
                    estimatedDurationMs = if (durationMs > 0) durationMs else announcement.estimatedDurationMs
                )
            )
        }
        return durationMs
    }

    val isRecording: Boolean get() = audio.isRecording

    /** Previews how an announcement will sound, using placeholder data where live context (e.g. a real start time) isn't available. */
    fun playAnnouncement(announcement: Announcement) {
        viewModelScope.launch {
            when (announcement.type) {
                AnnouncementType.TIME -> {
                    val previewMs = System.currentTimeMillis() + announcement.timeOffsetSeconds * 1_000L
                    if (useRecordedTimeSounds.value) {
                        for (asset in RecordedTimeSpeech.assetsFor(previewMs)) audio.playAssetClip(asset)
                    } else {
                        audio.initialize()
                        audio.speak(TimeSpeech.phraseFor(previewMs))
                    }
                }
                AnnouncementType.STARTER_NAMES -> {
                    audio.initialize()
                    audio.speak(app.loadedStarters.firstOrNull()?.name ?: "Example Runner")
                }
                AnnouncementType.TEXT -> {
                    audio.initialize()
                    audio.speak(announcement.text.ifEmpty { "No text set" })
                }
                AnnouncementType.RECORDED_CLIP -> audio.playClip(announcement.audioFilePath)
                AnnouncementType.COUNTDOWN_BEEPS -> BeepGenerator.playCountdownBeeps(announcement.beepCount)
                AnnouncementType.START_BEEP -> BeepGenerator.playStartBeep()
            }
        }
    }

    /** Scheduling estimate for a TIME announcement, using the current moment as a stand-in
     *  for the real next-start time (only the offset's word composition matters for length). */
    fun previewTimeDurationMs(timeOffsetSeconds: Int): Long {
        val previewMs = System.currentTimeMillis() + timeOffsetSeconds * 1_000L
        return if (useRecordedTimeSounds.value) {
            val assets = RecordedTimeSpeech.assetsFor(previewMs)
            assets.sumOf { audio.assetDurationMs(it) } + assets.size * AudioEngine.CLIP_PLAYBACK_OVERHEAD_MS
        } else {
            TimeSpeech.wordsFor(previewMs).sumOf { audio.timeWordDurationMs(it) } + AudioEngine.TTS_CALL_OVERHEAD_MS
        }
    }

    /** Measures the real TTS duration of fixed TEXT-announcement wording (no playback heard)
     *  and reports it back so the caller can update the draft's stored estimate. Includes the
     *  same call overhead ClockEngine accounts for, so a live speak() call has real slack. */
    fun measureTextDuration(text: String, onMeasured: (Long) -> Unit) {
        viewModelScope.launch {
            audio.initialize()
            val ms = audio.measureSpeechDurationMs(text)
            if (ms > 0) onMeasured(ms + AudioEngine.TTS_CALL_OVERHEAD_MS)
        }
    }

    /** Re-reads a recorded clip's real duration from disk, in case the stored estimate is
     *  stale or was never captured correctly right after recording. */
    fun remeasureClipDuration(audioFilePath: String, onMeasured: (Long) -> Unit) {
        if (audioFilePath.isEmpty()) return
        val ms = audio.fileDurationMs(audioFilePath)
        if (ms > 0) onMeasured(ms)
    }
}
