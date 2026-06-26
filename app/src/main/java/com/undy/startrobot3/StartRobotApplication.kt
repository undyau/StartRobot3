package com.undy.startrobot3

import android.app.Application
import android.net.Uri
import com.undy.startrobot3.data.db.AppDatabase
import com.undy.startrobot3.data.prefs.EventPreferences
import com.undy.startrobot3.data.repository.EventRepository
import com.undy.startrobot3.data.model.Starter
import com.undy.startrobot3.engine.AudioEngine
import com.undy.startrobot3.engine.ClockEngine
import com.undy.startrobot3.engine.GpsTimeProvider
import com.undy.startrobot3.iof.IofXmlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

class StartRobotApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val eventRepository by lazy { EventRepository(database) }
    val eventPreferences by lazy { EventPreferences(this) }
    val audioEngine by lazy { AudioEngine(this) }
    val gpsTimeProvider by lazy { GpsTimeProvider(this) }
    val clockEngine by lazy { ClockEngine(audioEngine, gpsTimeProvider) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startersCacheFile by lazy { File(filesDir, "starters_cache.json") }

    private val _loadedStartersFlow = MutableStateFlow<List<Starter>>(emptyList())
    val loadedStartersFlow: StateFlow<List<Starter>> = _loadedStartersFlow.asStateFlow()

    var loadedStarters: List<Starter>
        get() = _loadedStartersFlow.value
        set(value) {
            _loadedStartersFlow.value = value
            appScope.launch {
                if (value.isNotEmpty()) saveStartersToCache(value)
                else withContext(Dispatchers.IO) { runCatching { startersCacheFile.delete() } }
            }
        }

    /** Ensures starters are in memory before the clock starts. Reads from the local cache
     *  first (works offline), falling back to the live URL/file only if the cache is cold. */
    suspend fun ensureStartersLoaded() {
        if (loadedStarters.isNotEmpty()) return
        val cached = loadStartersFromCache()
        if (cached.isNotEmpty()) {
            _loadedStartersFlow.value = cached  // bypass setter — no need to re-write what we just read
            return
        }
        val path = eventPreferences.startListPath.first()
        if (path.isEmpty()) return
        val isUrl = eventPreferences.startListIsUrl.first()
        runCatching {
            loadedStarters = withContext(Dispatchers.IO) {
                if (isUrl) URL(path).openStream().use { IofXmlParser.parse(it) }
                else contentResolver.openInputStream(Uri.parse(path))
                    ?.use { IofXmlParser.parse(it) } ?: emptyList()
            }
        }
    }

    private suspend fun saveStartersToCache(starters: List<Starter>) = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONArray()
            starters.forEach { s ->
                array.put(JSONObject().put("name", s.name).put("startTimeMs", s.startTimeMs))
            }
            startersCacheFile.writeText(array.toString())
        }
    }

    private suspend fun loadStartersFromCache(): List<Starter> = withContext(Dispatchers.IO) {
        runCatching {
            if (!startersCacheFile.exists()) return@runCatching emptyList()
            val array = JSONArray(startersCacheFile.readText())
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Starter(obj.getString("name"), obj.getLong("startTimeMs"))
            }
        }.getOrDefault(emptyList())
    }
}
