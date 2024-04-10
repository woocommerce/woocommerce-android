package com.woocommerce.android.ui.payments.taptopay.about

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForGB
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.ui.common.LocalCountriesRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TapToPayAboutViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val localCountriesRepository: LocalCountriesRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    @Test
    fun `given card reader config for canada, when view model init, then state contains important section`() {
        // GIVEN
        val cardReaderConfig = CardReaderConfigForCanada

        val countryName = "Canada"
        val amount = "250$"
        val returnString = "Tap to pay is available for purchases up to $amount in $countryName"
        whenever(localCountriesRepository.getLocalCountries()).thenReturn(mapOf("CA" to countryName))
        whenever(currencyFormatter.formatCurrencyRounded(250.00, "CAD")).thenReturn(amount)
        whenever(
            resourceProvider.getString(
                R.string.card_reader_tap_to_pay_about_important_info_description_1,
                countryName,
                amount,
            )
        ).thenReturn(
            returnString
        )

        // WHEN
        val viewModel = initViewModel(cardReaderConfig)

        // THEN
        val importantInfo = viewModel.viewState.value?.importantInfo
        assertThat(importantInfo?.pinDescription).isEqualTo(returnString)
    }

    @Test
    fun `given card reader config for the uk, when view model init, then state contains important section`() {
        // GIVEN
        val cardReaderConfig = CardReaderConfigForGB

        val countryName = "United Kingdom"
        val amount = "250$"
        val returnString = "Tap to pay is available for purchases up to $amount in $countryName"
        whenever(localCountriesRepository.getLocalCountries()).thenReturn(mapOf("GB" to countryName))
        whenever(currencyFormatter.formatCurrencyRounded(100.00, "GBP")).thenReturn(amount)
        whenever(
            resourceProvider.getString(
                R.string.card_reader_tap_to_pay_about_important_info_description_1,
                countryName,
                amount,
            )
        ).thenReturn(
            returnString
        )

        // WHEN
        val viewModel = initViewModel(cardReaderConfig)

        // THEN
        val importantInfo = viewModel.viewState.value?.importantInfo
        assertThat(importantInfo?.pinDescription).isEqualTo(returnString)
    }

    @Test
    fun `given card reader config for the us, when view model init, then state doesnt contain`() {
        // GIVEN
        val cardReaderConfig = CardReaderConfigForUSA

        // WHEN
        val viewModel = initViewModel(cardReaderConfig)

        // THEN
        val importantInfo = viewModel.viewState.value?.importantInfo
        assertThat(importantInfo).isNull()
    }

    @Test
    fun `given card reader config for the canada, when click on learn more, then open web view event emitted`() {
        // GIVEN
        val cardReaderConfig = CardReaderConfigForCanada
        val countryName = "Canada"
        val amount = "250$"
        val returnString = "Tap to pay is available for purchases up to $amount in $countryName"
        whenever(localCountriesRepository.getLocalCountries()).thenReturn(mapOf("CA" to countryName))
        whenever(currencyFormatter.formatCurrencyRounded(250.00, "CAD")).thenReturn(amount)
        whenever(
            resourceProvider.getString(
                R.string.card_reader_tap_to_pay_about_important_info_description_1,
                countryName,
                amount,
            )
        ).thenReturn(
            returnString
        )

        val viewModel = initViewModel(cardReaderConfig)

        // WHEN
        viewModel.viewState.value?.importantInfo?.onLearnMoreAboutCardReaders?.invoke()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            NavigateToUrlInGenericWebView(
                "https://woocommerce.com/products/hardware/CA"
            )
        )
    }

    @Test
    fun `given card reader config for the uk, when click on learn more, then open web view event emitted`() {
        // GIVEN
        val cardReaderConfig = CardReaderConfigForGB
        val countryName = "United Kingdom"
        val amount = "250$"
        val returnString = "Tap to pay is available for purchases up to $amount in $countryName"
        whenever(localCountriesRepository.getLocalCountries()).thenReturn(mapOf("GB" to countryName))
        whenever(currencyFormatter.formatCurrencyRounded(100.00, "GBP")).thenReturn(amount)
        whenever(
            resourceProvider.getString(
                R.string.card_reader_tap_to_pay_about_important_info_description_1,
                countryName,
                amount,
            )
        ).thenReturn(
            returnString
        )

        val viewModel = initViewModel(cardReaderConfig)

        // WHEN
        viewModel.viewState.value?.importantInfo?.onLearnMoreAboutCardReaders?.invoke()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(
            NavigateToUrlInGenericWebView(
                "https://woocommerce.com/products/hardware/GB"
            )
        )
    }

    private fun initViewModel(cardReaderConfig: CardReaderConfigForSupportedCountry): TapToPayAboutViewModel {
        return TapToPayAboutViewModel(
            resourceProvider = resourceProvider,
            localCountriesRepository = localCountriesRepository,
            currencyFormatter = currencyFormatter,
            savedStateHandle = TapToPayAboutFragmentArgs(
                cardReaderConfig
            ).toSavedStateHandle()
        )
    }
}
