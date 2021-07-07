package com.woocommerce.android.ui.prefs.cardreader.connect.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CardReaderEligibilityViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {

}
