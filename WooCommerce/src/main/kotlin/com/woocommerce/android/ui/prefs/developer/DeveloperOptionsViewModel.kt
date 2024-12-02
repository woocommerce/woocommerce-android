package com.woocommerce.android.ui.prefs.developer

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsViewModel.DeveloperOptionsViewState.UpdateFrequencyUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val developerOptionsRepository: DeveloperOptionsRepository
) : ScopedViewModel(savedState) {
    private val isSimulatedReaderEnabled = developerOptionsRepository.observeSimulatedCardReaderEnabled()

    private val simulatedCardReaderFlow = isSimulatedReaderEnabled.map { simulated ->
        ToggleableListItem(
            icon = R.drawable.img_card_reader,
            label = UiStringRes(R.string.enable_card_reader),
            key = UiStringRes(R.string.simulated_reader_key),
            isEnabled = true,
            isChecked = simulated,
            onToggled = ::onSimulatedReaderToggled
        )
    }

    private val readerUpdateFrequencyFlow = isSimulatedReaderEnabled.map { simulatedReader ->
        if (!simulatedReader) return@map null

        NonToggleableListItem(
            icon = R.drawable.img_card_reader_update_progress,
            iconTint = R.color.color_primary,
            endIcon = R.drawable.ic_arrow_drop_down,
            label = UiStringRes(R.string.update_simulated_reader),
            key = UiStringRes(R.string.update_simulated_reader_key),
            isEnabled = true,
            onClick = ::onUpdateSimulatedReaderClicked,
        )
    }

    private val interacPaymentEnabledFlow = combine(
        isSimulatedReaderEnabled,
        developerOptionsRepository.observeInteracPaymentEnabled()
    ) { simulatedReader, useInterac ->
        if (!simulatedReader) return@combine null

        ToggleableListItem(
            icon = R.drawable.ic_credit_card_give,
            label = UiStringRes(R.string.enable_interac_payment),
            key = UiStringRes(R.string.enable_interac_key),
            isEnabled = true,
            isChecked = useInterac,
            onToggled = developerOptionsRepository::changeEnableInteracPaymentState
        )
    }

    private val savedPrivacySettingsOnDialogFlow = developerOptionsRepository
        .observeSavedPrivacyBannerSettings()
        .map { isChecked ->
            ToggleableListItem(
                icon = R.drawable.ic_more_screen_settings,
                label = UiString.UiStringText("Saved privacy settings on dialog?"),
                key = UiString.UiStringText(""),
                isEnabled = true,
                isChecked = isChecked,
                onToggled = developerOptionsRepository::changeSavedPrivacyBannerSettings
            )
        }

    val viewState = combine(
        simulatedCardReaderFlow,
        readerUpdateFrequencyFlow,
        interacPaymentEnabledFlow,
        savedPrivacySettingsOnDialogFlow
    ) { items ->
        DeveloperOptionsViewState(
            rows = items.filterNotNull()
        )
    }.asLiveData()

    private fun onSimulatedReaderToggled(isChecked: Boolean) {
        developerOptionsRepository.changeSimulatedReaderState(isChecked)
        if (!isChecked) {
            disconnectAndClearSelectedCardReader()
            triggerEvent(
                DeveloperOptionsEvents.ShowToastString(R.string.simulated_reader_toast)
            )
        }
    }

    private fun disconnectAndClearSelectedCardReader() {
        launch {
            developerOptionsRepository.clearSelectedCardReader()
        }
    }

    private fun onUpdateSimulatedReaderClicked() {
        triggerEvent(
            DeveloperOptionsEvents.ShowUpdateOptionsDialog(
                UpdateFrequencyUiModel.entries,
                UpdateFrequencyUiModel.fromDomainModel(developerOptionsRepository.getUpdateSimulatedReaderOption())
            )
        )
    }

    fun onUpdateReaderOptionChanged(selectedOption: UpdateFrequencyUiModel) {
        developerOptionsRepository.updateSimulatedReaderOption(selectedOption.toDomainModel())
    }

    sealed class DeveloperOptionsEvents : MultiLiveEvent.Event() {
        data class ShowToastString(val message: Int) : DeveloperOptionsEvents()
        data class ShowUpdateOptionsDialog(
            val options: List<UpdateFrequencyUiModel>,
            var selectedValue: UpdateFrequencyUiModel,
        ) : DeveloperOptionsEvents()
    }

    data class DeveloperOptionsViewState(
        var rows: List<ListItem>
    ) {
        sealed class ListItem {
            abstract val label: UiString
            abstract val icon: Int
            abstract val iconTint: Int?
            abstract var isEnabled: Boolean
            abstract var key: UiString

            data class ToggleableListItem(
                @DrawableRes override val icon: Int,
                @ColorRes override val iconTint: Int? = null,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onToggled: (Boolean) -> Unit,
                val isChecked: Boolean
            ) : ListItem()

            data class NonToggleableListItem(
                @DrawableRes override val icon: Int,
                @ColorRes override val iconTint: Int? = null,
                @DrawableRes val endIcon: Int? = null,
                override val label: UiString,
                override var isEnabled: Boolean = false,
                override var key: UiString,
                val onClick: () -> Unit
            ) : ListItem()
        }

        enum class UpdateFrequencyUiModel(@StringRes val title: Int) {
            ALWAYS(R.string.always_update_reader),
            NEVER(R.string.never_update_reader),
            LOW_BATTERY_ERROR(R.string.low_battery_error_update_reader),
            LOW_BATTERY_SUCCEED_CONNECT(R.string.low_battery_succeed_connect_update_reader),
            RANDOM(R.string.randomly_update_reader);

            fun toDomainModel() = CardReaderManager.SimulatorUpdateFrequency.valueOf(name)

            companion object {
                fun fromDomainModel(model: CardReaderManager.SimulatorUpdateFrequency) = valueOf(model.name)
            }
        }
    }
}
