package com.example.hzlokuma.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.hzlokuma.presentation.MainActivity

class MainComplicationService : SuspendingComplicationDataSourceService() {

    private fun getTapAction(): PendingIntent? {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortTextData("%45", "Okuma")
            ComplicationType.RANGED_VALUE -> createRangedValueData(45f, "%45", "Okuma")
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val sharedPref = getSharedPreferences("HizliOkuma", android.content.Context.MODE_PRIVATE)
        val progress = sharedPref.getInt("progress", 0)
        
        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextData("%$progress", "Okuma İlerlemesi")
            ComplicationType.RANGED_VALUE -> createRangedValueData(progress.toFloat(), "%$progress", "Okuma İlerlemesi")
            else -> null
        }
    }

    private fun createShortTextData(text: String, description: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(description).build()
        )
        .setTapAction(getTapAction())
        .build()

    private fun createRangedValueData(value: Float, text: String, description: String) =
        RangedValueComplicationData.Builder(
            value = value,
            min = 0f,
            max = 100f,
            contentDescription = PlainComplicationText.Builder(description).build()
        )
        .setText(PlainComplicationText.Builder(text).build())
        .setTapAction(getTapAction())
        .build()
}
