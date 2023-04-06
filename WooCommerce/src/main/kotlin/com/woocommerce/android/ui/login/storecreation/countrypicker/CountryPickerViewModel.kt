package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Failed
import com.woocommerce.android.ui.login.storecreation.CreateFreeTrialStore.StoreCreationState.Loading
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
    private val createStore: CreateFreeTrialStore,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val DEFAULT_LOCATION_CODE = "US"
    }

    private val availableCountries = MutableStateFlow(emptyList<StoreCreationCountry>())

    val countryPickerState: LiveData<CountryPickerState> = combine(
        availableCountries,
        createStore.state
    ) { countries, createStoreState ->
        when (createStoreState) {
            is Failed -> CountryPickerState.Error(createStoreState.type)
            else -> CountryPickerState.Contentful(
                storeName = newStore.data.name ?: "",
                countries = countries,
                creatingStoreInProgress = createStoreState is Loading
            )
        }
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_PROFILER_COUNTRY
            )
        )
        launch {
            val loadedCountriesMap = localCountriesRepository.getLocalCountries()
            val defaultCountryCode = when {
                !newStore.data.country?.code.isNullOrEmpty() -> newStore.data.country?.code!!
                loadedCountriesMap.containsKey(Locale.getDefault().country) -> Locale.getDefault().country
                else -> DEFAULT_LOCATION_CODE
            }
            availableCountries.update {
                loadedCountriesMap.map { (code, name) ->
                    StoreCreationCountry(
                        name = name,
                        code = code,
                        emojiFlag = emojiUtils.countryCodeToEmojiFlag(code),
                        isSelected = defaultCountryCode == code
                    )
                }
            }
            newStore.update(
                country = availableCountries.value.first { it.isSelected }
                    .toNewStoreCountry()
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
        launch {
            if (FeatureFlag.FREE_TRIAL_M2.isEnabled()) {
                triggerEvent(NavigateToSummaryStep)
            } else {
                triggerEvent(NavigateToDomainPickerStep)
            }
        }
    }


    /**
     * We're currently not using this method anymore,
     * but we need to keep it until we have a final decision on
     * the store free trial creation flow steps.
     */
    @Suppress("UnusedPrivateMember")
    private suspend fun startFreeTrialSiteCreation() {
        createStore(
            storeDomain = newStore.data.domain,
            storeName = newStore.data.name,
        ).filterNotNull().collect {
            newStore.update(siteId = it)
            triggerEvent(NavigateToSummaryStep)
        }
    }

    fun onCountrySelected(country: StoreCreationCountry) {
        availableCountries.update { currentCountryList ->
            currentCountryList.map {
                when (it.code) {
                    country.code -> it.copy(isSelected = true)
                    else -> it.copy(isSelected = false)
                }
            }
        }
        newStore.update(country = country.toNewStoreCountry())
    }

    fun onExitTriggered() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun StoreCreationCountry.toNewStoreCountry() =
        NewStore.Country(
            name = name,
            code = code,
        )

    object NavigateToDomainPickerStep : MultiLiveEvent.Event()
    object NavigateToSummaryStep : MultiLiveEvent.Event()

    sealed class CountryPickerState {
        data class Contentful(
            val storeName: String,
            val countries: List<StoreCreationCountry>,
            val creatingStoreInProgress: Boolean,
        ) : CountryPickerState()

        data class Error(val errorType: StoreCreationErrorType) : CountryPickerState()
    }

    data class StoreCreationCountry(
        val name: String,
        val code: String,
        val emojiFlag: String,
        val isSelected: Boolean = false
    )
}
