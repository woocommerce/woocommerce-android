package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.compose.ui.text.intl.Locale
import androidx.core.text.HtmlCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.ui.login.storecreation.NewStore
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
    val localCountriesRepository: LocalCountriesRepository,
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val DEFAULT_LOCATION_CODE = "US"
    }

    private val availableCountries = MutableStateFlow(emptyList<StoreCreationCountry>())
    val countryPickerContent = availableCountries.map { countries ->
        CountryPickerContent(
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
                        emoji = code.toFlagEmoji(),
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
        triggerEvent(NavigateToDomainPickerStep)
    }

    fun onCountrySelected(country: StoreCreationCountry) {
        newStore.update(country = country.name)
    }

    private fun String.toFlagEmoji(): String {
        val countryCodeCaps = this.uppercase() // upper case is important because we are calculating offset
        val firstLetter = Character.codePointAt(countryCodeCaps, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCodeCaps, 1) - 0x41 + 0x1F1E6
        if (!countryCodeCaps[0].isLetter() || !countryCodeCaps[1].isLetter()) {
            return this
        }
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

    object NavigateToDomainPickerStep : MultiLiveEvent.Event()

    data class CountryPickerContent(
        val storeName: String,
        val countries: List<StoreCreationCountry>
    )

    data class StoreCreationCountry(
        val name: String,
        val code: String,
        val emoji: String,
        val isSelected: Boolean = false
    )
}
