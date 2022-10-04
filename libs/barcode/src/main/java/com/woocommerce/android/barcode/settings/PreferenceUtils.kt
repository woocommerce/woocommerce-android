/*
 * Copyright 2020 Google LLC
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

package com.woocommerce.android.barcode.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.google.android.gms.common.images.Size
import com.woocommerce.android.barcode.R
import com.woocommerce.android.barcode.camera.CameraSizePair

/** Utility class to retrieve shared preferences.  */
object PreferenceUtils {
    fun saveStringPreference(context: Context, @StringRes prefKeyId: Int, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(context.getString(prefKeyId), value)
            .apply()
    }

    fun getUserSpecifiedPreviewSize(context: Context): CameraSizePair? {
        return try {
            val previewSizePrefKey = context.getString(R.string.pref_key_rear_camera_preview_size)
            val pictureSizePrefKey = context.getString(R.string.pref_key_rear_camera_picture_size)
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            CameraSizePair(
                Size.parseSize(sharedPreferences.getString(previewSizePrefKey, null) ?: ""),
                Size.parseSize(sharedPreferences.getString(pictureSizePrefKey, null) ?: "")
            )
        } catch (e: NumberFormatException) {
            null
        }
    }
}
