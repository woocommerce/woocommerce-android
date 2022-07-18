package com.woocommerce.android.ui.banner

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.compose.component.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class BannerDisplayEligibilityCheckerTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker

    private val site = SiteModel()

    @Before
    fun setup() {
        bannerDisplayEligibilityChecker = BannerDisplayEligibilityChecker(
            wooStore,
            appPrefsWrapper,
            coroutinesTestRule.testDispatchers,
            selectedSite
        )
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given upsell banner and store in the US, when purchase reader clicked, then verify url is proper`() {
        runTest {
            // GIVEN
            whenever(wooStore.getStoreCountryCode(any())).thenReturn("US")

            // WHEN
            val actualUrl = bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl()

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
            val actualUrl = bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl()

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

        bannerDisplayEligibilityChecker.onRemindLaterClicked(currentTimeInMillis)

        verify(appPrefsWrapper).setCardReaderUpsellBannerRemindMeLater(
            ArgumentMatchers.eq(currentTimeInMillis),
            ArgumentMatchers.anyInt(),
            ArgumentMatchers.anyLong(),
            ArgumentMatchers.anyLong()
        )
    }

    @Test
    fun `given card reader banner has dismissed forever, then store this info to shared prefs`() {
        bannerDisplayEligibilityChecker.onDontShowAgainClicked()

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
}
