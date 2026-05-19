package com.example.hzlokuma.presentation

import android.content.ComponentName
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.hzlokuma.complication.MainComplicationService
import com.example.hzlokuma.presentation.theme.HızlıOkumaTheme
import com.example.hzlokuma.tile.MainTileService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = SpeedReaderViewModel(application)
        setContent {
            LaunchedEffect(viewModel.isPlaying) {
                if (viewModel.isPlaying) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            WearApp(viewModel)
        }
    }

    override fun onPause() {
        super.onPause()
        val componentName = ComponentName(this, MainComplicationService::class.java)
        ComplicationDataSourceUpdateRequester.create(this, componentName).requestUpdateAll()
        TileService.getUpdater(this).requestUpdate(MainTileService::class.java)
    }
}

@Composable
fun WearApp(viewModel: SpeedReaderViewModel) {
    HızlıOkumaTheme {
        var currentScreen by remember { mutableStateOf("home") }

        AppScaffold {
            when (currentScreen) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToReader = { currentScreen = "reader" },
                    onNavigateToSettings = { currentScreen = "settings" }
                )
                "reader" -> ReaderScreen(
                    viewModel = viewModel,
                    onBack = { 
                        viewModel.pause()
                        currentScreen = "home" 
                    }
                )
                "settings" -> SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = "home" }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: SpeedReaderViewModel,
    onNavigateToReader: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hızlı Okuma",
                color = Color(0xFF60A5FA),
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val progressText = if (viewModel.words.isEmpty()) {
                "Kitap yüklenmedi."
            } else {
                val percent = (viewModel.currentIndex * 100 / viewModel.words.size)
                "% $percent Tamamlandı\n(${viewModel.currentIndex} / ${viewModel.words.size})"
            }
            
            Text(
                text = progressText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onNavigateToReader,
                enabled = viewModel.words.isNotEmpty()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            IconButton(
                onClick = onNavigateToSettings
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
fun ReaderScreen(viewModel: SpeedReaderViewModel, onBack: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.pause()
        }
    }
    BackHandler {
        onBack()
    }
    val word = if (viewModel.currentIndex < viewModel.words.size) {
        viewModel.words[viewModel.currentIndex]
    } else {
        ""
    }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { viewModel.togglePlayPause() }
        ) {
            // RSVP Display
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (viewModel.isPlaying) {
                    WordDisplay(word, viewModel.fontSize)
                } else {
                    Text(
                        text = if (viewModel.currentIndex >= viewModel.words.size && viewModel.words.isNotEmpty()) 
                            "Bitti!" else "II Bekliyor",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Bottom Controls & Progress
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!viewModel.isPlaying && viewModel.words.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.rewind() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (viewModel.words.isNotEmpty()) {
                    val progress = viewModel.currentIndex.toFloat() / viewModel.words.size.toFloat()
                    val percent = (progress * 100).toInt()
                    
                    Text(
                        text = "%$percent",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .background(Color.DarkGray, shape = MaterialTheme.shapes.extraSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(Color(0xFF2563EB), shape = MaterialTheme.shapes.extraSmall)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WordDisplay(word: String, fontSize: Int) {
    if (word.isEmpty()) return
    
    val cleanWord = word.replace(Regex("[.,!?;:]"), "")
    val length = if (cleanWord.isEmpty()) word.length else cleanWord.length
    
    var pivot = length / 2
    if (length > 5) pivot = length / 2 - 1
    if (length <= 1) pivot = 0
    
    val actualPivot = if (pivot < word.length) pivot else 0
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = word.substring(0, actualPivot),
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = word.substring(actualPivot, (actualPivot + 1).coerceAtMost(word.length)),
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )
        Text(
            text = word.substring((actualPivot + 1).coerceAtMost(word.length)),
            fontSize = fontSize.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SettingsScreen(viewModel: SpeedReaderViewModel, onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
    val scrollState = rememberScrollState()
    var showTextInput by remember { mutableStateOf(false) }

    ScreenScaffold {
        if (showTextInput) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Metin Yükle", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    viewModel.saveText("Bu bir hızlı okuma testi metnidir. Wear OS üzerinde RSVP yöntemi ile kitap okumak oldukça verimlidir. Başarılar dileriz!")
                    showTextInput = false
                }) {
                    Text("Örnek Yükle", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showTextInput = false }) {
                    Text("Vazgeç")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Ayarlar", style = MaterialTheme.typography.titleSmall)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.updateSpeed(-25) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Slower")
                    }
                    Text("${viewModel.readingSpeedWPM} WPM", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { viewModel.updateSpeed(25) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Faster")
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.updateFontSize(-2) }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Smaller")
                    }
                    Text("${viewModel.fontSize} px", style = MaterialTheme.typography.bodySmall)
                    IconButton(onClick = { viewModel.updateFontSize(2) }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Larger")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    IconButton(onClick = { showTextInput = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.resetProgress() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(onClick = onBack) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            }
        }
    }
}
