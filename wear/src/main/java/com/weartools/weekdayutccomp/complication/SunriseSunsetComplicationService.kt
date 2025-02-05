/*
 * Copyright 2022 amoledwatchfaces™
 * support@amoledwatchfaces.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.weartools.weekdayutccomp.complication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon.createWithResource
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.weartools.weekdayutccomp.MoonPhaseHelper
import com.weartools.weekdayutccomp.R
import com.weartools.weekdayutccomp.R.drawable
import kotlinx.coroutines.runBlocking
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SunriseSunsetComplicationService : SuspendingComplicationDataSourceService() {

    override fun onComplicationActivated(
        complicationInstanceId: Int,
        type: ComplicationType
    ) {
        Log.d(TAG, "onComplicationActivated(): $complicationInstanceId")
        reqPermissionFunction(applicationContext)
    }

    private fun reqPermissionFunction(context: Context) {

        runBlocking {
            val result = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (result == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted")
            } else {
                Toast.makeText(context, context.getString(R.string.enable_permission_toast), Toast.LENGTH_SHORT).show()
            }
        }

        Log.d(TAG, "req permission func")
    }


override fun getPreviewData(type: ComplicationType): ComplicationData? {
    return when (type) {
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "19:00").build(),
            contentDescription = PlainComplicationText.Builder(text = getString(R.string.sunrise_sunset_comp_name)).build())
            .setMonochromaticImage(MonochromaticImage.Builder(image = createWithResource(this, drawable.ic_sunset_3)).build())
            .setTapAction(null)
            .build()

        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "${getString(R.string.sunset)}: 19:00").build(),
            contentDescription = PlainComplicationText.Builder(text = getString(R.string.sunrise_sunset_comp_name)).build())
            .setTapAction(null)
            .build()

        else -> {null}
    }
}

override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
    Log.d(TAG, "onComplicationRequest() id: ${request.complicationInstanceId}")

    MoonPhaseHelper.updateSun(context = this)

    val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    val isSunrise = prefs.getBoolean(getString(R.string.is_sunrise), false)
    val ismilitary = prefs.getBoolean(getString(R.string.time_ampm_setting_key), true)
    val leadingzero = prefs.getBoolean(getString(R.string.time_setting_leading_zero_key), true)
    val coarseEnabled = prefs.getBoolean(getString(R.string.coarse_enabled), false)
    var icon = prefs.getInt(getString(R.string.sunrise_sunset_icon), drawable.ic_sunrise_3)
    var time = prefs.getString(getString(R.string.change_time), "0").toString()

    val text = if (isSunrise) getString(R.string.sunrise) else getString(R.string.sunset)
    val fmt = if (ismilitary && leadingzero) "HH:mm"
    else if (!ismilitary && !leadingzero) "h:mm a"
    else if (ismilitary) "H:mm"
    else "hh:mm"
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(fmt)

    if (time=="0" || !coarseEnabled) {
        icon = drawable.ic_location_not_available
        time = "-"
    }
    else {
        time = ZonedDateTime.parse(time).format(dateTimeFormatter)
    }

    return when (request.complicationType) {

        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = time).build(),
            contentDescription = PlainComplicationText.Builder(text = "$text: $time").build())
            .setMonochromaticImage(MonochromaticImage.Builder(createWithResource(this, icon)).build())
            .setTapAction(null)
            .build()

        ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "$text: $time").build(),
            contentDescription = PlainComplicationText.Builder(text = "$text: $time").build())
            .setTapAction(null)
            .build()


        else -> {
            if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type ${request.complicationType}")
            }
            null
        }


    }
}

override fun onComplicationDeactivated(complicationInstanceId: Int) {
    Log.d(TAG, "onComplicationDeactivated(): $complicationInstanceId")
}

companion object {
    private const val TAG = "CompDataSourceService"
}
}

