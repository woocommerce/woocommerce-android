package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_BANNER_PAYMENTS
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.banner.BannerDisplayEligibilityChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class MainPresenterTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val store: WooCommerceStore = mock()
    private val mainPresenterSettingsContractView: MainSettingsContract.View = mock()
    private val accountStore: AccountStore = mock()
    private val bannerDisplayEligibilityChecker: BannerDisplayEligibilityChecker = mock()

    private lateinit var mainSettingsPresenter: MainSettingsPresenter

    @Before
    fun setup() {
        mainSettingsPresenter = MainSettingsPresenter(
            selectedSite,
            accountStore,
            store,
            mock(),
            mock(),
            bannerDisplayEligibilityChecker,
        )
        mainSettingsPresenter.takeView(mainPresenterSettingsContractView)
    }

    //region Card Reader Upsell
    @Test
    fun `given upsell banner, when purchase reader clicked, then trigger proper event`() {
        runTest {
            // GIVEN
            whenever(
                bannerDisplayEligibilityChecker.getPurchaseCardReaderUrl(KEY_BANNER_PAYMENTS)
            ).thenReturn(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )

            // WHEN
            mainSettingsPresenter.onCtaClicked(KEY_BANNER_PAYMENTS)

            // Then
            verify(mainPresenterSettingsContractView).openPurchaseCardReaderLink(
                "${AppUrls.WOOCOMMERCE_PURCHASE_CARD_READER_IN_COUNTRY}US"
            )
        }
    }

    @Test
    fun `given store not eligible for IPP, then isEligibleForInPersonPayments is false`() {
        runTest {
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(false)

            setup()

            Assertions.assertThat(mainSettingsPresenter.isEligibleForInPersonPayments.value).isFalse
        }
    }

    @Test
    fun `given store eligible for IPP, then isEligibleForInPersonPayments is true`() {
        runTest {
            whenever(bannerDisplayEligibilityChecker.isEligibleForInPersonPayments()).thenReturn(true)

            setup()

            Assertions.assertThat(mainSettingsPresenter.isEligibleForInPersonPayments.value).isTrue
        }
    }
    //endregion
}
