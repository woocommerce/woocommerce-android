package com.woocommerce.android.ui.prefs

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class DeveloperOptionsRepository @Inject constructor(
    private val appPrefs: AppPrefs,
    private val context: Context,
    private val clearCardReaderDataAction: ClearCardReaderDataAction
) {

    fun isSimulatedCardReaderEnabled(): Boolean {
        return appPrefs.isSimulatedReaderEnabled
    }

    fun changeSimulatedReaderState(isChecked: Boolean) {
        appPrefs.isSimulatedReaderEnabled = isChecked
    }

    suspend fun clearSelectedCardReader() {
        clearCardReaderDataAction.invoke()
    }

    fun showToast() {
        ToastUtils.showToast(context, R.string.simulated_reader_toast)
    }
}
