package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.StoreCreationErrorType.SITE_ADDRESS_ALREADY_EXISTS
import com.woocommerce.android.ui.login.storecreation.StoreCreationRepository
import com.woocommerce.android.ui.login.storecreation.StoreCreationResult
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class CountryPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val localCountriesRepository: LocalCountriesRepository,
    private val emojiUtils: EmojiUtils,
    private val repository: StoreCreationRepository,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val DEFAULT_LOCATION_CODE = "US"
    }

    private val availableCountries = MutableStateFlow(emptyList<StoreCreationCountry>())
    private val creatingStoreInProgress = MutableStateFlow(false)

    val countryPickerState = availableCountries.combine(creatingStoreInProgress) { countries, creatingStoreInProgress ->
        CountryPickerState(
            storeName = newStore.data.name ?: "",
            countries = countries,
            creatingStoreInProgress = creatingStoreInProgress
        )
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
            newStore.update(country = availableCountries.value.first { it.isSelected }.toNewStoreCountry())
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
            if (FeatureFlag.FREE_TRIAL.isEnabled()) {
                creatingStoreInProgress.value = true
                createFreeTrialSite().ifSuccessfulThen { siteId ->
                    newStore.update(siteId = siteId)
                    triggerEvent(NavigateToInstallationStep)
                    creatingStoreInProgress.value = false
                }
            } else {
                triggerEvent(NavigateToDomainPickerStep)
            }
        }
    }

    private suspend fun <T : Any?> StoreCreationResult<T>.ifSuccessfulThen(
        successAction: suspend (T) -> Unit
    ) {
        when (this) {
            is StoreCreationResult.Success -> successAction(this.data)
            is StoreCreationResult.Failure -> Unit // todo error-state
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

    private suspend fun createFreeTrialSite(): StoreCreationResult<Long> {
        suspend fun StoreCreationResult<Long>.recoverIfSiteExists(): StoreCreationResult<Long> {
            return if ((this as? StoreCreationResult.Failure<Long>)?.type == SITE_ADDRESS_ALREADY_EXISTS) {
                repository.getSiteByUrl(newStore.data.domain)?.let { site ->
                    StoreCreationResult.Success(site.siteId)
                } ?: this
            } else {
                this
            }
        }

        return repository.createNewFreeTrialSite(
            StoreCreationRepository.SiteCreationData(
                siteDesign = PlansViewModel.NEW_SITE_THEME,
                domain = newStore.data.domain,
                title = newStore.data.name,
                segmentId = null
            ),
            PlansViewModel.NEW_SITE_LANGUAGE_ID,
            TimeZone.getDefault().id
        ).recoverIfSiteExists()
    }

    private fun StoreCreationCountry.toNewStoreCountry() =
        NewStore.Country(
            name = name,
            code = code,
        )

    object NavigateToDomainPickerStep : MultiLiveEvent.Event()
    object NavigateToInstallationStep : MultiLiveEvent.Event()

    data class CountryPickerState(
        val storeName: String,
        val countries: List<StoreCreationCountry>,
        val creatingStoreInProgress: Boolean,
    )

    data class StoreCreationCountry(
        val name: String,
        val code: String,
        val emojiFlag: String,
        val isSelected: Boolean = false
    )
}
