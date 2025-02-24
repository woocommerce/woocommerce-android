package com.woocommerce.android.ui.common.environment

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.network.environment.EnvironmentRestClient
import com.woocommerce.android.network.environment.EnvironmentRestClient.EnvironmentDto
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import kotlin.math.pow

@OptIn(ExperimentalCoroutinesApi::class)
class EnvironmentRepositoryTest : BaseUnitTest() {
    private val site: SiteModel = SiteModel().apply { siteId = 1 }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val environmentRestClient: EnvironmentRestClient = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var environmentRepository: EnvironmentRepository

    @Before
    fun setup() {
        environmentRepository = EnvironmentRepository(
            selectedSite,
            environmentRestClient,
            appPrefsWrapper,
            coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `given store ID is stored locally, when fetchOrGetStoreID is called, then return stored store ID`() =
        testBlocking {
            val storedStoreID = "storeID"
            given(appPrefsWrapper.getWCStoreID(site.siteId)).willReturn(storedStoreID)

            val result = environmentRepository.fetchOrGetStoreID()

            assertThat(result.getOrNull()).isEqualTo(storedStoreID)
            then(environmentRestClient).should(never()).fetchStoreEnvironment(site)
        }

    @Test
    fun `given store ID is not stored locally, when fetchOrGetStoreID is called, then fetch store ID and store it locally`() =
        testBlocking {
            val storeID = "storeID"
            given(appPrefsWrapper.getWCStoreID(site.siteId)).willReturn(null)
            given(environmentRestClient.fetchStoreEnvironment(site)).willReturn(WooPayload(EnvironmentDto(storeID)))

            val result = environmentRepository.fetchOrGetStoreID()

            assertThat(result.getOrNull()).isEqualTo(storeID)
            then(appPrefsWrapper).should().setWCStoreID(site.siteId, storeID)
        }

    @Test
    fun `given store ID fetch fails, when fetchOrGetStoreID is called, then retry using exponential backoff`() =
        testBlocking {
            val storeID = "storeID"
            given(appPrefsWrapper.getWCStoreID(site.siteId)).willReturn(null)
            var stubbing = given(environmentRestClient.fetchStoreEnvironment(site))
            repeat(EnvironmentRepository.MAX_RETRIES - 1) {
                stubbing = stubbing.willReturn(
                    WooPayload(
                        WooError(
                            type = WooErrorType.GENERIC_ERROR,
                            BaseRequest.GenericErrorType.UNKNOWN
                        )
                    )
                )
            }
            stubbing.willReturn(WooPayload(EnvironmentDto(storeID)))

            val result = environmentRepository.fetchOrGetStoreID()

            assertThat(result.getOrNull()).isEqualTo(storeID)
            then(environmentRestClient).should(times(EnvironmentRepository.MAX_RETRIES)).fetchStoreEnvironment(site)
            val expectedDelay = EnvironmentRepository.BACKOFF_RETRY_DELAY *
                EnvironmentRepository.BACKOFF_RETRY_EXPONENTIAL_FACTOR.pow(EnvironmentRepository.MAX_RETRIES - 1)
            assertThat(testScheduler.currentTime).isGreaterThan(expectedDelay.toLong())
        }
}
