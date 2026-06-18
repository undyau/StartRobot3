package com.undy.startrobot3.engine

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * Tracks wall-clock time from GPS fixes instead of the device's system clock. GPS time comes
 * from satellite atomic clocks, so it stays correct even when the phone has no cell/network
 * signal to sync against (the common case for orienteering events in the field).
 */
class GpsTimeProvider(private val context: Context) {

    // Last GPS fix's UTC time, paired with the elapsedRealtime at which it was taken, so
    // nowMs() can extrapolate forward using the monotonic clock without needing a fix every tick.
    @Volatile private var gpsEpochMsAtFix: Long = 0L
    @Volatile private var elapsedRealtimeAtFixMs: Long = 0L

    val hasFix: Boolean get() = gpsEpochMsAtFix != 0L

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val listener = LocationListener { location -> onLocation(location) }

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener)
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { onLocation(it) }
            }
        } catch (_: SecurityException) {
        }
    }

    fun stop() {
        try { locationManager.removeUpdates(listener) } catch (_: SecurityException) {}
    }

    private fun onLocation(location: Location) {
        gpsEpochMsAtFix = location.time
        elapsedRealtimeAtFixMs = location.elapsedRealtimeNanos / 1_000_000L
    }

    /** Best current wall-clock estimate: the last GPS fix's time, extrapolated forward by the
     *  monotonic elapsed-time clock. Falls back to the device clock if no fix has arrived yet. */
    fun nowMs(): Long =
        if (hasFix) gpsEpochMsAtFix + (SystemClock.elapsedRealtime() - elapsedRealtimeAtFixMs)
        else System.currentTimeMillis()
}
