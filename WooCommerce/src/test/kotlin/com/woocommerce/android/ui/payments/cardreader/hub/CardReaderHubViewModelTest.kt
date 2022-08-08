package com.woocommerce.android.ui.payments.cardreader.hub

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.WOOCOMMERCE_PAYMENTS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wooStore: WooCommerceStore = mock()
    private val cardReaderChecker: CardReaderOnboardingChecker = mock {
        onBlocking { getOnboardingState() } doReturn mock<CardReaderOnboardingState.OnboardingCompleted>()
    }

    private val savedState = CardReaderHubFragmentArgs(
        cardReaderFlowParam = CardReaderFlowParam.CardReadersHub,
    ).initSavedStateHandle()

    @Before
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `when screen shown, then collect payments row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_manage_card_reader
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
            }
    }

    @Test
    fun `when screen shown, then collect payment row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_gridicons_money_on_surface
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_shopping_cart
            }
    }

    @Test
    fun `when screen shown, then manual card reader row icon is present`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual
            }
    }

    @Test
    fun `when user clicks on collect payment, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
        }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPaymentCollectionScreen
            )
    }

    @Test
    fun `when user clicks on manage card reader, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
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

        (viewModel.viewStateData.value)?.rows?.find {
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
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow(
                    "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
                )
            )
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview with in-person-payments link`() {
        (viewModel.viewStateData.value)?.rows?.find {
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

        (viewModel.viewStateData.value)?.rows?.find {
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

        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onItemClicked.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo(AppUrls.STRIPE_M2_PURCHASE_CARD_READER)
    }

    @Test
    fun ` when screen shown, then manuals row is displayed`() {
        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual &&
                    it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
            }
    }

    @Test
    fun `when user clicks on manuals row, then app navigates to manuals screen`() {
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
        }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value)
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen
            )
    }

    @Test
    fun `when multiple plugins installed, then payment provider row is shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
            }
    }

    @Test
    fun `when multiple plugins installed, then payment provider icon is shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .anyMatch {
                it.icon == R.drawable.ic_payment_provider
            }
    }

    @Test
    fun `given multiple plugins installed, when change payment provider clicked, then trigger onboarding event`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onItemClicked.invoke()

        assertThat(viewModel.event.value).isEqualTo(
            CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen
        )
    }

    @Test
    fun `given multiple plugins installed, when payment provider clicked, then clear plugin selected flag`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onItemClicked.invoke()

        verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(false)
        )
    }

    @Test
    fun `given multiple plugins installed, when change payment provider clicked, then track event`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.value)?.rows?.find {
            it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onItemClicked.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED)
    }

    @Test
    fun `when single plugin installed, then payment provider row is not shown`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(false)

        initViewModel()

        assertThat((viewModel.viewStateData.value)?.rows)
            .noneMatch {
                it.label == UiString.UiStringRes(R.string.card_reader_manage_payment_provider)
            }
    }

    @Test
    fun `given multiple plugins installed but not selected, when view model init, then error`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.ChoosePaymentGatewayProvider>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.errorText).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding error, when view model init, then show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.errorText).isEqualTo(
                UiString.UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding complete, when view model init, then do not show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.value?.errorText).isNull()
        }

    @Test
    fun `given onboarding error, when screen shown, then manage card reader row disabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
                }?.isEnabled
            ).isFalse
        }

    @Test
    fun `given onboarding complete, when screen shown, then manage card reader row enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            initViewModel()

            assertThat(
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_manage_card_reader)
                }?.isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding error, when screen shown, then collect payment row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.card_reader_collect_payment)
                }?.isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding error, when screen shown, then card reader manual is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                viewModel.viewStateData.value?.rows?.find {
                    it.label == UiString.UiStringRes(R.string.settings_card_reader_manuals)
                }?.isEnabled
            ).isTrue()
        }

    private fun initViewModel() {
        viewModel = CardReaderHubViewModel(
            savedState,
            inPersonPaymentsCanadaFeatureFlag,
            appPrefsWrapper,
            selectedSite,
            analyticsTrackerWrapper,
            wooStore,
            cardReaderChecker,
        )
    }
}
