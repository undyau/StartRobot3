package com.undy.startrobot3.ui.run

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undy.startrobot3.StartRobotApplication
import com.undy.startrobot3.data.model.Starter
import com.undy.startrobot3.engine.ClockEngine
import com.undy.startrobot3.service.ClockService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StartRobotApplication
    private val engine = app.clockEngine
    private val prefs = app.eventPreferences
    private val repo = app.eventRepository

    val clockState: StateFlow<ClockEngine.State> = engine.state

    /** Same GPS-derived time source the clock engine schedules announcements against, so the
     *  displayed clock never disagrees with when the anchor actually fires. */
    fun currentTimeMs(): Long = app.gpsTimeProvider.nowMs()

    /** Start acquiring a GPS fix as soon as this screen is viewed, not just once the clock
     *  is started, so the displayed time (and the first announcement) are accurate from the start. */
    fun ensureGpsTracking() = app.gpsTimeProvider.start()

    val delayMinutes = prefs.delayMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Starters loaded by StartListViewModel and stored in application scope
    var loadedStarters: List<Starter>
        get() = app.loadedStarters
        set(value) { app.loadedStarters = value }

    fun startClock() {
        viewModelScope.launch {
            val intervalSeconds = prefs.intervalSeconds.first()
            val chains = repo.chains.first()
            val delay = prefs.delayMinutes.first()
            val useRecordedTimeSounds = prefs.useRecordedTimeSounds.first()

            // Initialize TTS if not done
            app.audioEngine.initialize()

            // Start foreground service
            val svcIntent = Intent(getApplication(), ClockService::class.java).apply {
                action = ClockService.ACTION_START_FOREGROUND
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(svcIntent)
            } else {
                getApplication<Application>().startService(svcIntent)
            }

            engine.start(intervalSeconds, chains, loadedStarters, delay, useRecordedTimeSounds)
        }
    }

    fun stopClock() {
        engine.stop()
        val svcIntent = Intent(getApplication(), ClockService::class.java).apply {
            action = ClockService.ACTION_STOP
        }
        getApplication<Application>().startService(svcIntent)
    }

    fun adjustDelay(deltaMinutes: Int) {
        viewModelScope.launch {
            val current = prefs.delayMinutes.first()
            prefs.setDelayMinutes(current + deltaMinutes)
            if (engine.state.value.isRunning) {
                engine.adjustDelay(deltaMinutes)
            }
        }
    }
}
