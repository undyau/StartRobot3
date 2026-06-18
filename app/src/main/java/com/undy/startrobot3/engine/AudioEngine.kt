package com.undy.startrobot3.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.undy.startrobot3.data.model.TimeSpeech
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Locale
import java.util.UUID

class AudioEngine(private val context: Context) {

    companion object {
        // synthesizeToFile()-measured durations only capture the rendered audio's own length —
        // a live speak() call has extra real-world synthesis/startup latency before audio
        // actually starts, which isn't reflected there. Callers computing scheduling estimates
        // for TTS should add this per speak() call so backward-scheduled items get real slack.
        const val TTS_CALL_OVERHEAD_MS = 1000L

        // Each playClip()/playAssetClip() call spins up its own MediaPlayer (setDataSource +
        // prepareAsync + start), which has its own (smaller) setup latency per clip.
        const val CLIP_PLAYBACK_OVERHEAD_MS = 150L
    }

    private var tts: TextToSpeech? = null
    private var recorder: MediaRecorder? = null
    private var lastRecordingFile: String? = null
    private val mutex = Mutex()

    // Tracked so stopCurrent() can truncate an in-flight speak()/playClip() call from
    // outside the mutex, letting a higher-priority anchor announcement preempt it.
    @Volatile private var currentMediaPlayer: MediaPlayer? = null
    @Volatile private var currentDone: CompletableDeferred<Unit>? = null

    // Cache of measured per-word TTS durations (device's default voice), so a TIME
    // announcement's scheduling estimate can be computed instead of guessed.
    private val wordDurationPrefs by lazy {
        context.getSharedPreferences("tts_word_durations", Context.MODE_PRIVATE)
    }

    var isRecording = false
        private set

    suspend fun initialize() {
        if (tts != null) return
        val ready = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                }
                ready.complete(Unit)
            }
        }
        ready.await()
        calibrateTimeWordsIfNeeded()
    }

    /** Measures each word used to speak a time (see TimeSpeech) exactly once, the first
     *  time the app runs on this device/voice, and caches the results permanently. */
    private suspend fun calibrateTimeWordsIfNeeded() {
        for (word in TimeSpeech.ALL_WORDS) {
            if (wordDurationPrefs.contains(word)) continue
            val ms = measureSpeechDurationMs(word)
            if (ms > 0) wordDurationPrefs.edit().putLong(word, ms).apply()
        }
    }

    /** Cached duration of a single calibrated time-word; falls back to a rough guess
     *  if calibration hasn't completed yet (e.g. it's still running). */
    fun timeWordDurationMs(word: String): Long =
        wordDurationPrefs.getLong(word, 400L)

    /** Renders text to a temporary file without playing it audibly, then measures the
     *  real resulting duration. Used for time-word calibration and TEXT-announcement previews. */
    suspend fun measureSpeechDurationMs(text: String): Long = mutex.withLock {
        val t = tts ?: return@withLock -1L
        val file = File(context.cacheDir, "measure_${UUID.randomUUID()}.wav")
        val done = CompletableDeferred<Unit>()
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.Main) {
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(u: String?) {}
                override fun onDone(u: String?) { done.complete(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(u: String?) { done.complete(Unit) }
                override fun onError(u: String?, errorCode: Int) { done.complete(Unit) }
            })
            if (t.synthesizeToFile(text, null, file, id) == TextToSpeech.ERROR) {
                done.complete(Unit)
            }
        }
        withTimeoutOrNull(15_000L) { done.await() }
        val durationMs = fileDurationMs(file)
        file.delete()
        durationMs
    }

    suspend fun speak(text: String) = mutex.withLock {
        val t = tts ?: return@withLock
        val done = CompletableDeferred<Unit>()
        currentDone = done
        val id = UUID.randomUUID().toString()
        withContext(Dispatchers.Main) {
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(u: String?) {}
                override fun onDone(u: String?) { done.complete(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(u: String?) { done.complete(Unit) }
                override fun onError(u: String?, errorCode: Int) { done.complete(Unit) }
            })
            if (t.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
                done.complete(Unit)
            }
        }
        withTimeoutOrNull(30_000L) { done.await() }
        currentDone = null
    }

    suspend fun playClip(relativePath: String) {
        val file = File(recordingsDir(), relativePath)
        if (!file.exists()) return
        playFromMediaPlayer { it.setDataSource(file.absolutePath) }
    }

    /** Plays a bundled asset (e.g. a pre-recorded time-announcement clip) instead of a
     *  user-recorded one in filesDir/recordings. */
    suspend fun playAssetClip(assetPath: String) {
        val afd = try { context.assets.openFd(assetPath) } catch (_: Exception) { return }
        try {
            playFromMediaPlayer { it.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length) }
        } finally {
            afd.close()
        }
    }

    private suspend fun playFromMediaPlayer(setSource: (MediaPlayer) -> Unit) = mutex.withLock {
        val done = CompletableDeferred<Unit>()
        currentDone = done
        withContext(Dispatchers.Main) {
            val mp = MediaPlayer()
            currentMediaPlayer = mp
            try {
                setSource(mp)
                mp.setOnCompletionListener {
                    if (currentMediaPlayer === it) currentMediaPlayer = null
                    it.release(); done.complete(Unit)
                }
                mp.setOnErrorListener { it, _, _ ->
                    if (currentMediaPlayer === it) currentMediaPlayer = null
                    it.release(); done.complete(Unit); true
                }
                mp.prepareAsync()
                mp.setOnPreparedListener { it.start() }
            } catch (e: Exception) {
                currentMediaPlayer = null
                mp.release()
                done.complete(Unit)
            }
        }
        withTimeoutOrNull(60_000L) { done.await() }
        currentDone = null
    }

    // Bundled asset clips never change at runtime, so their durations are cached in memory
    // for the life of the process rather than persisted.
    private val assetDurationCache = mutableMapOf<String, Long>()

    /** Real duration of a bundled time-sound asset, for chain-scheduling purposes. */
    fun assetDurationMs(assetPath: String): Long = assetDurationCache.getOrPut(assetPath) {
        val afd = try { context.assets.openFd(assetPath) } catch (_: Exception) { return 0L }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
            afd.close()
        }
    }

    /** Immediately halts whatever is currently speaking/playing, so a higher-priority
     *  announcement (the anchor) can start exactly on schedule instead of waiting its turn. */
    fun stopCurrent() {
        tts?.stop()
        currentMediaPlayer?.let { mp ->
            try { mp.stop() } catch (_: Exception) {}
            try { mp.release() } catch (_: Exception) {}
        }
        currentMediaPlayer = null
        currentDone?.complete(Unit)
        currentDone = null
    }

    fun startRecording(filename: String): String {
        stopRecording()
        lastRecordingFile = filename
        val dir = recordingsDir()
        dir.mkdirs()
        val file = File(dir, filename)
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        isRecording = true
        return filename
    }

    /** Stops recording and returns the actual duration of the recorded file in ms. */
    fun stopRecording(): Long {
        try { recorder?.stop() } catch (_: Exception) {}
        recorder?.release()
        recorder = null
        isRecording = false
        return lastRecordingFile?.let { fileDurationMs(it) } ?: 0L
    }

    fun fileDurationMs(filename: String): Long = fileDurationMs(File(recordingsDir(), filename))

    // Right after MediaRecorder.stop(), the file's duration metadata can momentarily read as
    // 0 on some devices before it's fully flushed to disk — retry briefly rather than accept
    // a wrong/missing duration.
    private fun fileDurationMs(file: File): Long {
        if (!file.exists()) return 0L
        repeat(5) { attempt ->
            val retriever = MediaMetadataRetriever()
            val ms = try {
                retriever.setDataSource(file.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } catch (_: Exception) {
                0L
            } finally {
                retriever.release()
            }
            if (ms > 0) return ms
            if (attempt < 4) Thread.sleep(40)
        }
        return 0L
    }

    fun recordingsDir(): File = File(context.filesDir, "recordings")

    fun shutdown() {
        stopRecording()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
