package com.example.hzlokuma.presentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SpeedReaderViewModel(application: Application) : AndroidViewModel(application), SharedPreferences.OnSharedPreferenceChangeListener {
    private val context = application.applicationContext
    private val sharedPref: SharedPreferences =
        context.getSharedPreferences("HizliOkuma", Context.MODE_PRIVATE)

    var words by mutableStateOf(listOf<String>())
        private set
    var currentIndex by mutableStateOf(0)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var readingSpeedWPM by mutableStateOf(300)
        private set
    var fontSize by mutableStateOf(24) // in sp
        private set

    private var timerJob: Job? = null

    init {
        loadData()
        sharedPref.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        android.util.Log.d("HizliOkuma", "Pref changed: $key")
        if (key == "speedReader_text") {
            loadData()
        }
    }

    private fun loadData() {
        val savedText = sharedPref.getString("speedReader_text", "") ?: ""
        android.util.Log.d("HizliOkuma", "Loading data, text length: ${savedText.length}")
        if (savedText.isNotEmpty()) {
            words = savedText.split(Regex("\\s+")).filter { it.isNotEmpty() }
        } else {
            words = emptyList()
        }
        currentIndex = sharedPref.getInt("speedReader_index", 0)
        readingSpeedWPM = sharedPref.getInt("speedReader_wpm", 300)
        fontSize = sharedPref.getInt("speedReader_font_size", 24)
        
        if (currentIndex >= words.size && words.isNotEmpty()) {
            currentIndex = 0
        }
    }

    fun saveData() {
        with(sharedPref.edit()) {
            putInt("speedReader_index", currentIndex)
            putInt("speedReader_wpm", readingSpeedWPM)
            putInt("speedReader_font_size", fontSize)
            val progress = if (words.isEmpty()) 0 else (currentIndex * 100 / words.size)
            putInt("progress", progress)
            apply()
        }
    }

    fun saveText(text: String) {
        with(sharedPref.edit()) {
            putString("speedReader_text", text)
            putInt("speedReader_index", 0)
            apply()
        }
        // loadData() will be called via OnSharedPreferenceChangeListener
    }

    fun togglePlayPause() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {
        if (words.isEmpty()) return
        if (currentIndex >= words.size) currentIndex = 0
        isPlaying = true
        startTimer()
    }

    fun pause() {
        isPlaying = false
        timerJob?.cancel()
        saveData()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isPlaying && currentIndex < words.size) {
                val word = words[currentIndex]
                var delayMs = (60_000 / readingSpeedWPM).toLong()
                if (word.endsWith(".") || word.endsWith(",") || word.endsWith("!") || 
                    word.endsWith("?") || word.endsWith(";") || word.endsWith(":")) {
                    delayMs += 250
                }
                delay(delayMs)
                currentIndex++
                if (currentIndex % 50 == 0) {
                    saveData()
                }
            }
            if (currentIndex >= words.size) {
                isPlaying = false
                saveData()
            }
        }
    }

    fun rewind() {
        currentIndex = (currentIndex - 20).coerceAtLeast(0)
        if (!isPlaying) saveData()
    }

    fun updateSpeed(delta: Int) {
        readingSpeedWPM = (readingSpeedWPM + delta).coerceAtLeast(50)
        saveData()
    }

    fun updateFontSize(delta: Int) {
        fontSize = (fontSize + delta).coerceIn(12, 60)
        saveData()
    }
    
    fun resetProgress() {
        currentIndex = 0
        saveData()
    }
}
