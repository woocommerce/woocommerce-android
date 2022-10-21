package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefs
import javax.inject.Inject

class DeveloperOptionsRepository @Inject constructor(
    private val appPrefs: AppPrefs
) {

    fun isSimulatedCardReaderEnabled( ): Boolean {
        return appPrefs.isSimulatedReaderEnabled
    }
}



