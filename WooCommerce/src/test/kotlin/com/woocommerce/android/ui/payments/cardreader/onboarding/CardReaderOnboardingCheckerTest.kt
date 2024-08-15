package com.woocommerce.android.ui.payments.cardreader.onboarding

import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_NOT_COMPLETED
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus.CARD_READER_ONBOARDING_PENDING
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.cardreader.config.SupportedExtension
import com.woocommerce.android.cardreader.config.SupportedExtensionType
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.CashOnDeliverySettingsRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState.PluginIsNotSupportedInTheCountry
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType.STRIPE_EXTENSION_GATEWAY
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.STRIPE
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore.InPersonPaymentsPluginType.WOOCOMMERCE_PAYMENTS
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CardReaderOnboardingCheckerTest : BaseUnitTest() {
    private lateinit var checker: CardReaderOnboardingChecker

    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wcInPersonPaymentsStore: WCInPersonPaymentsStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock()
    private val cashOnDeliverySettingsRepository: CashOnDeliverySettingsRepository = mock()
    private val cardReaderOnboardingCheckResultCache: CardReaderOnboardingCheckResultCache = mock()
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper = mock()
    private val paymentsFlowTracker: PaymentsFlowTracker = mock()

    private val site = SiteModel()

    private val countryCode = "US"
    private val wcPayPluginVersion = "3.3.0"
    private val wcPayPluginVersionCanada = "4.0.0"
    private val stripePluginVersion = "6.6.0"

    @Before
    fun setUp() = testBlocking {
        checker = CardReaderOnboardingChecker(
            selectedSite,
            appPrefsWrapper,
            wooStore,
            wcInPersonPaymentsStore,
            coroutinesTestRule.testDispatchers,
            networkStatus,
            cardReaderTrackingInfoKeeper,
            cardReaderCountryConfigProvider,
            cashOnDeliverySettingsRepository,
            cardReaderOnboardingCheckResultCache,
            paymentsFlowTracker,
        )
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn(countryCode)
        whenever(wcInPersonPaymentsStore.loadAccount(WOOCOMMERCE_PAYMENTS, site))
            .thenReturn(buildPaymentAccountResult())
        whenever(wcInPersonPaymentsStore.loadAccount(STRIPE, site))
            .thenReturn(buildPaymentAccountResult())
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf(buildWCPayPluginInfo())))
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("US"))
            .thenReturn(CardReaderConfigForUSA)
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("us"))
            .thenReturn(CardReaderConfigForUSA)
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("CA"))
            .thenReturn(CardReaderConfigForCanada)
        whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(true)
    }

    @Test
    fun `when not connected to network, then NO_CONNECTION returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(false)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when connected to network, then NO_CONNECTION not returned`() = testBlocking {
        whenever(networkStatus.isConnected()).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.NoConnectionError::class.java)
    }

    @Test
    fun `when store country not supported, then STORE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `when store country supported, then STORE_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given country in lower case, when store country supported, then STORE_COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("us")

            val result = checker.getOnboardingState()

            assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
        }

    @Test
    fun `when country code is not found, then STORE_COUNTRY_NOT_SUPPORTED returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
        }

    @Test
    fun `given country not supported, then stripe account loading does not even start`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

            checker.getOnboardingState()

            verify(wcInPersonPaymentsStore, never()).loadAccount(anyOrNull(), anyOrNull())
        }

    @Test
    fun `when account country not supported, then STRIPE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                countryCode = "unsupported country abc",
            )
        )
        whenever(cardReaderCountryConfigProvider.provideCountryConfigFor("unsupported country abc"))
            .thenReturn(CardReaderConfigForUnsupportedCountry)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
    }

    @Test
    fun `when account country supported, then ACCOUNT_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                countryCode = "US"
            )
        )

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
    }

    @Test
    fun `given country in lower case, when country supported, then ACCOUNT_COUNTRY_NOT_SUPPORTED not returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    countryCode = "us"
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StripeAccountCountryNotSupported::class.java)
        }

    @Test
    fun `given wcpay installed and activated, when stripe is not installed, then onboarding complete with wcpay`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    wcPayPluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `given enabled account status and wcpay installed and activated, when getOnboardingState, then onboarding complete with wcpay`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.ENABLED
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    wcPayPluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `given enabled account status and stripe installed and activated, when getOnboardingState, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.ENABLED
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    STRIPE_EXTENSION_GATEWAY,
                    stripePluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `given stripe installed and activated, when wcpay is not installed, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    STRIPE_EXTENSION_GATEWAY,
                    stripePluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `when stripe extension plugin outdated, then UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(version = "2.8.1"),
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.PluginUnsupportedVersion(STRIPE_EXTENSION_GATEWAY)
            )
        }

    @Test
    fun `given store in Canada, when wcpay plugin outdated, then UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(version = wcPayPluginVersion)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS)
            )
        }

    @Test
    fun `given stripe extension plugin, when stripe account not connected, then SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.SetupNotCompleted(
                    STRIPE_EXTENSION_GATEWAY
                )
            )
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is not activated, then onboarding complete returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = false),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is not activated, then onboarding complete with wcpay`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = false),
                        buildWCPayPluginInfo(isActive = true),
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS,
                    wcPayPluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `given wcpay and stripe are installed, when both not activated, then wcpay NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = false),
                        buildWCPayPluginInfo(isActive = false)
                    )
                ),
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotActivated)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is activated, then onboarding complete returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = false)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is activated, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = false),
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    STRIPE_EXTENSION_GATEWAY,
                    stripePluginVersion,
                    countryCode
                )
            )
        }

    @Test
    fun `given wcpay and stripe are installed in RU, when get state, then country code RU returned`() =
        testBlocking {
            val countryCode = "RU"
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(countryCode)
            val cardReaderConfigForSupportedCountry = mock<CardReaderConfigForSupportedCountry>()
            whenever(cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode))
                .thenReturn(cardReaderConfigForSupportedCountry)
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat((result as PluginIsNotSupportedInTheCountry).countryCode).isEqualTo(countryCode)
        }

    @Test
    fun `given wcpay and stripe are installed in RU with Stripe support, when get state, then WCPay returned`() =
        testBlocking {
            val countryCode = "RU"
            whenever(wooStore.getStoreCountryCode(site)).thenReturn(countryCode)
            val cardReaderConfigForSupportedCountry = mock<CardReaderConfigForSupportedCountry> {
                on { this.supportedExtensions }.thenReturn(
                    listOf(
                        SupportedExtension(
                            type = SupportedExtensionType.STRIPE,
                            supportedSince = "4.0.0"
                        ),
                    ),
                )
            }
            whenever(cardReaderCountryConfigProvider.provideCountryConfigFor(countryCode))
                .thenReturn(cardReaderConfigForSupportedCountry)
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat((result as PluginIsNotSupportedInTheCountry).preferredPlugin).isEqualTo(
                PluginType.WOOCOMMERCE_PAYMENTS
            )
        }

    @Test
    fun `when woocommerce payments plugin not installed, then WCPAY_NOT_INSTALLED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotInstalled)
        }

    @Test
    fun `when woocommerce payments plugin outdated, then WCPAY_UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(version = "2.8.1")
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS)
            )
        }

    @Test
    fun `when woocommerce payments plugin not active, then WCPAY_NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(buildWCPayPluginInfo(isActive = false))
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotActivated)
        }

    @Test
    fun `when stripe account not connected, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.SetupNotCompleted(
                    PluginType.WOOCOMMERCE_PAYMENTS
                )
            )
        }

    @Test
    fun `when stripe account under review, then WCPAY_SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountUnderReview::class.java)
        }

    @Test
    fun `when stripe account pending requirements, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account restricted soon, then STRIPE_ACCOUNT_PENDING_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED_SOON,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = false
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountPendingRequirement::class.java)
        }

    @Test
    fun `when stripe account has overdue requirements, then STRIPE_ACCOUNT_OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED,
                    hasPendingRequirements = false,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountOverdueRequirement::class.java)
        }

    @Test
    fun `when stripe account has both pending and overdue requirements, then OVERDUE_REQUIREMENT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED,
                    hasPendingRequirements = true,
                    hadOverdueRequirements = true
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountOverdueRequirement::class.java)
        }

    @Test
    fun `when stripe account marked as fraud, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_FRAUD,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountRejected::class.java)
        }

    @Test
    fun `when stripe account listed, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_LISTED,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountRejected::class.java)
        }

    @Test
    fun `when stripe account violates terms of service, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_TERMS_OF_SERVICE,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountRejected::class.java)
        }

    @Test
    fun `when stripe account rejected for other reasons, then STRIPE_ACCOUNT_REJECTED returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_OTHER,
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.StripeAccountRejected::class.java)
        }

    @Test
    fun `when test mode enabled on site with live account, then WcpayInTestModeWithLiveStripeAccount returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount::class.java)
        }

    @Test
    fun `when test mode disabled on site with live account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result)
                .isNotInstanceOf(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount::class.java)
        }

    @Test
    fun `when test mode disabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result)
                .isNotInstanceOf(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount::class.java)
        }

    @Test
    fun `when test mode enabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result)
                .isNotInstanceOf(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount::class.java)
        }

    @Test
    fun `when test mode flag not supported, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(testModeEnabled = null)
            )

            val result = checker.getOnboardingState()

            assertThat(result)
                .isNotInstanceOf(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount::class.java)
        }

    @Test
    fun `given wcpay installed, when onboarding check completed, then onboarding completed status saved and cached`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true),
                    )
                )
            )

            checker.getOnboardingState()

            val captor = argumentCaptor<PersistentOnboardingData>()
            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                captor.capture(),
            )
            assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_COMPLETED)
            verify(cardReaderOnboardingCheckResultCache).value =
                CardReaderOnboardingCheckResultCache.Result.Cached(
                    CardReaderOnboardingState.OnboardingCompleted(
                        PluginType.WOOCOMMERCE_PAYMENTS,
                        wcPayPluginVersion,
                        countryCode,
                    )
                )
        }

    @Test
    fun `given wcpay installed, when onboarding pending, then onboarding pending status saved but status not cached`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    hasPendingRequirements = true,
                    status = WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
                )
            )

            checker.getOnboardingState()

            val captor = argumentCaptor<PersistentOnboardingData>()
            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                captor.capture(),
            )
            assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_PENDING)
            verify(cardReaderOnboardingCheckResultCache, never()).value = any()
        }

    @Test
    fun `given wcpay installed, when onboarding pending, then wcpay version saved but status not cached`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true),
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    hasPendingRequirements = true,
                    status = WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
                )
            )

            checker.getOnboardingState()

            val captor = argumentCaptor<PersistentOnboardingData>()
            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                captor.capture(),
            )
            assertThat(captor.firstValue.version).isEqualTo(wcPayPluginVersion)
            verify(cardReaderOnboardingCheckResultCache, never()).value = any()
        }

    @Test
    fun `given stripe ext installed, when onboarding completed, then stripe version saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildStripeExtensionPluginInfo(isActive = true)
                )
            )
        )

        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_COMPLETED)
    }

    @Test
    fun `given stripe ext installed, when onboarding completed, then onboarding status saved and status is cached`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf(buildStripeExtensionPluginInfo())))

            checker.getOnboardingState()

            val captor = argumentCaptor<PersistentOnboardingData>()
            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                captor.capture(),
            )
            assertThat(captor.firstValue.version).isEqualTo(stripePluginVersion)
            verify(cardReaderOnboardingCheckResultCache).value =
                CardReaderOnboardingCheckResultCache.Result.Cached(
                    CardReaderOnboardingState.OnboardingCompleted(
                        STRIPE_EXTENSION_GATEWAY,
                        stripePluginVersion,
                        countryCode,
                    )
                )
        }

    @Test
    fun `given stripe ext installed, when onboarding pending, then pending status saved but not cached`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    hasPendingRequirements = true,
                    status = WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
                )
            )

            checker.getOnboardingState()

            val captor = argumentCaptor<PersistentOnboardingData>()
            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                captor.capture(),
            )
            assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_PENDING)
            verify(cardReaderOnboardingCheckResultCache, never()).value = any()
        }

    @Test
    fun `when payment account loads, then statement descriptor saved`() = testBlocking {
        val expected = "Woo Site Test"
        whenever(wcInPersonPaymentsStore.loadAccount(WOOCOMMERCE_PAYMENTS, site))
            .thenReturn(buildPaymentAccountResult(statementDescriptor = expected))

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderStatementDescriptor(
            eq(expected),
            anyInt(),
            anyLong(),
            anyLong(),
        )
    }

    @Test
    fun `when onboarding NOT completed, then onboarding completed NOT saved`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_TERMS_OF_SERVICE,
            )
        )
        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_NOT_COMPLETED)
    }

    @Test
    fun `when onboarding completed using wcpay, then wcpay plugin type saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )

        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.preferredPlugin).isEqualTo(PluginType.WOOCOMMERCE_PAYMENTS)
    }

    @Test
    fun `given stripe ext active, when verifying state, then stripe ext account endpoint used`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildStripeExtensionPluginInfo(isActive = true)
                )
            ),
        )

        checker.getOnboardingState()

        verify(wcInPersonPaymentsStore).loadAccount(eq(STRIPE), any())
    }

    @Test
    fun `given wcpay active, when verifying state, then wcpay account endpoint used`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(buildWCPayPluginInfo(isActive = true))
            )
        )

        checker.getOnboardingState()

        verify(wcInPersonPaymentsStore).loadAccount(eq(WOOCOMMERCE_PAYMENTS), any())
    }

    @Test
    fun `when onboarding completed using stripe, then onboarding completed saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(buildStripeExtensionPluginInfo(isActive = true))
            )
        )

        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_COMPLETED)
    }

    @Test
    fun `when onboarding completed using stripe, then stripe plugin type saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildStripeExtensionPluginInfo(isActive = true)
                )
            )
        )

        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.preferredPlugin).isEqualTo(STRIPE_EXTENSION_GATEWAY)
    }

    @Test
    fun `when onboarding failed due to error, then onboarding not completed is saved`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_OTHER,
            )
        )

        checker.getOnboardingState()

        val captor = argumentCaptor<PersistentOnboardingData>()
        verify(appPrefsWrapper).setCardReaderOnboardingData(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue.status).isEqualTo(CARD_READER_ONBOARDING_NOT_COMPLETED)
    }

    @Test
    fun `given Canada flag true, when store is Canada, then STORE_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given store in UK, when getting onboardign state, then UK stored in tracking keeper`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("UK")

        checker.getOnboardingState()
    }

    @Test
    fun `given Canada store, when stripe ext activated, then plugin is not supported in country returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = false),
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )
            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(PluginIsNotSupportedInTheCountry::class.java)
        }

    @Test
    fun `given US store, when stripe ext activated, then store OnboardingCompleted returned`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = false),
                        buildStripeExtensionPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given Canada store, when wcpay activated, then onboardingcompleted returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true, version = wcPayPluginVersionCanada),
                    buildStripeExtensionPluginInfo(isActive = false)
                )
            )
        )

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
    }

    @Test
    fun `given network error, when fetching plugin, then error returned`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                WooError(
                    WooErrorType.TIMEOUT,
                    BaseRequest.GenericErrorType.HTTP_AUTH_ERROR
                )
            )
        )

        val result = checker.fetchPreferredPlugin()

        assertThat(result).isInstanceOf(PreferredPluginResult.Error::class.java)
    }

    @Test
    fun `given network success and stripe, when fetching plugin, then stripe returned`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildStripeExtensionPluginInfo(isActive = true)
                )
            )
        )

        val result = checker.fetchPreferredPlugin()

        assertThat(result).isInstanceOf(PreferredPluginResult.Success::class.java)
        assertThat((result as PreferredPluginResult.Success).type).isEqualTo(STRIPE_EXTENSION_GATEWAY)
    }

    @Test
    fun `given network success and woo, when fetching plugin, then woo returned`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )

        val result = checker.fetchPreferredPlugin()

        assertThat(result).isInstanceOf(PreferredPluginResult.Success::class.java)
        assertThat((result as PreferredPluginResult.Success).type).isEqualTo(PluginType.WOOCOMMERCE_PAYMENTS)
    }

    //region - Multiple Plugins detected tests

    @Test
    fun `when onboarding not completed, then clear plugin flag`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

        checker.getOnboardingState()

        val captor = argumentCaptor<Boolean>()
        verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
            anyInt(),
            anyLong(),
            anyLong(),
            captor.capture(),
        )
        assertThat(captor.firstValue).isFalse()
    }

    @Test
    fun `given multiple plugins, when pending requirements, then don't clear plugin selected flag`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED,
                hasPendingRequirements = true,
                hadOverdueRequirements = false
            )
        )
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildStripeExtensionPluginInfo(isActive = true),
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        ).thenReturn(true)
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        ).thenReturn(STRIPE_EXTENSION_GATEWAY)

        checker.getOnboardingState()

        verify(appPrefsWrapper, never()).setIsCardReaderPluginExplicitlySelectedFlag(
            anyInt(),
            anyLong(),
            anyLong(),
            anyBoolean(),
        )
    }

    @Test
    fun `given multiple plugins, when onboarding completed, then don't clear plugin selected flag`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true),
                    buildStripeExtensionPluginInfo(isActive = true),
                )
            )
        )
        whenever(
            appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        ).thenReturn(true)
        whenever(
            appPrefsWrapper.getCardReaderPreferredPlugin(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        ).thenReturn(STRIPE_EXTENSION_GATEWAY)

        checker.getOnboardingState()

        verify(appPrefsWrapper, never()).setIsCardReaderPluginExplicitlySelectedFlag(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(false),
        )
    }

    @Test
    fun `when wcpay and stripe activated, then ChoosePaymentProvider returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.ChoosePaymentGatewayProvider)
        }

    @Test
    fun `given no plugin selected & selected flag false,when multiple plugins,then ChoosePaymentProvider returned `() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true),
                    )
                )
            )
            whenever(
                appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(false)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.ChoosePaymentGatewayProvider)
        }

    @Test
    fun `given plugin selected, when multiple plugins, then set plugin flag`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            checker.getOnboardingState(PluginType.WOOCOMMERCE_PAYMENTS)

            verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
                anyInt(),
                anyLong(),
                anyLong(),
                eq(true)
            )
        }

    @Test
    fun `given stripe ext plugin is not supported, when multiple plugins, then use wcpay`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(version = wcPayPluginVersionCanada, isActive = true)
                    )
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given single plugin, when payment gateway feature is enabled, then clear plugin selected flag`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )

            checker.getOnboardingState()

            verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
                anyInt(),
                anyLong(),
                anyLong(),
                eq(false)
            )
        }

    @Test
    fun `when onboarding fails, then clear isPluginExplicitlySelected flag`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("unsupported country abc")

            checker.getOnboardingState()

            verify(appPrefsWrapper).setIsCardReaderPluginExplicitlySelectedFlag(
                anyInt(),
                anyLong(),
                anyLong(),
                eq(false)
            )
        }

    @Test
    fun `given plugin selected, when navigating to onboarding, then select the plugin from shared preference`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )
            whenever(
                appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(true)
            whenever(
                appPrefsWrapper.getCardReaderPreferredPlugin(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(STRIPE_EXTENSION_GATEWAY)

            checker.getOnboardingState()

            verify(appPrefsWrapper).getCardReaderPreferredPlugin(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        }

    @Test
    fun `when navigating to onboarding via payment gateway screen, then store the plugin to the shared preference`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )
            whenever(
                appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(true)
            whenever(
                appPrefsWrapper.getCardReaderPreferredPlugin(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)

            checker.getOnboardingState(PluginType.WOOCOMMERCE_PAYMENTS)

            verify(appPrefsWrapper).setCardReaderOnboardingData(
                anyInt(),
                anyLong(),
                anyLong(),
                eq(
                    PersistentOnboardingData(
                        CARD_READER_ONBOARDING_NOT_COMPLETED,
                        PluginType.WOOCOMMERCE_PAYMENTS,
                        null
                    )
                )
            )
        }

    @Test(expected = IllegalStateException::class)
    fun `given plugin selected flag is true, when no plugin found in shared preference , then throw exception`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildStripeExtensionPluginInfo(isActive = true),
                        buildWCPayPluginInfo(isActive = true)
                    )
                )
            )
            whenever(
                appPrefsWrapper.isCardReaderPluginExplicitlySelected(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(true)
            whenever(
                appPrefsWrapper.getCardReaderPreferredPlugin(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(null)

            checker.getOnboardingState()
        }

    //endregion

    @Test
    fun `when cod enabled, then onboarding state doesn't ask to enable cod`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )
        whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.CashOnDeliveryDisabled::class.java)
    }

    @Test
    fun `when cod disabled, then onboarding state is CashOnDeliveryDisabled`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )
        whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(false)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.CashOnDeliveryDisabled::class.java)
    }

    @Test
    fun `given cod disabled state, when cod disabled skipped, then don't show CashOnDeliveryDisabled`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(
            WooResult(
                listOf(
                    buildWCPayPluginInfo(isActive = true)
                )
            )
        )
        whenever(
            appPrefsWrapper.isCashOnDeliveryDisabledStateSkipped(
                anyInt(),
                anyLong(),
                anyLong(),
            )
        ).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.CashOnDeliveryDisabled::class.java)
    }

    @Test
    fun `given cod disabled state, when cod disabled not skipped, then show CashOnDeliveryDisabled but do not cache result`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(
                    listOf(
                        buildWCPayPluginInfo(isActive = true),
                    )
                ),
            )
            whenever(
                appPrefsWrapper.isCashOnDeliveryDisabledStateSkipped(
                    anyInt(),
                    anyLong(),
                    anyLong(),
                )
            ).thenReturn(false)
            whenever(cashOnDeliverySettingsRepository.isCashOnDeliveryEnabled()).thenReturn(false)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.CashOnDeliveryDisabled::class.java)
            verify(cardReaderOnboardingCheckResultCache, never()).value = any()
        }

    @Test
    fun `given value in cache, when get onboarding state, then return cached value`() = testBlocking {
        val state = mock<CardReaderOnboardingState.OnboardingCompleted>()
        whenever(cardReaderOnboardingCheckResultCache.value).thenReturn(
            CardReaderOnboardingCheckResultCache.Result.Cached(state)
        )

        val result = checker.getOnboardingState()

        assertThat(result).isEqualTo(state)
    }

    @Test
    fun `given value not in cache, when get onboarding state, then saved country set to tracking info keeper`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")

            checker.getOnboardingState()

            verify(cardReaderTrackingInfoKeeper).setCountry(eq("US"))
        }

    @Test
    fun `when stripe account pending verification, then ONBOARDING_COMPLETED returned`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPaymentAccountStatus.PENDING_VERIFICATION,
                hasPendingRequirements = false,
                hadOverdueRequirements = false
            )
        )

        val result = checker.getOnboardingState()

        assertThat(result).isEqualTo(
            CardReaderOnboardingState.OnboardingCompleted(
                PluginType.WOOCOMMERCE_PAYMENTS,
                wcPayPluginVersion,
                countryCode
            )
        )
    }

    @Test
    fun `when stripe account pending verification and stripe is activated, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(
                WooResult(listOf(buildStripeExtensionPluginInfo(isActive = true)))
            )
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(WCPaymentAccountResult.WCPaymentAccountStatus.PENDING_VERIFICATION)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.STRIPE_EXTENSION_GATEWAY,
                    stripePluginVersion,
                    countryCode
                )
            )
        }

    private fun buildPaymentAccountResult(
        status: WCPaymentAccountResult.WCPaymentAccountStatus = WCPaymentAccountResult.WCPaymentAccountStatus.COMPLETE,
        hasPendingRequirements: Boolean = false,
        hadOverdueRequirements: Boolean = false,
        liveAccount: Boolean = true,
        testModeEnabled: Boolean? = false,
        countryCode: String = "US",
        statementDescriptor: String = "",
    ) = WooResult(
        WCPaymentAccountResult(
            status,
            hasPendingRequirements = hasPendingRequirements,
            hasOverdueRequirements = hadOverdueRequirements,
            currentDeadline = null,
            statementDescriptor = statementDescriptor,
            storeCurrencies = WCPaymentAccountResult.WCPaymentAccountStatus.StoreCurrencies("", listOf()),
            country = countryCode,
            isLive = liveAccount,
            testMode = testModeEnabled
        )
    )

    private fun buildWCPayPluginInfo(
        isActive: Boolean = true,
        version: String = wcPayPluginVersion
    ) = SitePluginModel().apply {
        this.version = version
        this.setIsActive(isActive)
        this.name = "woocommerce-payments"
    }

    private fun buildStripeExtensionPluginInfo(
        isActive: Boolean = true,
        version: String = stripePluginVersion
    ) = SitePluginModel().apply {
        this.version = version
        this.setIsActive(isActive)
        this.name = "woocommerce-gateway-stripe"
    }
}
