package com.woocommerce.android.ui.prefs.privacy.banner

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

@HiltViewModel
class PrivacyBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedStateHandle) {

    val analyticsEnabled: LiveData<Boolean> = appPrefsWrapper.observePrefs()
        .onStart { emit(Unit) }
        .map {
            analyticsTrackerWrapper.sendUsageStats
        }.asLiveData()
}
