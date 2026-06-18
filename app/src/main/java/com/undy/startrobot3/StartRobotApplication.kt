package com.undy.startrobot3

import android.app.Application
import com.undy.startrobot3.data.db.AppDatabase
import com.undy.startrobot3.data.prefs.EventPreferences
import com.undy.startrobot3.data.repository.EventRepository
import com.undy.startrobot3.data.model.Starter
import com.undy.startrobot3.engine.AudioEngine
import com.undy.startrobot3.engine.ClockEngine
import com.undy.startrobot3.engine.GpsTimeProvider

class StartRobotApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val eventRepository by lazy { EventRepository(database) }
    val eventPreferences by lazy { EventPreferences(this) }
    val audioEngine by lazy { AudioEngine(this) }
    val gpsTimeProvider by lazy { GpsTimeProvider(this) }
    val clockEngine by lazy { ClockEngine(audioEngine, gpsTimeProvider) }
    var loadedStarters: List<Starter> = emptyList()
}
