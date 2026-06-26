package com.undy.startrobot3.ui.startlist

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartListScreen(vm: StartListViewModel = viewModel()) {
    val starters by vm.starters.collectAsState()
    val status by vm.status.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val savedPath by vm.startListPath.collectAsState(initial = "")
    val savedIsUrl by vm.startListIsUrl.collectAsState(initial = false)

    val fileIsActive = !savedIsUrl && savedPath.isNotEmpty()
    val urlIsActive = savedIsUrl && savedPath.isNotEmpty()

    var urlText by remember { mutableStateOf("") }
    var showUrlInput by remember { mutableStateOf(false) }

    LaunchedEffect(savedPath, savedIsUrl) {
        if (savedIsUrl && savedPath.isNotEmpty()) {
            urlText = savedPath
            showUrlInput = true
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) vm.loadFromUri(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Start List", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (fileIsActive) {
                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) { Text("Load from file") }
            } else {
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f)
                ) { Text("Load from file") }
            }

            if (urlIsActive) {
                Button(
                    onClick = { showUrlInput = !showUrlInput },
                    modifier = Modifier.weight(1f)
                ) { Text("Load from URL") }
            } else {
                OutlinedButton(
                    onClick = { showUrlInput = !showUrlInput },
                    modifier = Modifier.weight(1f)
                ) { Text("Load from URL") }
            }
        }

        if (fileIsActive) {
            Text(
                fileDisplayName(savedPath),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (showUrlInput) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("IOF XML URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.loadFromUrl(urlText) },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlText.isNotBlank()
            ) { Text("Load") }
        }

        if (starters.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = { vm.clearStartList() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear start list") }
        }

        Spacer(Modifier.height(8.dp))

        if (status.isNotEmpty()) {
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (starters.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Text(
                "${starters.size} starters",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(starters) { starter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(starter.name, fontWeight = FontWeight.Medium)
                        Text(
                            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                .format(Date(starter.startTimeMs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun fileDisplayName(uriString: String): String = try {
    val decoded = URLDecoder.decode(Uri.parse(uriString).lastPathSegment ?: uriString, "UTF-8")
    decoded.substringAfterLast('/').substringAfterLast(':').ifEmpty { "File" }
} catch (_: Exception) { "File" }
