package com.undy.startrobot3.ui.run

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.undy.startrobot3.engine.ClockEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// High-contrast outdoor palette — fixed regardless of system/dynamic theme, since this
// screen needs to stay readable in direct sunlight rather than follow the app's normal look.
private val OutdoorBackground = Color.Black
private val ClockYellow = Color(0xFFFFD600)
private val ClockGreen = Color(0xFF00E676)
private val DimText = Color(0xFFB0B0B0)
private val PanelBackground = Color(0xFF1A1A1A)
private val StopRed = Color(0xFFFF5252)

// 12-hour clock without AM/PM — obvious from context, and saves screen space.
private val clockTimeFormat = SimpleDateFormat("h:mm:ss", Locale.getDefault())

@Composable
fun RunScreen(vm: RunViewModel = viewModel()) {
    val clockState by vm.clockState.collectAsState()

    var currentTimeMs by remember { mutableStateOf(vm.currentTimeMs()) }
    var gpsHasFix by remember { mutableStateOf(vm.gpsHasFix()) }
    var gpsSatellitesUsed by remember { mutableStateOf(vm.gpsSatellitesUsed()) }
    LaunchedEffect(Unit) {
        vm.ensureGpsTracking()
        while (true) {
            currentTimeMs = vm.currentTimeMs()
            gpsHasFix = vm.gpsHasFix()
            gpsSatellitesUsed = vm.gpsSatellitesUsed()
            // Wake up right after the next second boundary instead of a fixed 500ms — a fixed
            // delay drifts out of phase with the actual clock (render overhead, GC pauses), which
            // made the displayed second occasionally appear to tick over early or late.
            val msUntilNextSecond = 1000 - (currentTimeMs % 1000)
            kotlinx.coroutines.delay(msUntilNextSecond)
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OutdoorBackground)
    ) {
        if (isLandscape) {
            LandscapeRunLayout(clockState = clockState, currentTimeMs = currentTimeMs, vm = vm)
        } else {
            PortraitRunLayout(clockState = clockState, currentTimeMs = currentTimeMs, vm = vm)
        }

        GpsIndicator(
            hasFix = gpsHasFix,
            satellitesUsed = gpsSatellitesUsed,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        )
    }
}

@Composable
private fun GpsIndicator(hasFix: Boolean, satellitesUsed: Int, modifier: Modifier = Modifier) {
    val (icon, color) = when {
        !hasFix -> Icons.Default.GpsOff to StopRed
        satellitesUsed < 4 -> Icons.Default.GpsNotFixed to ClockYellow
        else -> Icons.Default.GpsFixed to ClockGreen
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = "GPS status", tint = color, modifier = Modifier.size(16.dp))
        if (hasFix) {
            Spacer(Modifier.width(4.dp))
            Text(satellitesUsed.toString(), style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
private fun PortraitRunLayout(
    clockState: ClockEngine.State,
    currentTimeMs: Long,
    vm: RunViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClockDisplay(currentTimeMs = currentTimeMs, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))

        NextStartText(clockState)

        Spacer(Modifier.height(16.dp))

        StartersCard(clockState.nextStarters, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))

        StartStopButton(clockState, vm, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun LandscapeRunLayout(
    clockState: ClockEngine.State,
    currentTimeMs: Long,
    vm: RunViewModel
) {
    val hasStarters = clockState.nextStarters.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = if (hasStarters) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = if (hasStarters) {
                Modifier.weight(0.62f).fillMaxHeight()
            } else {
                Modifier.fillMaxWidth(0.7f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ClockDisplay(currentTimeMs = currentTimeMs, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))

            NextStartText(clockState)

            Spacer(Modifier.height(16.dp))

            StartStopButton(clockState, vm, modifier = Modifier.fillMaxWidth())
        }

        if (hasStarters) {
            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                StartersCard(
                    clockState.nextStarters,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ClockDisplay(currentTimeMs: Long, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        AutoSizeClockText(
            text = clockTimeFormat.format(Date(currentTimeMs)),
            color = ClockYellow,
            availableWidth = maxWidth
        )
    }
}

/** Grows the clock to fill [availableWidth], shrinking only as far as needed to avoid
 *  clipping — so the clock is always as large as the layout allows. */
@Composable
private fun AutoSizeClockText(
    text: String,
    color: Color,
    availableWidth: Dp,
    maxFontSize: TextUnit = 220.sp,
    minFontSize: TextUnit = 36.sp
) {
    var fontSize by remember(availableWidth) { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize) {
                fontSize = (fontSize.value - 4f).coerceAtLeast(minFontSize.value).sp
            }
        }
    )
}

@Composable
private fun NextStartText(clockState: ClockEngine.State) {
    if (clockState.isRunning && clockState.nextStartTimeMs > 0) {
        Text(
            text = "Next start: ${clockTimeFormat.format(Date(clockState.nextStartTimeMs))}",
            style = MaterialTheme.typography.titleMedium,
            color = ClockGreen
        )
    }
}

@Composable
private fun StartersCard(names: List<String>, modifier: Modifier = Modifier) {
    if (names.isEmpty()) return
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Next starters",
                style = MaterialTheme.typography.labelMedium,
                color = DimText
            )
            Spacer(Modifier.height(4.dp))
            names.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ClockGreen
                )
            }
        }
    }
}

@Composable
private fun StartStopButton(clockState: ClockEngine.State, vm: RunViewModel, modifier: Modifier = Modifier) {
    if (clockState.isRunning) {
        Button(
            onClick = { vm.stopClock() },
            colors = ButtonDefaults.buttonColors(
                containerColor = StopRed,
                contentColor = Color.Black
            ),
            modifier = modifier
        ) {
            Icon(Icons.Default.Pause, contentDescription = null)
            Text("  Stop Clock", fontSize = 18.sp)
        }
    } else {
        Button(
            onClick = { vm.startClock() },
            colors = ButtonDefaults.buttonColors(
                containerColor = ClockGreen,
                contentColor = Color.Black
            ),
            modifier = modifier
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text("  Start Clock", fontSize = 18.sp)
        }
    }
}
