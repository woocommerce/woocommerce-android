package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.EmojiUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CountryListPickerViewModel @Inject constructor(
    private val localCountriesRepository: LocalCountriesRepository,
    private val emojiUtils: EmojiUtils,
    private val newStore: NewStore,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val availableCountries = MutableStateFlow(emptyList<StoreCreationCountry>())
    private val navArgs: CountryListPickerFragmentArgs by savedStateHandle.navArgs()

    val countryListPickerState = availableCountries
        .map { CountryListPickerState(it) }
        .asLiveData()

    init {
        launch {
            val loadedCountriesMap = localCountriesRepository.getLocalCountries()

            availableCountries.update {
                loadedCountriesMap.map { (code, name) ->
                    StoreCreationCountry(
                        name = name,
                        code = code,
                        emojiFlag = emojiUtils.countryCodeToEmojiFlag(code),
                        isSelected = navArgs.currentLocationCode == code
                    )
                }.sortedBy { it.name }
            }
        }
    }

    fun onArrowBackPressed() {
        triggerEvent(Exit)
    }

    fun onCountrySelected(selectedCountry: StoreCreationCountry) {
        availableCountries.update {
            it.map { country ->
                country.copy(isSelected = country.code == selectedCountry.code)
            }
        }
    }

    fun onContinueClicked() {
        val selectedCountry = availableCountries.value.first { it.isSelected }
        newStore.update(
            country = selectedCountry.toNewStoreCountry()
        )

        launch {
            triggerEvent(NavigateToSummaryStep)
        }
    }

    object NavigateToSummaryStep : MultiLiveEvent.Event()

    data class CountryListPickerState(
        val countries: List<StoreCreationCountry>
    )
}
