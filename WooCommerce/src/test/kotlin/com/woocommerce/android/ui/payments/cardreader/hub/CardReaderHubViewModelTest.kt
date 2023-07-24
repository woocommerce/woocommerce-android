package com.woocommerce.android.ui.payments.cardreader.hub

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfig
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.feedback.FeedbackRepository
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CardReaderTracker
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider
import com.woocommerce.android.ui.payments.cardreader.LearnMoreUrlProvider.LearnMoreUrlType.CASH_ON_DELIVERY
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.NavigateToTapTooPaySummaryScreen
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.NavigateToTapTooPaySurveyScreen
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.OpenGenericWebView
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToast
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.ShowToastString
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CashOnDeliverySource.PAYMENTS_HUB
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.GapBetweenSections
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.ToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.CardReadersHub.OpenInHub
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.StripeAccountPendingRequirement
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.Available
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable.GooglePlayServicesNotAvailable
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus.Result.NotAvailable.SystemVersionNotSupported
import com.woocommerce.android.util.UtmProvider
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Calendar
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class CardReaderHubViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderHubViewModel
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val wooStore: WooCommerceStore = mock()
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock()
    private val cardReaderChecker: CardReaderOnboardingChecker = mock {
        onBlocking { getOnboardingState() } doReturn mock<CardReaderOnboardingState.OnboardingCompleted>()
    }
    private val cashOnDeliverySettingsRepository: CashOnDeliverySettingsRepository = mock {
        onBlocking { isCashOnDeliveryEnabled() } doReturn false
    }
    private val learnMoreUrlProvider: LearnMoreUrlProvider = mock()
    private val cardReaderTracker: CardReaderTracker = mock()
    private val paymentMenuUtmProvider: UtmProvider = mock()
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus = mock {
        on { invoke() }.thenReturn(Available)
    }
    private val appPrefs: AppPrefs = mock()
    private val feedbackRepository: FeedbackRepository = mock {
        on { getFeatureFeedbackSetting(any()) }.thenReturn(
            FeatureFeedbackSettings(FeatureFeedbackSettings.Feature.TAP_TO_PAY)
        )
    }
    private val cardReaderHubTapToPayUnavailableHandler: CardReaderHubTapToPayUnavailableHandler = mock()

    @Before
    fun setUp() {
        initViewModel()
    }

    @Test
    fun `when screen shown, then collect payments row present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.label == UiStringRes(R.string.card_reader_hub_collect_payment)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.label == UiStringRes(R.string.card_reader_manage_card_reader)
            }
    }

    @Test
    fun `when screen shown, then manage card reader row icon is present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.icon == R.drawable.ic_manage_card_reader
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.label == UiStringRes(R.string.card_reader_purchase_card_reader)
            }
    }

    @Test
    fun `when screen shown, then collect payment row icon is present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.icon == R.drawable.ic_gridicons_money_on_surface
            }
    }

    @Test
    fun `when screen shown, then purchase card reader row icon is present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.icon == R.drawable.ic_shopping_cart
            }
    }

    @Test
    fun `given supported country, when screen shown, then manual card reader row is present`() {
        val supportedCountry: CardReaderConfig = CardReaderConfigForUSA
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")

        initViewModel()

        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.icon == R.drawable.ic_card_reader_manual &&
                    it.label == UiStringRes(R.string.settings_card_reader_manuals)
            }
    }

    @Test
    fun `given unsupported country, when screen shown, then manual card reader row is not present`() {
        val unSupportedCountry: CardReaderConfig = CardReaderConfigForUnsupportedCountry
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("BR")).thenReturn(unSupportedCountry)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("BR")

        initViewModel()

        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .noneMatch {
                it.icon == R.drawable.ic_card_reader_manual &&
                    it.label == UiStringRes(R.string.settings_card_reader_manuals)
            }
    }

    @Test
    fun `when user clicks on collect payment, then app navigates to payment collection screen`() {
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_hub_collect_payment)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.getOrAwaitValue())
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToPaymentCollectionScreen
            )
    }

    @Test
    fun `when user clicks on collect payment, then collect payment event tracked`() {
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_hub_collect_payment)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_COLLECT_PAYMENT_TAPPED)
    }

    @Test
    fun `when user clicks on manage card reader, then app navigates to card reader detail screen`() {
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_card_reader)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.getOrAwaitValue())
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderDetail(
                    CardReaderFlowParam.CardReadersHub()
                )
            )
    }

    @Test
    fun `when user clicks on manage card reader, then manage card readers event tracked`() {
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_card_reader)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_MANAGE_CARD_READERS_TAPPED)
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview`() {
        whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
        whenever(paymentMenuUtmProvider.getUrlWithUtmParams(any())).thenReturn(
            "${WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
        )

        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        val event = (
            viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow
            )

        assertThat(event.url)
            .isEqualTo("${WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US")
        assertThat(event.titleRes)
            .isEqualTo(R.string.card_reader_purchase_card_reader)
    }

    @Test
    fun `when user clicks on purchase card reader, then orders card reader event tracked`() {
        whenever(paymentMenuUtmProvider.getUrlWithUtmParams(any())).thenReturn(
            "${WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
        )
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_ORDER_CARD_READER_TAPPED)
    }

    @Test
    fun `when user clicks on purchase card reader, then app opens external webview with in-person-payments link`() {
        val storeCountryCode = wooStore.getStoreCountryCode(selectedSite.get())
        whenever(paymentMenuUtmProvider.getUrlWithUtmParams(any())).thenReturn(
            "$WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY$storeCountryCode"
        )
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_purchase_card_reader)
        }!!.onClick!!.invoke()

        assertThat(
            (viewModel.event.value as CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow).url
        ).isEqualTo("$WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY$storeCountryCode")
    }

    @Test
    fun `given onboarding check error, when user clicks on text, then onboarding shown`() = testBlocking {
        val genericError = mock<CardReaderOnboardingState.GenericError>()
        whenever(cardReaderChecker.getOnboardingState()).thenReturn(
            genericError
        )

        initViewModel()

        viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction!!.onClick.invoke()

        assertThat(viewModel.event.getOrAwaitValue())
            .isEqualTo(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(
                    genericError
                )
            )
    }

    @Test
    fun `given onboarding check error, when user clicks on text, then payments hub tapped tracked`() = testBlocking {
        whenever(cardReaderChecker.getOnboardingState()).thenReturn(
            mock<CardReaderOnboardingState.GenericError>()
        )

        initViewModel()

        viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction!!.onClick.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_ONBOARDING_ERROR_TAPPED)
    }

    @Test
    fun `when user clicks on manuals row, then app navigates to manuals screen`() {
        val supportedCountry: CardReaderConfig = CardReaderConfigForUSA
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")

        initViewModel()

        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.settings_card_reader_manuals)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.getOrAwaitValue())
            .isInstanceOf(
                CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen::class.java
            )
    }

    @Test
    fun `when user clicks on manuals row, then click on manuals tracked`() {
        val supportedCountry: CardReaderConfig = CardReaderConfigForUSA
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")

        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.settings_card_reader_manuals)
        }!!.onClick!!.invoke()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED)
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

        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
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

        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
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
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

        assertThat(viewModel.event.getOrAwaitValue()).isEqualTo(
            CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen(
                CardReaderOnboardingState.ChoosePaymentGatewayProvider
            )
        )
    }

    @Test
    fun `given multiple plugins installed, when change payment provider clicked, then invalidate cache invoked`() {
        val site = selectedSite.get()
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                selfHostedSiteId = site.selfHostedSiteId
            )
        ).thenReturn(true)

        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

        verify(cardReaderChecker).invalidateCache()
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
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

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
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
        }!!.onClick!!.invoke()

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

        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .noneMatch {
                it.label == UiStringRes(R.string.card_reader_manage_payment_provider)
            }
    }

    @Test
    fun `given multiple plugins installed but not selected, when view model init, then error`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.ChoosePaymentGatewayProvider>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction?.text).isEqualTo(
                UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding error, when view model init, then show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction?.text).isEqualTo(
                UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )
        }

    @Test
    fun `given onboarding complete, when view model init, then do not show error message`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction).isNull()
        }

    @Test
    fun `given onboarding error, when screen shown, then manage card reader row disabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
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
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
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
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_hub_collect_payment)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding error, when screen shown, then card reader manual is enabled`() =
        testBlocking {
            val supportedCountry: CardReaderConfig = CardReaderConfigForUSA
            whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")

            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.settings_card_reader_manuals)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given onboarding status changed to competed, when screen shown again, then onboarding error hidden`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.GenericError>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction?.text).isEqualTo(
                UiStringRes(R.string.card_reader_onboarding_not_finished, containsHtml = true)
            )

            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )

            viewModel.onViewVisible()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction?.text).isNull()
        }

    @Test
    fun `given pending requirements status, when screen shown, then onboarding error visible`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(viewModel.viewStateData.getOrAwaitValue().onboardingErrorAction?.text).isEqualTo(
                UiStringRes(R.string.card_reader_onboarding_with_pending_requirements, containsHtml = true)
            )
        }

    @Test
    fun `given pending requirements status, when screen shown, then collect payment row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_hub_collect_payment)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then order card reader row is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_purchase_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then card reader manuals is enabled`() =
        testBlocking {
            val supportedCountry: CardReaderConfig = CardReaderConfigForUSA
            whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US")).thenReturn(supportedCountry)
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")

            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.settings_card_reader_manuals)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    @Test
    fun `given pending requirements status, when screen shown, then manage card reader is enabled`() =
        testBlocking {
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<StripeAccountPendingRequirement>()
            )

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_manage_card_reader)
                    }
                        as NonToggleableListItem
                    ).isEnabled
            ).isTrue()
        }

    // region cash on delivery
    @Test
    fun `when screen shown, then cash on delivery row present`() {
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
            .anyMatch {
                it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
            }
    }

    @Test
    fun `when screen shown, then cod row present with correct icon`() =
        testBlocking {
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows)
                .anyMatch {
                    it.icon == R.drawable.ic_gridicons_credit_card
                }
        }

    @Test
    fun `when screen shown, then cash on delivery row present with correct description`() {
        assertThat(
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).description
        ).isEqualTo(
            UiStringRes(
                R.string.card_reader_enable_pay_in_person_description,
                containsHtml = true
            )
        )
    }

    @Test
    fun `when screen shown, then cash on delivery is disabled`() {
        assertThat(
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).isChecked
        ).isFalse
    }

    @Test
    fun `when screen shown, then cash on delivery is allowed to toggle`() {
        assertThat(
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).isEnabled
        ).isTrue
    }

    @Test
    fun `given cash on delivery enabled, when screen shown, then cash on delivery state is enabled`() =
        testBlocking {
            whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(true)

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isTrue()
        }

    @Test
    fun `given cash on delivery disabled, when screen shown, then cash on delivery state is disabled`() =
        testBlocking {
            whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(false)

            initViewModel()

            assertThat(
                (
                    viewModel.viewStateData.getOrAwaitValue().rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isFalse
        }

    @Test
    fun `given cash on delivery api in progress, when cod toggled, then cash on delivery state not allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[1].rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isFalse
        }

    @Test
    fun `given cash on delivery api success, when cod toggled, then cash on delivery state is allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isTrue
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then cash on delivery state is allowed to click`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isEnabled
            ).isTrue
        }

    @Test
    fun `given cash on delivery api success, when cod toggled, then isChecked is set to correct value`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isTrue
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then isChecked is reverted back to old value`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )
            val receivedViewStates = mutableListOf<CardReaderHubViewState>()
            viewModel.viewStateData.observeForever {
                receivedViewStates.add(it)
            }

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(
                (
                    receivedViewStates[2].rows.find {
                        it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                    }
                        as ToggleableListItem
                    ).isChecked
            ).isFalse
        }

    @Test
    fun `given cash on delivery api success, when cod enabled, then track cod success event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getSuccessWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryEnabledSuccess(PAYMENTS_HUB)
        }

    @Test
    fun `given cash on delivery api failure, when cod enabled, then track cod failure event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryEnabledFailure(
                PAYMENTS_HUB,
                "Toggling COD failed. Please try again later"
            )
        }

    @Test
    fun `given cash on delivery api success, when cod disabled, then do not track cod success event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(false)
            ).thenReturn(
                getSuccessWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(false)

            // THEN
            verify(cardReaderTracker, never()).trackCashOnDeliveryEnabledSuccess(PAYMENTS_HUB)
        }

    @Test
    fun `given cash on delivery api failure, when cod disabled, then do not track cod failure event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(false)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(false)

            // THEN
            verify(cardReaderTracker, never()).trackCashOnDeliveryEnabledFailure(
                PAYMENTS_HUB,
                "Toggling COD failed. Please try again later"
            )
        }

    @Test
    fun `given cash on delivery api failure, when cod disabled, then track cod disabled failure event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(false)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(false)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryDisabledFailure(
                PAYMENTS_HUB,
                "Toggling COD failed. Please try again later"
            )
        }

    @Test
    fun `given cash on delivery api success, when cod disabled, then track cod disabled success event`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(false)
            ).thenReturn(
                getSuccessWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(false)

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryDisabledSuccess(
                PAYMENTS_HUB
            )
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then show toast event is triggered`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isInstanceOf(ShowToastString::class.java)
        }

    @Test
    fun `given cash on delivery api failure, when cod toggled, then show toast event with correct message is fired`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult()
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isEqualTo(
                ShowToastString(
                    "Toggling COD failed. Please try again later"
                )
            )
        }

    @Test
    fun `given cod api failure with null message, when cod toggled, then toast event with default message is fired`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult(message = null)
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isEqualTo(
                ShowToast(R.string.something_went_wrong_try_again)
            )
        }

    @Test
    fun `given cod api failure with empty message, when cod toggled, then toast event with default message is fired`() =
        testBlocking {
            // GIVEN
            whenever(
                cashOnDeliverySettingsRepository.toggleCashOnDeliveryOption(true)
            ).thenReturn(
                getFailureWooResult(message = "")
            )

            // WHEN
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onToggled.invoke(true)

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isEqualTo(
                ShowToast(R.string.something_went_wrong_try_again)
            )
        }

    @Test
    fun `when cash on delivery learn more clicked, then trigger proper event`() =
        testBlocking {
            // WHEN
            whenever(learnMoreUrlProvider.provideLearnMoreUrlFor(CASH_ON_DELIVERY)).thenReturn(
                AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
            )
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onLearnMoreClicked.invoke()

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isInstanceOf(OpenGenericWebView::class.java)
        }

    @Test
    fun `when cash on delivery learn more clicked, then trigger proper event with correct url`() =
        testBlocking {
            // WHEN
            whenever(learnMoreUrlProvider.provideLearnMoreUrlFor(CASH_ON_DELIVERY)).thenReturn(
                AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
            )
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onLearnMoreClicked.invoke()

            // THEN
            assertThat(viewModel.event.getOrAwaitValue()).isEqualTo(
                OpenGenericWebView(AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY)
            )
        }

    @Test
    fun `when cash on delivery learn more clicked, then track learn more tapped event`() =
        testBlocking {
            // WHEN
            whenever(learnMoreUrlProvider.provideLearnMoreUrlFor(CASH_ON_DELIVERY)).thenReturn(
                AppUrls.WOOCOMMERCE_LEARN_MORE_ABOUT_PAYMENTS_CASH_ON_DELIVERY
            )
            (
                viewModel.viewStateData.getOrAwaitValue().rows.find {
                    it.label == UiStringRes(R.string.card_reader_enable_pay_in_person)
                }
                    as ToggleableListItem
                ).onLearnMoreClicked.invoke()

            // THEN
            verify(cardReaderTracker).trackCashOnDeliveryLearnMoreTapped()
        }

    @Test
    fun `given ttp available, when view model started, then show ttp row with used description`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)

            // WHEN
            initViewModel()

            // THEN
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).anyMatch {
                it is GapBetweenSections && it.index == 4
            }
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).anyMatch {
                it is NonToggleableListItem &&
                    it.icon == R.drawable.ic_baseline_contactless &&
                    it.label == UiStringRes(R.string.card_reader_test_tap_to_pay) &&
                    it.description == UiStringRes(R.string.card_reader_tap_to_pay_description) &&
                    it.index == 5 &&
                    it.iconBadge == R.drawable.ic_badge_new
            }
        }

    @Test
    fun `given ttp available and used and feedback not given, when view model started, then show feedback row`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
            whenever(feedbackRepository.getFeatureFeedbackSetting(FeatureFeedbackSettings.Feature.TAP_TO_PAY))
                .thenReturn(FeatureFeedbackSettings(FeatureFeedbackSettings.Feature.TAP_TO_PAY))

            // WHEN
            initViewModel()

            // THEN
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).anyMatch {
                it is NonToggleableListItem &&
                    it.icon == R.drawable.ic_feedback_banner_logo &&
                    it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback) &&
                    it.description == null &&
                    it.index == 6 &&
                    it.iconBadge == null
            }
        }

    @Test
    fun `given ttp available and used and feedback given more than 30 days ago, when view model started, then dont show feedback row`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
            whenever(feedbackRepository.getFeatureFeedbackSetting(FeatureFeedbackSettings.Feature.TAP_TO_PAY))
                .thenReturn(
                    FeatureFeedbackSettings(
                        FeatureFeedbackSettings.Feature.TAP_TO_PAY,
                        FeatureFeedbackSettings.FeedbackState.GIVEN,
                        Calendar.getInstance().time.time - TimeUnit.DAYS.toMillis(31)
                    )
                )

            // WHEN
            initViewModel()

            // THEN
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
                it is NonToggleableListItem &&
                    it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback)
            }
        }

    @Test
    fun `given ttp available and used and feedback given less than 30 days ago, when view model started, then show feedback row`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
            whenever(feedbackRepository.getFeatureFeedbackSetting(FeatureFeedbackSettings.Feature.TAP_TO_PAY))
                .thenReturn(
                    FeatureFeedbackSettings(
                        FeatureFeedbackSettings.Feature.TAP_TO_PAY,
                        FeatureFeedbackSettings.FeedbackState.GIVEN,
                        Calendar.getInstance().time.time - TimeUnit.DAYS.toMillis(29)
                    )
                )

            // WHEN
            initViewModel()

            // THEN
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).anyMatch {
                it is NonToggleableListItem &&
                    it.icon == R.drawable.ic_feedback_banner_logo &&
                    it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback) &&
                    it.description == null &&
                    it.index == 6
            }
        }

    @Test
    fun `given ttp available and not used, when view model started, then dont show feedback row`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(false)

            // WHEN
            initViewModel()

            // THEN
            assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
                it is NonToggleableListItem &&
                    it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback)
            }
        }

    @Test
    fun `when learn more ipp clicked, then learn more button tracked with source`() =
        testBlocking {
            // GIVEN
            val url = "https://www.example.com"
            whenever(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
                )
            ).thenReturn(url)
            initViewModel()

            // WHEN
            (viewModel.viewStateData.getOrAwaitValue()).rows
                .first { it is CardReaderHubViewState.ListItem.LearnMoreListItem }
                .onClick!!.invoke()

            // THEN
            verify(cardReaderTracker).trackIPPLearnMoreClicked("payments_menu")
        }

    @Test
    fun `when learn more ipp clicked, then open web view event emitted`() =
        testBlocking {
            // GIVEN
            val url = "https://www.example.com"
            whenever(
                learnMoreUrlProvider.provideLearnMoreUrlFor(
                    LearnMoreUrlProvider.LearnMoreUrlType.IN_PERSON_PAYMENTS
                )
            ).thenReturn(url)
            initViewModel()

            // WHEN
            (viewModel.viewStateData.getOrAwaitValue()).rows
                .first { it is CardReaderHubViewState.ListItem.LearnMoreListItem }
                .onClick!!.invoke()

            // THEN
            val event = viewModel.event.getOrAwaitValue()
            assertThat((event as OpenGenericWebView).url).isEqualTo(url)
        }

    @Test
    fun `given ttp available and multiple plugin, when view model started, then rows shows sorted by index`() =
        testBlocking {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
            whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
            whenever(cardReaderChecker.getOnboardingState()).thenReturn(
                mock<CardReaderOnboardingState.OnboardingCompleted>()
            )
            whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US"))
                .thenReturn(CardReaderConfigForUSA)
            val site = selectedSite.get()
            whenever(
                appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId
                )
            ).thenReturn(true)
            whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)

            // WHEN
            initViewModel()

            // THEN
            val rows = (viewModel.viewStateData.getOrAwaitValue()).rows
            assertThat(rows.map { it.index }).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        }

    @Test
    fun `given ttp system not supported, when view model started, then do not show ttp row`() = testBlocking {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(SystemVersionNotSupported)

        // WHEN
        initViewModel()

        // THEN
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }
    }

    @Test
    fun `given ttp gps not available, when view model started, then do not show ttp row`() = testBlocking {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(GooglePlayServicesNotAvailable)

        // WHEN
        initViewModel()

        // THEN
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }
    }

    @Test
    fun `given ttp nfc not available, when view model started, then do not show ttp row`() = testBlocking {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(NfcNotAvailable)

        // WHEN
        initViewModel()

        // THEN
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }
    }

    @Test
    fun `given ttp country not supported, when view model started, then do not show ttp row`() = testBlocking {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("CA")
        whenever(tapToPayAvailabilityStatus()).thenReturn(CountryNotSupported)

        // WHEN
        initViewModel()

        // THEN
        assertThat((viewModel.viewStateData.getOrAwaitValue()).rows).noneMatch {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }
    }
    // endregion

    @Test
    fun `given tpp available, when tap to pay clicked, then navigate to tap to pay summary screen event emitted`() {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)

        // WHEN
        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }!!.onClick!!.invoke()

        // THEN
        assertThat(viewModel.event.value).isInstanceOf(NavigateToTapTooPaySummaryScreen::class.java)
    }

    @Test
    fun `given tpp available, when tap to pay clicked, then tap is tracked`() {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)

        // WHEN
        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_test_tap_to_pay)
        }!!.onClick!!.invoke()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_TAPPED)
    }

    @Test
    fun `when view model initiated, then only ttp non toggleable item has description`() {
        // WHEN
        initViewModel()

        // THEN
        val rows = (viewModel.viewStateData.getOrAwaitValue()).rows
        assertThat(
            rows.filterIsInstance<NonToggleableListItem>()
                .filter { it.label != UiStringRes(R.string.card_reader_test_tap_to_pay) }
                .map { it.description }
        ).allMatch { it == null }
    }

    @Test
    fun `when view model initiated, then learn more item visible`() {
        // WHEN
        initViewModel()

        // THEN
        val rows = (viewModel.viewStateData.getOrAwaitValue()).rows
        val learnMoreListItems = rows.filterIsInstance<CardReaderHubViewState.ListItem.LearnMoreListItem>()
        assertThat(learnMoreListItems).hasSize(1)
        assertThat(learnMoreListItems[0].label).isEqualTo(
            UiStringRes(
                R.string.card_reader_detail_learn_more,
                containsHtml = true
            )
        )
        assertThat(learnMoreListItems[0].icon).isEqualTo(R.drawable.ic_info_outline_20dp)
        assertThat(learnMoreListItems[0].index).isEqualTo(11)
    }

    @Test
    fun `given hub flow with ttp, when view model initiated, then navigate to ttp emitted`() {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)

        // WHEN
        initViewModel(OpenInHub.TAP_TO_PAY_SUMMARY)

        // THEN
        assertThat(viewModel.event.value).isInstanceOf(NavigateToTapTooPaySummaryScreen::class.java)
    }

    @Test
    fun `given hub flow with ttp when ttp is not available, when view model initiated, then handled by ttp availability handler`() {
        // GIVEN
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(SystemVersionNotSupported)

        // WHEN
        initViewModel(OpenInHub.TAP_TO_PAY_SUMMARY)

        // THEN
        verify(cardReaderHubTapToPayUnavailableHandler).handleTTPUnavailable(
            eq(SystemVersionNotSupported),
            any(),
            any(),
        )
        verify(cardReaderTracker).trackTapToPayNotAvailableReason(
            SystemVersionNotSupported,
            "payments_menu",
        )
    }

    @Test
    fun `given hub flow with none, when view model initiated, then navigate `() {
        // WHEN
        initViewModel(OpenInHub.NONE)

        // THEN
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `given ttp used and feedback not given, when on survey tapped, then navigate to tap to pay feedback screen event emitted`() {
        // GIVEN
        whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US"))
            .thenReturn(CardReaderConfigForUSA)

        // WHEN
        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback)
        }!!.onClick!!.invoke()

        // THEN
        assertThat(viewModel.event.value).isInstanceOf(NavigateToTapTooPaySurveyScreen::class.java)
    }

    @Test
    fun `given ttp used and feedback not given, when on survey tapped, then navigate tap is tracked`() {
        // GIVEN
        whenever(feedbackRepository.getFeatureFeedbackSetting(FeatureFeedbackSettings.Feature.TAP_TO_PAY))
            .thenReturn(FeatureFeedbackSettings(FeatureFeedbackSettings.Feature.TAP_TO_PAY))
        whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US"))
            .thenReturn(CardReaderConfigForUSA)

        // WHEN
        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback)
        }!!.onClick!!.invoke()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.PAYMENTS_HUB_TAP_TO_PAY_FEEDBACK_TAPPED)
    }

    @Test
    fun `given ttp used and feedback not given, when on survey tapped, then save that answer is given`() {
        // GIVEN
        whenever(appPrefs.isTTPWasUsedAtLeastOnce()).thenReturn(true)
        whenever(wooStore.getStoreCountryCode(selectedSite.get())).thenReturn("US")
        whenever(tapToPayAvailabilityStatus()).thenReturn(Available)
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US"))
            .thenReturn(CardReaderConfigForUSA)

        // WHEN
        initViewModel()
        (viewModel.viewStateData.getOrAwaitValue()).rows.find {
            it.label == UiStringRes(R.string.card_reader_tap_to_pay_share_feedback)
        }!!.onClick!!.invoke()

        // THEN
        verify(feedbackRepository).saveFeatureFeedback(
            FeatureFeedbackSettings.Feature.TAP_TO_PAY,
            FeatureFeedbackSettings.FeedbackState.GIVEN
        )
    }

    private fun getSuccessWooResult() = WooResult(
        model = WCGatewayModel(
            id = "",
            title = "",
            description = "",
            order = 0,
            isEnabled = true,
            methodTitle = "",
            methodDescription = "",
            features = emptyList()
        )
    )

    private fun getFailureWooResult(
        message: String? = "Toggling COD failed. Please try again later"
    ) = WooResult<WCGatewayModel>(
        error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = message
        )
    )

    private fun initViewModel(openInHub: OpenInHub = OpenInHub.NONE) {
        viewModel = CardReaderHubViewModel(
            CardReaderHubFragmentArgs(
                cardReaderFlowParam = CardReaderFlowParam.CardReadersHub(openInHub),
            ).initSavedStateHandle(),
            appPrefsWrapper,
            selectedSite,
            analyticsTrackerWrapper,
            wooStore,
            cardReaderChecker,
            cashOnDeliverySettingsRepository,
            learnMoreUrlProvider,
            cardReaderCountryConfigProvider,
            cardReaderTracker,
            paymentMenuUtmProvider,
            tapToPayAvailabilityStatus,
            appPrefs,
            feedbackRepository,
            cardReaderHubTapToPayUnavailableHandler,
        )
        viewModel.onViewVisible()
    }
}
