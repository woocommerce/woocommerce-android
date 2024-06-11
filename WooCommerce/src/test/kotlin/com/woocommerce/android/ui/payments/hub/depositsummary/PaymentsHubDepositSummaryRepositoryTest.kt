package com.woocommerce.android.ui.payments.hub.depositsummary

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.woo.WooPaymentsDepositsOverview
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.store.WCWooPaymentsStore

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsHubDepositSummaryRepositoryTest : BaseUnitTest() {
    private val store: WCWooPaymentsStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val site = SiteModel()

    private val repo = PaymentsHubDepositSummaryRepository(
        store = store,
        site = selectedSite,
    )

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(site)
    }

    @Test
    fun `given store has cache, when retrieveDepositOverview, then cache firstly returned`() = testBlocking {
        // GIVEN
        val overviewCache: WooPaymentsDepositsOverview = mock()
        whenever(store.getDepositsOverviewAll(site)).thenReturn(
            overviewCache
        )

        // WHEN
        val result = repo.retrieveDepositOverview()
        advanceUntilIdle()

        // THEN
        assertThat(result.first()).isEqualTo(
            RetrieveDepositOverviewResult.Cache(overviewCache)
        )
    }

    @Test
    fun `given store has no cache and error from remote, when retrieveDepositOverview, then error returned`() =
        testBlocking {
            // GIVEN
            whenever(store.getDepositsOverviewAll(site)).thenReturn(null)
            val error = WooError(
                type = WooErrorType.API_ERROR,
                original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                message = "message"
            )
            whenever(store.fetchDepositsOverview(site)).thenReturn(
                WooPayload(
                    error = error
                )
            )

            // WHEN
            val result = repo.retrieveDepositOverview()
            advanceUntilIdle()

            // THEN
            assertThat(result.first()).isEqualTo(
                RetrieveDepositOverviewResult.Error(
                    error
                )
            )
            verify(store).deleteDepositsOverview(site)
        }

    @Test
    fun `given store has no cache and success from remote, when retrieveDepositOverview, then success returned`() =
        testBlocking {
            // GIVEN
            whenever(store.getDepositsOverviewAll(site)).thenReturn(null)
            val overview: WooPaymentsDepositsOverview = mock()
            whenever(store.fetchDepositsOverview(site)).thenReturn(
                WooPayload(
                    result = overview
                )
            )

            // WHEN
            val result = repo.retrieveDepositOverview()
            advanceUntilIdle()

            // THEN
            assertThat(result.first()).isEqualTo(
                RetrieveDepositOverviewResult.Remote(
                    overview
                )
            )
            verify(store).insertDepositsOverview(site, overview)
        }
}
