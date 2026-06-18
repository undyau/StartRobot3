package com.undy.startrobot3.ui.run

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val delayMinutes by vm.delayMinutes.collectAsState()
    var showDelayControls by remember { mutableStateOf(false) }

    var currentTimeMs by remember { mutableStateOf(vm.currentTimeMs()) }
    LaunchedEffect(Unit) {
        vm.ensureGpsTracking()
        while (true) {
            currentTimeMs = vm.currentTimeMs()
            kotlinx.coroutines.delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OutdoorBackground)
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current time
        Text(
            text = clockTimeFormat.format(Date(currentTimeMs)),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = ClockYellow
        )

        Spacer(Modifier.height(8.dp))

        // Next start time
        if (clockState.isRunning && clockState.nextStartTimeMs > 0) {
            Text(
                text = "Next start: ${clockTimeFormat.format(Date(clockState.nextStartTimeMs))}",
                style = MaterialTheme.typography.titleMedium,
                color = ClockGreen
            )
        }

        Spacer(Modifier.height(16.dp))

        // Next starters
        if (clockState.nextStarters.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelBackground)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Next starters",
                        style = MaterialTheme.typography.labelMedium,
                        color = DimText
                    )
                    Spacer(Modifier.height(4.dp))
                    clockState.nextStarters.forEach { name ->
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

        Spacer(Modifier.height(24.dp))

        // Start / Stop button
        if (clockState.isRunning) {
            Button(
                onClick = { vm.stopClock() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StopRed,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("  Start Clock", fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = DimText)

        // Delay toggle row — always visible but unobtrusive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (delayMinutes == 0) "No delay"
                       else if (delayMinutes > 0) "Delay: +$delayMinutes min"
                       else "Delay: $delayMinutes min",
                style = MaterialTheme.typography.bodySmall,
                color = if (delayMinutes != 0) StopRed else DimText
            )
            TextButton(onClick = { showDelayControls = !showDelayControls }) {
                Text(
                    if (showDelayControls) "Hide" else "Adjust delay",
                    style = MaterialTheme.typography.bodySmall,
                    color = ClockYellow
                )
                Spacer(Modifier.size(4.dp))
                Icon(
                    if (showDelayControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ClockYellow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Delay controls — hidden by default
        AnimatedVisibility(visible = showDelayControls) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.adjustDelay(-1) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClockYellow),
                        border = BorderStroke(1.dp, DimText),
                        modifier = Modifier.weight(1f)
                    ) { Text("− 1 min") }
                    OutlinedButton(
                        onClick = { vm.adjustDelay(1) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClockYellow),
                        border = BorderStroke(1.dp, DimText),
                        modifier = Modifier.weight(1f)
                    ) { Text("+ 1 min") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.adjustDelay(-5) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClockYellow),
                        border = BorderStroke(1.dp, DimText),
                        modifier = Modifier.weight(1f)
                    ) { Text("− 5 min") }
                    OutlinedButton(
                        onClick = { vm.adjustDelay(5) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClockYellow),
                        border = BorderStroke(1.dp, DimText),
                        modifier = Modifier.weight(1f)
                    ) { Text("+ 5 min") }
                }
                FilledTonalButton(
                    onClick = { vm.adjustDelay(-delayMinutes) },
                    enabled = delayMinutes != 0,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PanelBackground,
                        contentColor = ClockYellow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reset delay") }
            }
        }
    }
    }
}
