package com.woocommerce.android.ui.prefs

import android.content.Context
import android.widget.Toast
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class DeveloperOptionsRepository @Inject constructor(
    private val appPrefs: AppPrefs,
    private val cardReaderManager: CardReaderManager,
    private val context: Context
) {

    fun isSimulatedCardReaderEnabled(): Boolean {
        return appPrefs.isSimulatedReaderEnabled
    }

    fun changeSimulatedReaderState(isChecked: Boolean) {
        appPrefs.isSimulatedReaderEnabled = isChecked
    }

    suspend fun clearSelectedCardReader() {
        cardReaderManager.disconnectReader()
    }

    fun showToast() {
        ToastUtils.showToast(context, R.string.simulated_reader_toast)
    }
}
