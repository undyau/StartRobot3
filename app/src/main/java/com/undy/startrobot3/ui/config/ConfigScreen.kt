package com.undy.startrobot3.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.undy.startrobot3.data.model.Announcement
import com.undy.startrobot3.data.model.AnnouncementChain
import com.undy.startrobot3.data.model.AnnouncementType
import com.undy.startrobot3.data.model.StartInterval

private fun formatSeconds(ms: Long): String {
    val seconds = ms / 1000.0
    return if (seconds == seconds.toLong().toDouble()) "${seconds.toLong()}s"
        else "%.2fs".format(seconds)
}

private fun formatDurationLabel(announcement: Announcement, vm: ConfigViewModel): String {
    val ms = if (announcement.type == AnnouncementType.TIME)
        vm.previewTimeDurationMs(announcement.timeOffsetSeconds)
    else announcement.effectiveDurationMs()
    val formatted = formatSeconds(ms)
    return when (announcement.type) {
        AnnouncementType.START_BEEP, AnnouncementType.COUNTDOWN_BEEPS, AnnouncementType.TIME -> formatted
        else -> "~$formatted"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(vm: ConfigViewModel = viewModel()) {
    val chains by vm.chains.collectAsState()
    val intervalLabel by vm.intervalLabel.collectAsState()
    val intervalSeconds by vm.intervalSeconds.collectAsState()
    val useRecordedTimeSounds by vm.useRecordedTimeSounds.collectAsState()
    var showIntervalMenu by remember { mutableStateOf(false) }
    var showRecordedSoundsPasswordDialog by remember { mutableStateOf(false) }
    var recordedSoundsPasswordInput by remember { mutableStateOf("") }
    var recordedSoundsPasswordError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Event Configuration", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = showIntervalMenu,
                onExpandedChange = { showIntervalMenu = it }
            ) {
                OutlinedTextField(
                    value = intervalLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Start interval") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showIntervalMenu) },
                    modifier = Modifier.fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = showIntervalMenu,
                    onDismissRequest = { showIntervalMenu = false }
                ) {
                    StartInterval.entries.forEach { interval ->
                        DropdownMenuItem(
                            text = { Text(interval.label) },
                            onClick = { vm.setInterval(interval); showIntervalMenu = false }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Use recorded number sounds for time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = useRecordedTimeSounds,
                    onCheckedChange = { checked ->
                        if (checked) {
                            recordedSoundsPasswordInput = ""
                            recordedSoundsPasswordError = false
                            showRecordedSoundsPasswordDialog = true
                        } else {
                            vm.setUseRecordedTimeSounds(false)
                        }
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Announcement Chains", style = MaterialTheme.typography.titleMedium)
                FilledTonalButton(onClick = { vm.addChain() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Add chain")
                }
            }
        }

        items(chains, key = { it.id }) { chain ->
            ChainCard(chain, vm, intervalSeconds)
        }

        item { Spacer(Modifier.height(80.dp)) }
    }

    if (showRecordedSoundsPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRecordedSoundsPasswordDialog = false },
            title = { Text("Enable recorded time sounds") },
            text = {
                Column {
                    OutlinedTextField(
                        value = recordedSoundsPasswordInput,
                        onValueChange = {
                            recordedSoundsPasswordInput = it
                            recordedSoundsPasswordError = false
                        },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = recordedSoundsPasswordError
                    )
                    if (recordedSoundsPasswordError) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Incorrect password",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (recordedSoundsPasswordInput == "bigfoot") {
                        vm.setUseRecordedTimeSounds(true)
                        showRecordedSoundsPasswordDialog = false
                    } else {
                        recordedSoundsPasswordError = true
                    }
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showRecordedSoundsPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChainCard(chain: AnnouncementChain, vm: ConfigViewModel, intervalSeconds: Int) {
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chain (${chain.anchorLabel()})", style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { vm.deleteChain(chain) }) {
                    Icon(Icons.Default.Delete, "Delete chain",
                        tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            chain.announcements.sortedBy { it.sortOrder }.forEach { announcement ->
                AnnouncementRow(
                    announcement = announcement,
                    chain = chain,
                    vm = vm,
                    onEdit = { editingAnnouncement = it }
                )
            }

            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = {
                    val maxSort = chain.announcements.maxOfOrNull { it.sortOrder } ?: -1
                    vm.addAnnouncement(chain.id, AnnouncementType.TEXT, maxSort)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(" Add announcement")
            }
        }
    }

    editingAnnouncement?.let { a ->
        AnnouncementEditDialog(
            announcement = a,
            chain = chain,
            intervalSeconds = intervalSeconds,
            vm = vm,
            onDismiss = { editingAnnouncement = null }
        )
    }
}

@Composable
private fun AnnouncementRow(
    announcement: Announcement,
    chain: AnnouncementChain,
    vm: ConfigViewModel,
    onEdit: (Announcement) -> Unit
) {
    val anchorLabel = if (announcement.isAnchor)
        " [anchor at ${announcement.anchorOffsetSeconds}s]" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (announcement.isAnchor) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                announcement.displayLabel() + anchorLabel,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                formatDurationLabel(announcement, vm),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { vm.playAnnouncement(announcement) }) {
            Icon(Icons.Default.PlayArrow, "Play", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { vm.moveAnnouncementUp(chain, announcement) }) {
            Icon(Icons.Default.KeyboardArrowUp, "Move up")
        }
        IconButton(onClick = { vm.moveAnnouncementDown(chain, announcement) }) {
            Icon(Icons.Default.KeyboardArrowDown, "Move down")
        }
        IconButton(onClick = { onEdit(announcement) }) {
            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = { vm.deleteAnnouncement(announcement) }) {
            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
        }
    }
}

/** Keeps its own text buffer so the field can be freely cleared/edited — committing
 *  [value] (and snapping back) only on each successful parse would otherwise make it
 *  impossible to delete the existing digits before typing new ones. */
@Composable
private fun IntTextField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: (@Composable () -> Unit)? = null,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v
            v.toIntOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        supportingText = supportingText,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnouncementEditDialog(
    announcement: Announcement,
    chain: AnnouncementChain,
    intervalSeconds: Int,
    vm: ConfigViewModel,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(announcement) }
    var isRecording by remember { mutableStateOf(false) }
    var recordedFilename by remember { mutableStateOf("") }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    // Re-measure TTS duration whenever the spoken text changes, debounced so a real TTS
    // synthesis pass doesn't run on every keystroke.
    LaunchedEffect(draft.type, draft.text) {
        if (draft.type == AnnouncementType.TEXT) {
            kotlinx.coroutines.delay(600)
            vm.measureTextDuration(draft.text) { ms -> draft = draft.copy(estimatedDurationMs = ms) }
        }
    }

    // Re-measure a recorded clip's real duration whenever it changes (new recording, or the
    // dialog opening on an existing clip) — a plain file metadata read, cheap enough to run
    // unconditionally rather than waiting on the manual "Measure duration" button.
    LaunchedEffect(draft.type, draft.audioFilePath) {
        if (draft.type == AnnouncementType.RECORDED_CLIP && draft.audioFilePath.isNotEmpty()) {
            vm.remeasureClipDuration(draft.audioFilePath) { ms -> draft = draft.copy(estimatedDurationMs = ms) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Let the dialog respond to IME insets — otherwise the soft keyboard just covers
        // the bottom of the dialog (including Save/Cancel) instead of pushing it up.
        properties = DialogProperties(decorFitsSystemWindows = false),
        modifier = Modifier.imePadding(),
        title = { Text("Edit Announcement") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type selector
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = draft.type.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AnnouncementType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name.replace('_', ' ').lowercase()
                                    .replaceFirstChar { it.uppercase() }) },
                                onClick = { draft = draft.copy(type = t); typeMenuExpanded = false }
                            )
                        }
                    }
                }

                // Anchor toggle + offset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Anchor point", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Fixed position in interval",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = draft.isAnchor,
                        onCheckedChange = { on ->
                            draft = draft.copy(
                                isAnchor = on,
                                // Default to end of interval when first enabling
                                anchorOffsetSeconds = if (on && !draft.isAnchor) intervalSeconds
                                                      else draft.anchorOffsetSeconds
                            )
                        }
                    )
                }

                if (draft.isAnchor) {
                    IntTextField(
                        value = draft.anchorOffsetSeconds,
                        onValueChange = { draft = draft.copy(anchorOffsetSeconds = it) },
                        label = "Seconds from interval start",
                        supportingText = {
                            Text("0 = after prev beep  •  $intervalSeconds = at start beep")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                FilledTonalButton(onClick = { vm.playAnnouncement(draft) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text(" Test play")
                }

                // Type-specific fields
                when (draft.type) {
                    AnnouncementType.TEXT -> Column {
                        OutlinedTextField(
                            value = draft.text,
                            onValueChange = { draft = draft.copy(text = it) },
                            label = { Text("Text to speak") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            vm.measureTextDuration(draft.text) { ms ->
                                draft = draft.copy(estimatedDurationMs = ms)
                            }
                        }) { Text("Measure duration") }
                    }
                    AnnouncementType.TIME -> Column {
                        IntTextField(
                            value = draft.timeOffsetSeconds,
                            onValueChange = { draft = draft.copy(timeOffsetSeconds = it) },
                            label = "Offset from next start time (s, negative = before)",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Duration: ${formatDurationLabel(draft, vm)} (computed, not editable)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    AnnouncementType.COUNTDOWN_BEEPS -> Column {
                        IntTextField(
                            value = draft.beepCount,
                            onValueChange = { draft = draft.copy(beepCount = it) },
                            label = "Number of beeps",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Duration: ${formatDurationLabel(draft, vm)} (computed, not editable)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    AnnouncementType.START_BEEP -> Text(
                        "Duration: ${formatDurationLabel(draft, vm)} (fixed, not editable)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    AnnouncementType.RECORDED_CLIP -> {
                        OutlinedTextField(
                            value = draft.text,
                            onValueChange = { draft = draft.copy(text = it) },
                            label = { Text("Name") },
                            placeholder = { Text("e.g. 30 seconds, Go!, Warning") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (draft.audioFilePath.isNotEmpty()) {
                            Text(
                                "Duration: ${formatSeconds(draft.estimatedDurationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            TextButton(onClick = {
                                vm.remeasureClipDuration(draft.audioFilePath) { ms ->
                                    draft = draft.copy(estimatedDurationMs = ms)
                                }
                            }) { Text("Measure duration") }
                        } else {
                            Text("No clip recorded yet", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                if (isRecording) {
                                    val durationMs = vm.stopRecording(draft, recordedFilename)
                                    draft = draft.copy(
                                        audioFilePath = recordedFilename,
                                        estimatedDurationMs = if (durationMs > 0) durationMs
                                                              else draft.estimatedDurationMs
                                    )
                                    isRecording = false
                                } else {
                                    recordedFilename = vm.startRecording(draft.id)
                                    isRecording = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                                 else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null
                            )
                            Text(if (isRecording) "  Stop recording" else "  Record clip")
                        }
                    }
                    else -> {}
                }

                if (draft.type != AnnouncementType.RECORDED_CLIP &&
                    draft.type != AnnouncementType.START_BEEP &&
                    draft.type != AnnouncementType.COUNTDOWN_BEEPS &&
                    draft.type != AnnouncementType.TIME) {
                    var durationText by remember(draft.estimatedDurationMs) {
                        mutableStateOf((draft.estimatedDurationMs / 1000).toString())
                    }
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { v ->
                            durationText = v
                            v.toLongOrNull()?.let { draft = draft.copy(estimatedDurationMs = it * 1000) }
                        },
                        label = { Text("Estimated duration (s) — for chain scheduling") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.saveAnnouncement(chain, draft)
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
