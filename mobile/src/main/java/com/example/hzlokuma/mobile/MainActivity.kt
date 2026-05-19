package com.example.hzlokuma.mobile

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        setContent {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Bir PDF dosyası seçin") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            statusText = "PDF İşleniyor..."
            scope.launch {
                val text = extractTextFromPdf(context, it)
                if (text.isNotEmpty()) {
                    val success = sendToWearable(context, text)
                    statusText = if (success) "Metin Saate Gönderildi!" else "Bağlantı Hatası: Saate ulaşılamıyor."
                } else {
                    statusText = "Hata: Metin çıkarılamadı."
                }
                isProcessing = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Hızlı Okuma",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = statusText)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { launcher.launch("application/pdf") },
                enabled = !isProcessing
            ) {
                Text("PDF Seç ve Saate Gönder")
            }
            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}

suspend fun extractTextFromPdf(context: android.content.Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val document = PDDocument.load(inputStream)
        val stripper = PDFTextStripper()
        val text = stripper.getText(document)
        document.close()
        text
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

suspend fun sendToWearable(context: android.content.Context, text: String): Boolean {
    val dataClient = Wearable.getDataClient(context)
    
    return try {
        val byteArray = text.toByteArray(StandardCharsets.UTF_8)
        val asset = Asset.createFromBytes(byteArray)
        
        val putDataReq = PutDataMapRequest.create("/speed_reader_text").run {
            dataMap.putAsset("text_asset", asset)
            dataMap.putLong("timestamp", System.currentTimeMillis())
            asPutDataRequest()
        }
        
        dataClient.putDataItem(putDataReq).await()
        true
    } catch (e: Exception) {
        android.util.Log.e("HizliOkuma", "Gönderim hatası: ${e.message}")
        false
    }
}
