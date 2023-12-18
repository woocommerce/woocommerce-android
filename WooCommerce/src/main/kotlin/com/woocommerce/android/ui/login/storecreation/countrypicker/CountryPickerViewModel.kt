package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.profiler.StoreProfilerRepository
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CountryPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val localCountriesRepository: LocalCountriesRepository,
    private val emojiUtils: EmojiUtils,
    private val storeProfilerRepository: StoreProfilerRepository
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val DEFAULT_LOCATION_CODE = "US"
    }

    private val detectedCountry = MutableStateFlow(
        StoreCreationCountry(
            name = "",
            code = "",
            emojiFlag = "",
            isSelected = false
        )
    )
    val countryPickerState = detectedCountry.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_COUNTRY
            )
        )
        launch {
            val loadedCountriesMap = localCountriesRepository.getLocalCountries()
            val currentCountryCode = when {
                !newStore.data.country?.code.isNullOrEmpty() -> newStore.data.country?.code!!
                loadedCountriesMap.containsKey(Locale.getDefault().country) -> Locale.getDefault().country
                else -> DEFAULT_LOCATION_CODE
            }

            val selectedCountry = StoreCreationCountry(
                name = loadedCountriesMap[currentCountryCode] ?: "",
                code = currentCountryCode,
                emojiFlag = emojiUtils.countryCodeToEmojiFlag(currentCountryCode),
                isSelected = true
            )

            detectedCountry.update { selectedCountry }

            newStore.update(
                country = selectedCountry.toNewStoreCountry()
            )
        }
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.STORE_CREATION))
    }

    fun onContinueClicked() {
        storeProfilerRepository.storeAnswers(
            siteId = newStore.data.siteId ?: 0L,
            countryCode = newStore.data.country?.code,
            profilerAnswers = newStore.data.profilerData
        )

        triggerEvent(NavigateToNextStep)
    }

    fun onCurrentCountryClicked() {
        triggerEvent(
            NavigateToDomainListPicker(
                locationCode = detectedCountry.value.code.takeIf { it.isNotEmpty() } ?: DEFAULT_LOCATION_CODE
            )
        )
    }

    fun onCountrySelected(country: StoreCreationCountry) {
        detectedCountry.update { country }
    }

    data class NavigateToDomainListPicker(val locationCode: String) : MultiLiveEvent.Event()
    object NavigateToNextStep : MultiLiveEvent.Event()
}
