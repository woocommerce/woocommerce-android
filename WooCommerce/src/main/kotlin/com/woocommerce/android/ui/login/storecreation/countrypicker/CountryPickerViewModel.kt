package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.compose.ui.text.intl.Locale
import androidx.core.text.HtmlCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountryPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val newStore: NewStore,
    private val localCountriesRepository: LocalCountriesRepository,
    private val emojiUtils: EmojiUtils
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val DEFAULT_LOCATION_CODE = "US"
    }

    private val availableCountries = MutableStateFlow(emptyList<StoreCreationCountry>())
    val countryPickerState = availableCountries.map { countries ->
        CountryPickerState(
            storeName = newStore.data.name ?: "",
            countries = countries
        )
    }.asLiveData()

    init {
        launch {
            val loadedCountriesMap = localCountriesRepository.getLocalCountries()
            val defaultCountryCode = when {
                loadedCountriesMap.containsKey(Locale.current.region) -> Locale.current.region
                else -> DEFAULT_LOCATION_CODE
            }
            availableCountries.update {
                loadedCountriesMap.map { (code, name) ->
                    StoreCreationCountry(
                        name = HtmlCompat.fromHtml(name, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                        code = code,
                        emojiFlag = emojiUtils.countryCodeToEmojiFlag(code),
                        isSelected = defaultCountryCode == code
                    )
                }
            }
        }
    }

    fun onArrowBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onHelpPressed() {
        triggerEvent(MultiLiveEvent.Event.NavigateToHelpScreen(HelpOrigin.STORE_CREATION))
    }

    fun onContinueClicked() {
        newStore.update(country = availableCountries.value.first { it.isSelected }.toNewStoreCountry())
        triggerEvent(NavigateToDomainPickerStep)
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
    }

    private fun StoreCreationCountry.toNewStoreCountry() =
        NewStore.Country(
            name = name,
            code = code,
        )

    object NavigateToDomainPickerStep : MultiLiveEvent.Event()

    data class CountryPickerState(
        val storeName: String,
        val countries: List<StoreCreationCountry>
    )

    data class StoreCreationCountry(
        val name: String,
        val code: String,
        val emojiFlag: String,
        val isSelected: Boolean = false
    )
}
