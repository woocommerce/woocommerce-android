package com.woocommerce.android.ui.payments.taptopay.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.ui.common.LocalCountriesRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TapToPayAboutViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val localCountriesRepository: LocalCountriesRepository,
    private val currencyFormatter: CurrencyFormatter,
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: TapToPayAboutFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableLiveData<UiState>()
    val viewState: LiveData<UiState> = _viewState

    init {
        _viewState.value = UiState(
            importantInfo = navArgs.cardReaderConfig.buildImportantInfoSection(),
            onLearnMoreAboutTapToPay = {
                triggerEvent(
                    NavigateToUrlInGenericWebView(
                        AppUrls.LEARN_MORE_ABOUT_TAP_TO_PAY
                    )
                )
            }
        )
    }

    fun onBackClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    private fun CardReaderConfigForSupportedCountry.buildImportantInfoSection(): UiState.ImportantInfo? {
        val chargeWithoutPin = maximumTTPAllowedChargeAmountWithoutPin
        return if (chargeWithoutPin == null) {
            null
        } else {
            val countryName = localCountriesRepository.getLocalCountries()[countryCode] ?: ""
            val amount = currencyFormatter.formatCurrencyRounded(
                rawValue = chargeWithoutPin.toDouble(),
                currencyCode = currency,
            )
            UiState.ImportantInfo(
                pinDescription = resourceProvider.getString(
                    R.string.card_reader_tap_to_pay_about_important_info_description_1,
                    countryName,
                    amount
                ),
                onLearnMoreAboutCardReaders = {
                    triggerEvent(
                        NavigateToUrlInGenericWebView(
                            "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$countryCode"
                        )
                    )
                }
            )
        }
    }

    data class UiState(
        val importantInfo: ImportantInfo?,
        val onLearnMoreAboutTapToPay: () -> Unit
    ) {
        data class ImportantInfo(
            val pinDescription: String,
            val onLearnMoreAboutCardReaders: () -> Unit,
        )
    }
}

data class NavigateToUrlInGenericWebView(val url: String) : MultiLiveEvent.Event()
