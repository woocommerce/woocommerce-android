package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateOptions
import javax.inject.Inject

class DeveloperOptionsRepository @Inject constructor(
    private val appPrefs: AppPrefs,
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

    private fun updateSharedPreferences(selectedOption: UpdateOptions) {
        appPrefs.setSimulatedReaderOption(selectedOption)
    }
}
