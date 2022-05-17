package com.woocommerce.android.ui.cardreader.hub

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class CardReaderHubViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderHubViewModel
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on(it.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(WOOCOMMERCE_PAYMENTS)
    }
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }

    private val countryCode = "US"
    private val savedState = CardReaderHubFragmentArgs(
        storeCountryCode = countryCode,
        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub,
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `when screen shown, then manage card reader row present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.icon == R.drawable.ic_manage_card_reader
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.icon == R.drawable.ic_shopping_cart
            }
    }

    @Test
    fun `when screen shown, then manual card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual
            }
    }

    @Test
    fun `when user clicks on manage card reader, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderDetail(
                    CardReaderFlowParam.CardReadersHub
                )
            )
    }

    @Test
    fun `given ipp canada disabled, when user clicks on purchase card reader, then app opens external webview`() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)
        initViewModel()

        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                    AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER
                )
            )
    }

    @Test
    fun `given ipp canada enabled, when user clicks on purchase card reader, then app opens external webview`() {
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)
        initViewModel()

        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                    "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}$countryCode"
                )
            )
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview with in-person-payments link`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun `given wcpay active, when user clicks on purchase card reader, then woo purchase link shown`() {
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(WOOCOMMERCE_PAYMENTS)

        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.WOOCOMMERCE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun `given stripe active, when user clicks on purchase card reader, then stripe purchase link shown`() {
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(STRIPE_EXTENSION_GATEWAY)

        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }!!.onItemClicked.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.STRIPE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun ` when screen shown, then manuals row is displayed`() {
        assertThat((viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual &&
                    it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
            }
    }

    @Test
    fun `when user clicks on manuals row, then app navigates to manuals screen`() {
        (viewModel.viewStateData.value as CardReaderHubViewModel.CardReaderHubViewState.Content).rows
            .find {
                it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
            }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen
            )
    }

    private fun initViewModel() {
        viewModel = CardReaderHubViewModel(
            savedState,
            inPersonPaymentsCanadaFeatureFlag,
            appPrefsWrapper,
            selectedSite
        )
    }
}
