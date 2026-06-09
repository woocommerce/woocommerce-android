package org.wordpress.android.fluxc.wc.taxes

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.taxes.WCTaxClassMapper
import org.wordpress.android.fluxc.model.taxes.WCTaxClassModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient.TaxClassApiResponse
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCTaxStore
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCTaxStoreTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var store: WCTaxStore

    private val restClient = mock<WCTaxRestClient>()
    private val site = SiteModel().apply { id = 321 }

    private val sampleTaxClassResponse: List<TaxClassApiResponse> = TaxTestUtils.generateSampleTaxClassApiResponse()
    private val sampleTaxClassList: List<WCTaxClassModel> =
        sampleTaxClassResponse.map { WCTaxClassMapper.map(site.localId(), it) }

    private val errorSite = SiteModel().apply { id = 123 }
    private val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

    @Before
    fun setUp() {
        store = WCTaxStore(
            restClient,
            initCoroutineEngine(),
            databaseRule.db.taxRateDao,
            databaseRule.db.taxClassDao
        )
    }

    @Test
    fun `get stored tax class list for site`() = runTest {
        fetchTaxClassListForSite()

        val storedTaxClassList = store.getTaxClassListForSite(site)
        assertThat(storedTaxClassList).containsExactlyInAnyOrderElementsOf(sampleTaxClassList)

        val invalidRequestResult = store.getTaxClassListForSite(errorSite)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `when fetch tax rate fails, then error is returned`() = runTest {
        val error = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")
        whenever(restClient.fetchTaxRateList(site, 1, 100)).thenReturn(WooPayload(error))

        val result = store.fetchTaxRateList(site, 1, 100)

        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when fetch tax rate succeeds, then success returns`() = runTest {
        val taxRateApiResponse = TaxTestUtils.generateSampleTaxRateApiResponse()
        whenever(restClient.fetchTaxRateList(site, 1, 100)).thenReturn(WooPayload(taxRateApiResponse))

        val result = store.fetchTaxRateList(site, 1, 100)

        assertThat(result.isError).isFalse
        assertThat(result).isEqualTo(WooResult(false))
    }

    private suspend fun fetchTaxClassListForSite(): WooResult<List<WCTaxClassModel>> {
        val fetchTaxClassListPayload = WooPayload(sampleTaxClassResponse.toTypedArray())
        whenever(restClient.fetchTaxClassList(site)).thenReturn(fetchTaxClassListPayload)
        whenever(restClient.fetchTaxClassList(errorSite)).thenReturn(WooPayload(error))
        return store.fetchTaxClassList(site)
    }
}
