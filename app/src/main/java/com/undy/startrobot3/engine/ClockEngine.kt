package com.undy.startrobot3.engine

import android.os.SystemClock
import com.undy.startrobot3.data.model.Announcement
import com.undy.startrobot3.data.model.AnnouncementChain
import com.undy.startrobot3.data.model.AnnouncementType
import com.undy.startrobot3.data.model.RecordedTimeSpeech
import com.undy.startrobot3.data.model.Starter
import com.undy.startrobot3.data.model.TimeSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class ClockEngine(private val audio: AudioEngine, private val gpsTime: GpsTimeProvider) {

    data class State(
        val isRunning: Boolean = false,
        val currentTimeMs: Long = System.currentTimeMillis(),
        val nextStartTimeMs: Long = 0L,
        val nextStarters: List<String> = emptyList()
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var scope: CoroutineScope? = null

    // Persisted across restarts for adjustDelay support
    private var savedIntervalSeconds: Int = 60
    private var savedChains: List<AnnouncementChain> = emptyList()
    private var savedStarters: List<Starter> = emptyList()
    private var savedDelayMinutes: Int = 0
    private var savedUseRecordedTimeSounds: Boolean = false

    fun start(
        intervalSeconds: Int,
        chains: List<AnnouncementChain>,
        starters: List<Starter>,
        delayMinutes: Int = 0,
        useRecordedTimeSounds: Boolean = false
    ) {
        savedIntervalSeconds = intervalSeconds
        savedChains = chains
        savedStarters = starters
        savedDelayMinutes = delayMinutes
        savedUseRecordedTimeSounds = useRecordedTimeSounds
        gpsTime.start()
        launchClock()
    }

    fun stop() {
        scope?.cancel()
        scope = null
        gpsTime.stop()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun adjustDelay(deltaMinutes: Int) {
        savedDelayMinutes += deltaMinutes
        if (_state.value.isRunning) launchClock() // restart from new boundary
    }

    private fun launchClock() {
        scope?.cancel()
        val intervalMs = savedIntervalSeconds * 1_000L
        val nowWall = gpsTime.nowMs()
        val nowElapsed = SystemClock.elapsedRealtime()
        // "Operating time" (real time corrected by the delay) is the system's notion of
        // truth: the schedule snaps to the normal interval grid in that corrected time,
        // and the elapsed wait must be measured from operating-now, not real-now, so the
        // real-world fire moment lines up with when the (delay-corrected) displayed clock
        // actually reaches the boundary.
        val operatingNow = nowWall + savedDelayMinutes * 60_000L
        val firstBoundaryMs = ((operatingNow / intervalMs) + 1) * intervalMs
        val firstBoundaryElapsed = nowElapsed + (firstBoundaryMs - operatingNow)

        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope!!.launch { tickClock() }
        scope!!.launch {
            _state.value = _state.value.copy(isRunning = true, nextStartTimeMs = firstBoundaryMs)
            runClock(intervalMs, firstBoundaryElapsed, firstBoundaryMs,
                savedChains, savedStarters)
        }
    }

    private suspend fun tickClock() {
        while (true) {
            _state.value = _state.value.copy(
                currentTimeMs = gpsTime.nowMs() + savedDelayMinutes * 60_000L
            )
            delay(500)
        }
    }

    private suspend fun runClock(
        intervalMs: Long,
        firstBoundaryElapsed: Long,
        firstBoundaryMs: Long,
        chains: List<AnnouncementChain>,
        starters: List<Starter>
    ) {
        var n = 0L
        while (true) {
            val boundaryElapsed = firstBoundaryElapsed + n * intervalMs
            val boundaryMs = firstBoundaryMs + n * intervalMs
            val intervalStartElapsed = boundaryElapsed - intervalMs

            // Show this interval's end boundary as the upcoming start
            val nextStarters = starters
                .filter { abs(it.startTimeMs - boundaryMs) < 5_000L }
                .map { it.name }
            _state.value = _state.value.copy(
                nextStartTimeMs = boundaryMs,
                nextStarters = nextStarters
            )

            // Schedule all chains — they fire at their offsets within this interval
            for (chain in chains) {
                scope?.launch {
                    scheduleChain(chain, intervalStartElapsed, boundaryElapsed, boundaryMs, starters)
                }
            }

            // Wait until this boundary fires
            val untilBoundary = boundaryElapsed - SystemClock.elapsedRealtime()
            if (untilBoundary > 0) delay(untilBoundary)

            // Boundary just fired — flip display to next boundary immediately
            _state.value = _state.value.copy(nextStartTimeMs = boundaryMs + intervalMs)

            // Loop immediately: n+1 iteration starts right at its interval start,
            // giving a full interval of lead time for all announcements
            n++
        }
    }

    private suspend fun scheduleChain(
        chain: AnnouncementChain,
        intervalStartElapsed: Long,
        intervalEndElapsed: Long,
        intervalEndMs: Long,
        starters: List<Starter>
    ) {
        val sorted = chain.announcements.sortedBy { it.sortOrder }
        val anchorIdx = sorted.indexOfFirst { it.isAnchor }
        if (anchorIdx < 0) return

        val anchor = sorted[anchorIdx]
        val anchorElapsed = intervalStartElapsed + anchor.anchorOffsetSeconds * 1_000L

        val times = LongArray(sorted.size)
        times[anchorIdx] = anchorElapsed

        var t = anchorElapsed
        for (i in anchorIdx - 1 downTo 0) {
            t -= sorted[i].schedulingDurationMs(intervalEndMs)
            times[i] = t
        }
        t = anchorElapsed + sorted[anchorIdx].schedulingDurationMs(intervalEndMs)
        for (i in anchorIdx + 1 until sorted.size) {
            times[i] = t
            t += sorted[i].schedulingDurationMs(intervalEndMs)
        }

        coroutineScope {
            // Items before the anchor play on their own timeline. If one overruns its
            // estimated duration, the anchor below truncates it rather than waiting.
            val beforeAnchor = launch {
                for (i in 0 until anchorIdx) {
                    if (SystemClock.elapsedRealtime() >= anchorElapsed) break
                    val waitMs = times[i] - SystemClock.elapsedRealtime()
                    if (waitMs < -5_000L) continue
                    if (waitMs > 0) delay(waitMs)
                    if (SystemClock.elapsedRealtime() >= anchorElapsed) break
                    playAnnouncement(sorted[i], intervalEndMs, starters)
                }
            }

            val waitForAnchor = anchorElapsed - SystemClock.elapsedRealtime()
            if (waitForAnchor > 0) delay(waitForAnchor)
            audio.stopCurrent()
            // cancelAndJoin (not cancel) — otherwise beforeAnchor can still be mid-flight and
            // race to grab the audio mutex for another item just as the anchor tries to play.
            beforeAnchor.cancelAndJoin()
            playAnnouncement(anchor, intervalEndMs, starters)

            for (i in anchorIdx + 1 until sorted.size) {
                val waitMs = times[i] - SystemClock.elapsedRealtime()
                if (waitMs < -5_000L) continue
                if (waitMs > 0) delay(waitMs)
                playAnnouncement(sorted[i], intervalEndMs, starters)
            }
        }
    }

    private suspend fun playAnnouncement(
        a: Announcement,
        nextStartMs: Long,
        starters: List<Starter>
    ) {
        when (a.type) {
            AnnouncementType.TIME -> {
                val ms = nextStartMs + a.timeOffsetSeconds * 1_000L
                if (savedUseRecordedTimeSounds) {
                    for (asset in RecordedTimeSpeech.assetsFor(ms)) audio.playAssetClip(asset)
                } else {
                    audio.speak(TimeSpeech.phraseFor(ms))
                }
            }
            AnnouncementType.STARTER_NAMES -> {
                starters
                    .filter { abs(it.startTimeMs - nextStartMs) < 5_000L }
                    .forEach { audio.speak(it.name) }
            }
            AnnouncementType.TEXT -> audio.speak(a.text)
            AnnouncementType.RECORDED_CLIP -> audio.playClip(a.audioFilePath)
            AnnouncementType.COUNTDOWN_BEEPS -> BeepGenerator.playCountdownBeeps(a.beepCount)
            AnnouncementType.START_BEEP -> BeepGenerator.playStartBeep()
        }
    }

    /** Scheduling duration for an announcement, given the interval boundary it's attached to.
     *  TIME is computed from either the calibrated TTS word durations or the bundled
     *  recordings' real durations (whichever is active) for what it will actually play, plus
     *  per-call overhead since neither measurement captures real-world playback startup
     *  latency; everything else uses Announcement.effectiveDurationMs(). */
    private fun Announcement.schedulingDurationMs(intervalEndMs: Long): Long =
        if (type == AnnouncementType.TIME) {
            val ms = intervalEndMs + timeOffsetSeconds * 1_000L
            if (savedUseRecordedTimeSounds) {
                val assets = RecordedTimeSpeech.assetsFor(ms)
                assets.sumOf { audio.assetDurationMs(it) } +
                    assets.size * AudioEngine.CLIP_PLAYBACK_OVERHEAD_MS
            } else {
                TimeSpeech.wordsFor(ms).sumOf { audio.timeWordDurationMs(it) } +
                    AudioEngine.TTS_CALL_OVERHEAD_MS
            }
        } else {
            effectiveDurationMs()
        }
}
