package net.kdt.pojavlaunch.kotlin.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.File
import java.io.IOException
import java.util.Locale

class ProfileSelectionViewModel : ViewModel() {
    var fullList by mutableStateOf<List<Instance>>(emptyList())
    var filteredList by mutableStateOf<List<Instance>>(emptyList())
    var selectedInstancePathName by mutableStateOf("")
    var searchQuery by mutableStateOf("")

    var showReleases by mutableStateOf(true)
    var showSnapshots by mutableStateOf(true)
    var showModded by mutableStateOf(true)

    var isLoading by mutableStateOf(false)

    fun loadProfiles() {
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val instances = Instances.loadAllInstances().filterNotNull()
                val selected = Instances.loadSelectedInstance()

                withContext(Dispatchers.Main) {
                    fullList = instances
                    selectedInstancePathName = selected?.instanceRoot?.name ?: ""
                    filter()
                    isLoading = false
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        filter()
    }

    fun updateFilters(releases: Boolean, snapshots: Boolean, modded: Boolean) {
        showReleases = releases
        showSnapshots = snapshots
        showModded = modded
        filter()
    }

    private fun filter() {
        val lowerQuery = searchQuery.lowercase(Locale.getDefault())
        filteredList = fullList.filter { instance ->
            val matchesSearch = searchQuery.isEmpty() ||
                    (instance.name?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true) ||
                    (instance.versionId?.lowercase(Locale.getDefault())?.contains(lowerQuery) == true)

            if (!matchesSearch) return@filter false

            val isSnapshot = instance.versionId?.let {
                it.contains("w") || it.contains("pre") || it.contains("rc")
            } ?: false
            val isRelease = instance.versionId?.let {
                !isSnapshot && (it != Instance.VERSION_LATEST_RELEASE) && (it != Instance.VERSION_LATEST_SNAPSHOT)
            } ?: false
            val isModded = instance.installer != null

            var shouldShow = false
            if (isModded && showModded) shouldShow = true
            else if (isSnapshot && showSnapshots) shouldShow = true
            else if (isRelease && showReleases) shouldShow = true
            else if (instance.versionId == Instance.VERSION_LATEST_RELEASE && showReleases) shouldShow = true
            else if (instance.versionId == Instance.VERSION_LATEST_SNAPSHOT && showSnapshots) shouldShow = true
            else if (!isModded && !isSnapshot && !isRelease) {
                shouldShow = showReleases || showSnapshots || showModded
            }

            shouldShow
        }
    }

    fun selectInstance(instance: Instance) {
        instance.instanceRoot?.name?.let {
            selectedInstancePathName = it
            Instances.setSelectedInstance(instance)
        }
    }

    fun deleteInstance(instance: Instance, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Instances.removeInstance(instance)
                withContext(Dispatchers.Main) {
                    loadProfiles()
                    onDone()
                }
            } catch (e: IOException) {
            }
        }
    }

    fun setBackgroundPath(context: Context, path: String?) {
        var finalPath = path
        if (path != null && path.startsWith("content://")) {
            try {
                val uri = Uri.parse(path)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = File(context.filesDir, "launcher_background.png")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    finalPath = file.absolutePath
                }
            } catch (e: Exception) {
                Log.e("ProfileSelectionVM", "Failed to copy background image", e)
            }
        }

        LauncherPreferences.PREF_BACKGROUND_PATH = finalPath
        LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value = finalPath
        LauncherPreferences.DEFAULT_PREF?.edit()?.putString("backgroundPath", finalPath)?.apply()
    }

    fun setBackgroundTransparency(transparency: Float) {
        LauncherPreferences.PREF_BACKGROUND_TRANSPARENCY = transparency
        LauncherPreferences.PREF_BACKGROUND_TRANSPARENCY_STATE.value = transparency
        LauncherPreferences.DEFAULT_PREF?.edit()?.putFloat("backgroundTransparency", transparency)?.apply()
    }

    fun setBackgroundBlur(blur: Float) {
        LauncherPreferences.PREF_BACKGROUND_BLUR = blur
        LauncherPreferences.PREF_BACKGROUND_BLUR_STATE.value = blur
        LauncherPreferences.DEFAULT_PREF?.edit()?.putFloat("backgroundBlur", blur)?.apply()
    }

    fun setBackgroundBlurEnabled(enabled: Boolean) {
        LauncherPreferences.PREF_BACKGROUND_BLUR_ENABLED = enabled
        LauncherPreferences.PREF_BACKGROUND_BLUR_ENABLED_STATE.value = enabled
        LauncherPreferences.DEFAULT_PREF?.edit()?.putBoolean("backgroundBlurEnabled", enabled)?.apply()
    }
}