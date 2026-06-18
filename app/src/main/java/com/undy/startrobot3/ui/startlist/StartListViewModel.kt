package com.undy.startrobot3.ui.startlist

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.undy.startrobot3.StartRobotApplication
import com.undy.startrobot3.data.model.Starter
import com.undy.startrobot3.iof.IofXmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class StartListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StartRobotApplication
    private val prefs = app.eventPreferences

    private val _starters = MutableStateFlow<List<Starter>>(emptyList())
    val starters: StateFlow<List<Starter>> = _starters.asStateFlow()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val startListPath = prefs.startListPath
    val startListIsUrl = prefs.startListIsUrl

    init {
        viewModelScope.launch {
            val path = prefs.startListPath.first()
            val isUrl = prefs.startListIsUrl.first()
            if (path.isNotEmpty()) reloadFromSaved(path, isUrl)
        }
    }

    fun loadFromUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val starters = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { IofXmlParser.parse(it) }
                        ?: emptyList()
                }
                applyStarters(starters)
                prefs.setStartList(uri.toString(), isUrl = false)
                _status.value = "Loaded ${starters.size} starters from file"
            } catch (e: Exception) {
                _status.value = "Error loading file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadFromUrl(url: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val starters = withContext(Dispatchers.IO) {
                    URL(url).openStream().use { IofXmlParser.parse(it) }
                }
                applyStarters(starters)
                prefs.setStartList(url, isUrl = true)
                _status.value = "Loaded ${starters.size} starters from URL"
            } catch (e: Exception) {
                _status.value = "Error loading URL: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearStartList() {
        viewModelScope.launch {
            applyStarters(emptyList())
            prefs.setStartList("", isUrl = false)
            _status.value = "Start list cleared"
        }
    }

    private fun applyStarters(starters: List<Starter>) {
        _starters.value = starters
        app.loadedStarters = starters
    }

    private fun reloadFromSaved(path: String, isUrl: Boolean) {
        if (isUrl) loadFromUrl(path)
        else {
            try {
                loadFromUri(Uri.parse(path))
            } catch (_: Exception) {
                _status.value = "Could not reload saved start list"
            }
        }
    }
}
