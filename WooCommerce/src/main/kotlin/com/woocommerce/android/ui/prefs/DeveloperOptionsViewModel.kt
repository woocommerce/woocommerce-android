package com.woocommerce.android.ui.prefs

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.SpinnerListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateOptions
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    cardReaderCountryConfigProvider: CardReaderCountryConfigProvider,
    savedState: SavedStateHandle,
    selectedSite: SelectedSite,
    wooStore: WooCommerceStore,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val cardReaderManager: CardReaderManager,

) : ScopedViewModel(savedState) {
    private val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
    private val countryConfig = cardReaderCountryConfigProvider.provideCountryConfigFor(storeCountryCode)
    private val _viewState = MutableLiveData(
        DeveloperOptionsViewState(
            rows = if (developerOptionsRepository.isSimulatedCardReaderEnabled()) {
                if (countryConfig is CardReaderConfigForCanada) {
                    createDeveloperOptionsList() + createReaderUpdateFrequencyItem() + createEnableInteractItem()
                } else {
                    createDeveloperOptionsList() + createReaderUpdateFrequencyItem()
                }
            } else {
                createDeveloperOptionsList()
            }
        )
    )

    val viewState: LiveData<DeveloperOptionsViewState> = _viewState

    private fun createDeveloperOptionsList(): List<ListItem> = mutableListOf<ListItem>(
        ToggleableListItem(
            icon = drawable.img_card_reader_connecting,
            label = UiStringRes(string.enable_card_reader),
            key = UiStringRes(string.simulated_reader_key),
            isEnabled = true,
            isChecked = developerOptionsRepository.isSimulatedCardReaderEnabled(),
            onToggled = ::onSimulatedReaderToggled
        ),
    )

    private fun createReaderUpdateFrequencyItem() =
        SpinnerListItem(
            icon = drawable.img_card_reader_update_progress,
            endIcon = drawable.ic_arrow_drop_down,
            label = UiStringRes(string.update_simulated_reader),
            key = UiStringRes(string.update_simulated_reader_key),
            isEnabled = true,
            onClick = ::onUpdateSimulatedReaderClicked,
        )

    private fun createEnableInteractItem() =
        ToggleableListItem(
            icon = drawable.ic_credit_card_give,
            label = UiStringRes(string.enable_interac_payment),
            key = UiStringRes(string.enable_interac_key),
            isEnabled = true,
            isChecked = developerOptionsRepository.isInteracPaymentEnabled(),
            onToggled = ::onEnableInteracToggled
        )

    private fun onSimulatedReaderToggled(isChecked: Boolean) {
        if (!isChecked) {
            viewState.value?.rows = createDeveloperOptionsList()
            disconnectAndClearSelectedCardReader()
            triggerEvent(
                DeveloperOptionsEvents.ShowToastString(string.simulated_reader_toast)
            )
        } else {
            viewState.value?.rows =
                if (countryConfig is CardReaderConfigForCanada) {
                    createDeveloperOptionsList() + createReaderUpdateFrequencyItem() + createEnableInteractItem()
                } else {
                    createDeveloperOptionsList() + createReaderUpdateFrequencyItem()
                }
        }
        simulatedReaderStateChanged(isChecked)
    }

    private fun disconnectAndClearSelectedCardReader() {
        launch {
            developerOptionsRepository.clearSelectedCardReader()
        }
    }

    private fun simulatedReaderStateChanged(isChecked: Boolean) {
        developerOptionsRepository.changeSimulatedReaderState(isChecked)
        val currentViewState = viewState.value
        (
            currentViewState?.rows?.find {
                it.key == UiStringRes(string.simulated_reader_key)
            } as? ToggleableListItem
            )?.let { originalListItem ->
            val newState = originalListItem.copy(isChecked = isChecked)
            _viewState.value = currentViewState.copy(
                rows = currentViewState.rows.map {
                    if (it.label == newState.label)
                        newState
                    else it
                }
            )
        }
    }

    private fun enableInteracStateChange(isChecked: Boolean) {
        developerOptionsRepository.changeEnableInteracPaymentState(isChecked)
        val currentViewState = viewState.value
        (
            currentViewState?.rows?.find {
                it.key == UiStringRes(string.enable_interac_key)
            } as? ToggleableListItem
            )?.let { originalListItem ->
                val newState = originalListItem.copy(isChecked = isChecked)
                _viewState.value = currentViewState.copy(
                    rows = currentViewState.rows.map {
                        if (it.label == newState.label)
                            newState
                        else it
                    }
                )

            }
    }

    private fun onEnableInteracToggled(isChecked: Boolean) {
        if (cardReaderManager.initialized) {
            if (!isChecked) {
                cardReaderManager.disableSimulatorInteract()
            } else  {
                cardReaderManager.enableSimulatorInterac()
            }
        }
        enableInteracStateChange(isChecked)
    }

    private fun onUpdateSimulatedReaderClicked() {
        triggerEvent(
            DeveloperOptionsEvents.ShowUpdateOptionsDialog(
                UpdateOptions.values().toList(),
                developerOptionsRepository.getUpdateSimulatedReaderOption()
            )
        )
    }

    fun onUpdateReaderOptionChanged(selectedOption: UpdateOptions) {
        if (cardReaderManager.initialized) {
            cardReaderManager.initializeOnUpdateFrequencyChange(mapUpdateOptions(selectedOption))
        }
        developerOptionsRepository.updateSimulatedReaderOption(selectedOption)
    }

    private fun mapUpdateOptions(updateFrequency: UpdateOptions): CardReaderManager.SimulatorUpdateFrequency {
        return when (updateFrequency) {
            UpdateOptions.ALWAYS -> CardReaderManager.SimulatorUpdateFrequency.ALWAYS
            UpdateOptions.NEVER -> CardReaderManager.SimulatorUpdateFrequency.NEVER
            UpdateOptions.RANDOM -> CardReaderManager.SimulatorUpdateFrequency.RANDOM
        }
    }

    sealed class DeveloperOptionsEvents : MultiLiveEvent.Event() {
        data class ShowToastString(val message: Int) : DeveloperOptionsEvents()
        data class ShowUpdateOptionsDialog(
            val options: List<UpdateOptions>,
            var selectedValue: UpdateOptions,
        ) : DeveloperOptionsEvents()
    }

    data class DeveloperOptionsViewState(
        var rows: List<ListItem>
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int?
            abstract var isEnabled: Boolean
            abstract var key: UiString

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onToggled: (Boolean) -> Unit,
                val isChecked: Boolean
            ) : ListItem()

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onClick: () -> Unit
            ) : ListItem()

            data class SpinnerListItem(
                @DrawableRes override val icon: Int,
                @DrawableRes val endIcon: Int,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onClick: () -> Unit,

            ) : ListItem()
        }

        enum class UpdateOptions(@StringRes val title: Int) {
            ALWAYS(string.always_update_reader),
            NEVER(string.never_update_reader),
            RANDOM(string.randomly_update_reader)
        }
    }
}
