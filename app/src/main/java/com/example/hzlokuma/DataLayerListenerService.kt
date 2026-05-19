package com.example.hzlokuma

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets

class DataLayerListenerService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: com.google.android.gms.wearable.DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/speed_reader_text") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val asset = dataMap.getAsset("text_asset")
                
                if (asset != null) {
                    scope.launch {
                        try {
                            val inputStream = Wearable.getDataClient(this@DataLayerListenerService)
                                .getFdForAsset(asset).await().inputStream
                            
                            val text = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                            
                            if (text.isNotEmpty()) {
                                val sharedPref = getSharedPreferences("HizliOkuma", Context.MODE_PRIVATE)
                                sharedPref.edit().apply {
                                    putString("speedReader_text", text)
                                    putInt("speedReader_index", 0)
                                    apply()
                                }
                                Log.d("HizliOkuma", "Asset received and saved successfully")
                            }
                        } catch (e: Exception) {
                            Log.e("HizliOkuma", "Error receiving asset: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
