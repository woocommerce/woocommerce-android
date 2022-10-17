package com.woocommerce.android.ui.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_ORDER_LIST
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_REMIND_LATER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_SETTINGS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentCurrencySupportedChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class BannerDisplayEligibilityCheckerTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val cardReaderConfigProvider: CardReaderCountryConfigProvider = mock()
    private val cardReaderPaymentCurrencySupportedChecker: CardReaderPaymentCurrencySupportedChecker = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker

    private val site = SiteModel()

    @Before
    fun setup() {
        bannerDisplayEligibilityChecker = BannerDisplayEligibilityChecker(
            wooStore,
            appPrefsWrapper,
            coroutinesTestRule.testDispatchers,
            selectedSite,
            cardReaderConfigProvider,
            cardReaderPaymentCurrencySupportedChecker,
            analyticsTrackerWrapper,
        )
        whenever(selectedSite.get()).thenReturn(site)
        whenever(
            appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(false)
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(0L)
    }

    @Test
    fun `given upsell banner and store in the US, when purchase reader clicked, then verify url is proper`() {
        runTest {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")

            // WHEN
            val actualUrl = bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(
                KEY_BANNER_PAYMENTS
            )

            // Then
            assertThat(
                actualUrl
            ).isEqualTo("${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US")
        }
    }

    @Test
    fun `given upsell banner and store in the Canada, when purchase reader clicked, then verify url is proper`() {
        runTest {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("CA")

            // WHEN
            val actualUrl = bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(
                KEY_BANNER_PAYMENTS
            )

            // Then
            assertThat(
                actualUrl
            ).isEqualTo("${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}CA")
        }
    }

    @Test
    fun `given banner dismissed and current time in millis less than 14 days, then do not show banner again`() {
        // GIVEN
        val currentTimeInMillis = System.currentTimeMillis()
        val tenDays = (1000 * 60 * 60 * 24 * 10)
        val lastDialogDismissedInMillis = currentTimeInMillis - tenDays
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(lastDialogDismissedInMillis)

        // WHEN
        val result = bannerDisplayEligibilityChecker.isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis)

        // Then
        assertThat(result).isFalse
    }

    @Test
    fun `given banner dismissed and current time in millis greater than 14 days, then show banner again`() {
        // GIVEN
        val currentTimeInMillis = System.currentTimeMillis()
        val fifteenDays = (1000 * 60 * 60 * 24 * 15)
        val lastDialogDismissedInMillis = currentTimeInMillis - fifteenDays
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(lastDialogDismissedInMillis)

        // WHEN
        val result = bannerDisplayEligibilityChecker.isLastDialogDismissedMoreThan14DaysAgo(currentTimeInMillis)

        // Then
        assertThat(result).isTrue
    }

    @Test
    fun `given card reader hasn't dismissed even once, then display upsell card reader banner`() {
        whenever(
            appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(false)
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(0L)

        val canShowBanner = bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(0L)

        assertThat(canShowBanner).isTrue
    }

    @Test
    fun `given card reader has dismissed forever, then don't display upsell card reader banner`() {
        whenever(
            appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(true)

        val canShowBanner = bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(0L)

        assertThat(canShowBanner).isFalse
    }

    @Test
    fun `given card reader has dismissed via remind later, when threshold isn't passed, then don't display banner`() {
        whenever(
            appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(false)
        val currentTimeInMillis = System.currentTimeMillis()
        val tenDays = (1000 * 60 * 60 * 24 * 10)
        val lastDialogDismissedInMillis = currentTimeInMillis - tenDays
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(lastDialogDismissedInMillis)

        val canShowBanner = bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(currentTimeInMillis)

        assertThat(canShowBanner).isFalse
    }

    @Test
    fun `given card reader has dismissed via remind later, when threshold has passed, then display banner`() {
        whenever(
            appPrefsWrapper.isCardReaderUpsellBannerDismissedForever(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(false)
        val currentTimeInMillis = System.currentTimeMillis()
        val fifteenDays = (1000 * 60 * 60 * 24 * 15)
        val lastDialogDismissedInMillis = currentTimeInMillis - fifteenDays
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(lastDialogDismissedInMillis)

        val canShowBanner = bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(currentTimeInMillis)

        assertThat(canShowBanner).isTrue
    }

    @Test
    fun `given card reader has dismissed via remind later, then store current time in millis to shared prefs`() {
        val currentTimeInMillis = System.currentTimeMillis()

        bannerDisplayEligibilityChecker.onRemindLaterClicked(
            currentTimeInMillis,
            KEY_BANNER_PAYMENTS
        )

        verify(appPrefsWrapper).setCardReaderUpsellBannerRemindMeLater(
            ArgumentMatchers.eq(currentTimeInMillis),
            ArgumentMatchers.anyInt(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyLong()
        )
    }

    @Test
    fun `given card reader banner has dismissed forever, then store this info to shared prefs`() {
        bannerDisplayEligibilityChecker.onDontShowAgainClicked(KEY_BANNER_PAYMENTS)

        verify(appPrefsWrapper).setCardReaderUpsellBannerDismissed(
            ArgumentMatchers.eq(true),
            ArgumentMatchers.anyInt(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyLong()
        )
    }

    @Test
    fun `given merchant hasn't dismissed banner via remind later, then has merchant dismissed return false`() {
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(0L)

        val result = bannerDisplayEligibilityChecker.hasTheMerchantDismissedBannerViaRemindMeLater()

        assertThat(result).isFalse
    }

    @Test
    fun `given merchant has dismissed banner via remind later, then has merchant dismissed return true`() {
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(12345L)

        val result = bannerDisplayEligibilityChecker.hasTheMerchantDismissedBannerViaRemindMeLater()

        assertThat(result).isTrue
    }

    @Test
    fun `given store eligible for IPP, then isEligibleForInPersonPayments return true`() {
        runTest {
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")
            whenever(
                cardReaderConfigProvider.provideCountryConfigFor("US")
            ).thenReturn(CardReaderConfigForUSA)
            whenever(
                cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("USD")
            ).thenReturn(true)

            val result = bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()

            assertThat(result).isTrue
        }
    }

    @Test
    fun `given store is not eligible for IPP, then isEligibleForInPersonPayments return false`() {
        runTest {
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("IN")
            whenever(
                cardReaderConfigProvider.provideCountryConfigFor("IN")
            ).thenReturn(CardReaderConfigForUnsupportedCountry)

            val result = bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()

            assertThat(result).isFalse
        }
    }

    @Test
    fun `given store is eligible for IPP, when currency is invalid then isEligibleForInPersonPayments return false`() {
        runTest {
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("CA")
            whenever(
                cardReaderConfigProvider.provideCountryConfigFor("CA")
            ).thenReturn(CardReaderConfigForCanada)
            whenever(
                cardReaderPaymentCurrencySupportedChecker.isCurrencySupported(any())
            ).thenReturn(false)

            val result = bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()

            assertThat(result).isFalse
        }
    }

    @Test
    fun `given upsell banner, when banner is not eligible for display, then do not track event`() {
        // WHEN
        whenever(
            appPrefsWrapper.getCardReaderUpsellBannerLastDismissed(
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(12345L)

        bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(0L)

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            AnalyticsEvent.FEATURE_CARD_SHOWN,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_PAYMENTS,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS
            )
        )
    }

    @Test
    fun `given upsell banner from order list, when shouldTrackEvent is false, then do not track event`() {
        // WHEN
        bannerDisplayEligibilityChecker.canShowCardReaderUpsellBanner(0L)

        // Then
        verify(analyticsTrackerWrapper, never()).track(
            AnalyticsEvent.FEATURE_CARD_SHOWN,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_ORDER_LIST,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS
            )
        )
    }

    @Test
    fun `given payments screen, when banner is dismissed via remind me later, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onRemindLaterClicked(0L, KEY_BANNER_PAYMENTS)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_PAYMENTS,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to true
            )
        )
    }

    @Test
    fun `given order list screen, when banner is dismissed via remind me later, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onRemindLaterClicked(0L, KEY_BANNER_ORDER_LIST)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_ORDER_LIST,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to true
            )
        )
    }

    @Test
    fun `given settings screen, when banner is dismissed via remind me later, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onRemindLaterClicked(0L, KEY_BANNER_SETTINGS)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_SETTINGS,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to true
            )
        )
    }

    @Test
    fun `given payments screen, when banner is dismissed via don't show again, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onDontShowAgainClicked(KEY_BANNER_PAYMENTS)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_PAYMENTS,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to false
            )
        )
    }

    @Test
    fun `given order list screen, when banner is dismissed via don't show again, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onDontShowAgainClicked(KEY_BANNER_ORDER_LIST)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_ORDER_LIST,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to false
            )
        )
    }

    @Test
    fun `given settings screen, when banner is dismissed via don't show again, then track proper event`() {
        // WHEN
        bannerDisplayEligibilityChecker.onDontShowAgainClicked(KEY_BANNER_SETTINGS)

        // Then
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FEATURE_CARD_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_SETTINGS,
                AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                KEY_BANNER_REMIND_LATER to false
            )
        )
    }

    @Test
    fun `given payments screen, when banner cta is tapped, then track proper event`() {
        runTest {
            // WHEN
            bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_PAYMENTS)

            // Then
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.FEATURE_CARD_CTA_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_PAYMENTS,
                    AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                )
            )
        }
    }

    @Test
    fun `given order list screen, when banner cta is tapped, then track proper event`() {
        runTest {
            // WHEN
            bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_ORDER_LIST)

            // Then
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.FEATURE_CARD_CTA_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_ORDER_LIST,
                    AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                )
            )
        }
    }

    @Test
    fun `given settings screen, when banner cta is tapped, then track proper event`() {
        runTest {
            // WHEN
            bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_SETTINGS)

            // Then
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.FEATURE_CARD_CTA_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_BANNER_SOURCE to KEY_BANNER_SETTINGS,
                    AnalyticsTracker.KEY_BANNER_CAMPAIGN_NAME to AnalyticsTracker.KEY_BANNER_UPSELL_CARD_READERS,
                )
            )
        }
    }
}
