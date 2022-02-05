package com.woocommerce.android.ui.prefs.cardreader.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.InPersonPaymentsCanadaFeatureFlag
import com.woocommerce.android.ui.prefs.cardreader.StripeExtensionFeatureFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCPaymentAccountResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.persistence.WCPluginSqlUtils
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
    private val stripeExtensionFeatureFlag: StripeExtensionFeatureFlag = mock()
    private val inPersonPaymentsCanadaFeatureFlag: InPersonPaymentsCanadaFeatureFlag = mock()
    private val cardReaderConfigFactory: CardReaderConfigFactory = mock()

    private val site = SiteModel()

    @Before
    fun setUp() = testBlocking {
        checker = CardReaderOnboardingChecker(
            selectedSite,
            appPrefsWrapper,
            wooStore,
            wcInPersonPaymentsStore,
            coroutinesTestRule.testDispatchers,
            networkStatus,
            stripeExtensionFeatureFlag,
            inPersonPaymentsCanadaFeatureFlag,
            cardReaderConfigFactory
        )
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
        whenever(wcInPersonPaymentsStore.loadAccount(WOOCOMMERCE_PAYMENTS, site))
            .thenReturn(buildPaymentAccountResult())
        whenever(wcInPersonPaymentsStore.loadAccount(STRIPE, site))
            .thenReturn(buildPaymentAccountResult())
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo())
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(false)
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(CardReaderConfigForUSA)
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
                countryCode = "unsupported country abc"
            )
        )

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
    fun `given both wcpay and stripe installed, when stripe ext feature flag is off, then only wcpay is executed`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(false)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS))
        }

    @Test
    fun `given only stripe ext installed, when stripe ext feature flag is off, then wcpay NOT_INSTALLED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(null)
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(false)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.WcpayNotInstalled
            )
        }

    @Test
    fun `given wcpay installed and activated, when stripe is not installed, then onboarding complete with wcpay`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(null)
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = true))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS))
        }

    @Test
    fun `given stripe installed and activated, when wcpay is not installed, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(null)
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.STRIPE_EXTENSION_GATEWAY
                )
            )
        }

    @Test
    fun `when stripe extension plugin outdated, then UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(version = "2.8.1"))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS)).thenReturn(null)
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.STRIPE_EXTENSION_GATEWAY)
            )
        }

    @Test
    fun `given stripe extension plugin, when stripe account not connected, then SETUP_NOT_COMPLETED returned`() =
        testBlocking {
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS)).thenReturn(null)
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(
                    WCPaymentAccountResult.WCPaymentAccountStatus.NO_ACCOUNT
                )
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.SetupNotCompleted(
                    PluginType.STRIPE_EXTENSION_GATEWAY
                )
            )
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is not activated, then onboarding complete returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = false))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = true))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is not activated, then onboarding complete with wcpay`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = false))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = true))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.OnboardingCompleted(PluginType.WOOCOMMERCE_PAYMENTS))
        }

    @Test
    fun `given wcpay and stripe are installed, when both not activated, then wcpay NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = false))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotActivated)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is activated, then onboarding complete returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
        }

    @Test
    fun `given wcpay and stripe are installed, when stripe is activated, then onboarding complete with stripe`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.OnboardingCompleted(
                    PluginType.STRIPE_EXTENSION_GATEWAY
                )
            )
        }

    @Test
    fun `given wcpay and stripe are installed, when both are activated, then WCPAY_AND_STRIPE_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
                .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = true))
            whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayAndStripeActivated)
        }

    @Test
    fun `when woocommerce payments plugin not installed, then WCPAY_NOT_INSTALLED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS)).thenReturn(null)

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.WcpayNotInstalled)
        }

    @Test
    fun `when woocommerce payments plugin outdated, then WCPAY_UNSUPPORTED_VERSION returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(version = "2.8.1"))

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(
                CardReaderOnboardingState.PluginUnsupportedVersion(PluginType.WOOCOMMERCE_PAYMENTS)
            )
        }

    @Test
    fun `when woocommerce payments plugin not active, then WCPAY_NOT_ACTIVATED returned`() =
        testBlocking {
            whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
            whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
                .thenReturn(buildWCPayPluginInfo(isActive = false))

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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountUnderReview)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountOverdueRequirement)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
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

            assertThat(result).isEqualTo(CardReaderOnboardingState.StripeAccountRejected)
        }

    @Test
    fun `when test mode enabled on site with live account, then WcpayInTestModeWithLiveStripeAccount returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isEqualTo(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode disabled on site with live account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = true, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode disabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = false)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode enabled on site with test account, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(liveAccount = false, testModeEnabled = true)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `when test mode flag not supported, then WcpayInTestModeWithLiveStripeAccount NOT returned`() =
        testBlocking {
            whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
                buildPaymentAccountResult(testModeEnabled = null)
            )

            val result = checker.getOnboardingState()

            assertThat(result).isNotEqualTo(CardReaderOnboardingState.PluginInTestModeWithLiveStripeAccount)
        }

    @Test
    fun `given wcpay installed, when onboarding completed, then onboarding completed status saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = true))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(null)
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.WOOCOMMERCE_PAYMENTS)
        )
    }

    @Test
    fun `given wcpay installed, when onboarding pending, then onboarding pending status saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = true))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(null)
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                hasPendingRequirements = true,
                status = WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
            )
        )
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingPending(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.WOOCOMMERCE_PAYMENTS)
        )
    }

    @Test
    fun `given stripe ext installed, when onboarding completed, then onboarding status saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(null)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.STRIPE_EXTENSION_GATEWAY)
        )
    }

    @Test
    fun `given stripe ext installed, when onboarding pending, then pending status saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(null)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                hasPendingRequirements = true,
                status = WCPaymentAccountResult.WCPaymentAccountStatus.RESTRICTED
            )
        )
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingPending(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.STRIPE_EXTENSION_GATEWAY)
        )
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

        verify(appPrefsWrapper, never()).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            any()
        )
    }

    @Test
    fun `when onboarding completed using wcpay, then onboarding completed plugin type saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(null)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = true))
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.WOOCOMMERCE_PAYMENTS)
        )
    }

    @Test
    fun `given stripe ext active, when verifying state, then stripe ext account endpoint used`() = testBlocking {
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(null)
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(wcInPersonPaymentsStore).loadAccount(eq(STRIPE), any())
    }

    @Test
    fun `given wcpay active, when verifying state, then wcpay account endpoint used`() = testBlocking {
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(null)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = true))
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(wcInPersonPaymentsStore).loadAccount(eq(WOOCOMMERCE_PAYMENTS), any())
    }

    @Test
    fun `when onboarding completed using stripe, then onboarding completed plugin type saved`() = testBlocking {
        whenever(wooStore.fetchSitePlugins(site)).thenReturn(WooResult(listOf()))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(null)
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(PluginType.STRIPE_EXTENSION_GATEWAY)
        )
    }

    @Test
    fun `when onboarding failed due to error, then onboarding completed plugin type is reset`() = testBlocking {
        whenever(wcInPersonPaymentsStore.loadAccount(any(), any())).thenReturn(
            buildPaymentAccountResult(
                WCPaymentAccountResult.WCPaymentAccountStatus.REJECTED_OTHER,
            )
        )

        checker.getOnboardingState()

        verify(appPrefsWrapper).setCardReaderOnboardingCompleted(
            anyInt(),
            anyLong(),
            anyLong(),
            eq(null)
        )
    }

    @Test
    fun `given Canada flag true, when store is Canada, then STORE_COUNTRY_NOT_SUPPORTED not returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)

        val result = checker.getOnboardingState()

        assertThat(result).isNotInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given Canada flag false, when store is Canada, then STORE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(false)
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(CardReaderConfigForCanada)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given Canada store, when stripe ext activated, then STORE_COUNTRY_NOT_SUPPORTED returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = false))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = true))
        whenever(stripeExtensionFeatureFlag.isEnabled()).thenReturn(true)
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(CardReaderConfigForCanada)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.StoreCountryNotSupported::class.java)
    }

    @Test
    fun `given Canada store, when wcpay activated, then onboardingcompleted returned`() = testBlocking {
        whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(inPersonPaymentsCanadaFeatureFlag.isEnabled()).thenReturn(true)
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_PAYMENTS))
            .thenReturn(buildWCPayPluginInfo(isActive = true))
        whenever(wooStore.getSitePlugin(site, WooCommerceStore.WooPlugin.WOO_STRIPE_GATEWAY))
            .thenReturn(buildStripeExtensionPluginInfo(isActive = false))
        whenever(cardReaderConfigFactory.getCardReaderConfigFor(any())).thenReturn(CardReaderConfigForCanada)

        val result = checker.getOnboardingState()

        assertThat(result).isInstanceOf(CardReaderOnboardingState.OnboardingCompleted::class.java)
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
        version: String = SUPPORTED_WCPAY_VERSION
    ) = WCPluginSqlUtils.WCPluginModel(1, 1, isActive, "", "", version)

    private fun buildStripeExtensionPluginInfo(
        isActive: Boolean = true,
        version: String = SUPPORTED_STRIPE_EXTENSION_VERSION
    ) = WCPluginSqlUtils.WCPluginModel(1, 1, isActive, "", "", version)
}
