package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCShippingMethod
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.EMPTY_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippingsmethods.ShippingMethodsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippingsmethods.ShippingMethodsRestClient.ShippingMethodDto
import org.wordpress.android.fluxc.persistence.dao.ShippingMethodDao
import org.wordpress.android.fluxc.utils.initCoroutineEngine

@ExperimentalCoroutinesApi
class WCShippingMethodsStoreTest {
    private lateinit var sut: WCShippingMethodsStore
    private val shippingMethodsRestClient: ShippingMethodsRestClient = mock()
    private val shippingMethodDao: ShippingMethodDao = mock()
    private val coroutineEngine = initCoroutineEngine()
    private val defaultSiteModel = SiteModel().apply { siteId = 1L }

    @Before
    fun setup() {
        sut = WCShippingMethodsStore(
            restClient = shippingMethodsRestClient,
            shippingMethodDao = shippingMethodDao,
            coroutineEngine = coroutineEngine
        )
    }

    @Test
    fun `when shipping method id is not empty, then the shipping method can be updated`() =
        runBlockingTest {
            val shippingMethod = WCShippingMethod(id = "methodId", title = "methodTitle")

            sut.updateShippingMethod(defaultSiteModel, shippingMethod)

            verify(shippingMethodDao).insertShippingMethods(any())
        }

    @Test
    fun `when shipping method id is empty, then the shipping method CAN'T be updated`() =
        runBlockingTest {
            val shippingMethod = WCShippingMethod(id = "", title = "methodTitle")

            sut.updateShippingMethod(defaultSiteModel, shippingMethod)

            verify(shippingMethodDao, never()).insertShippingMethods(any())
        }

    @Test
    fun `when fetch shipping method success, then response is expected`() = runBlockingTest {
        val methodId = "methodId"
        val methodTitle = "Random Title"
        val expected = WCShippingMethod(methodId, methodTitle)

        val response = ShippingMethodDto(id = methodId, title = methodTitle)
        whenever(shippingMethodsRestClient.fetchShippingMethodsById(defaultSiteModel, methodId))
            .doReturn(WooPayload(response))

        val result = sut.fetchShippingMethod(defaultSiteModel, methodId)

        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        assertThat(result.model).isEqualTo(expected)
    }

    @Test
    fun `when fetch shipping method is null, then response is error`() = runBlockingTest {
        val methodId = "methodId"
        whenever(shippingMethodsRestClient.fetchShippingMethodsById(defaultSiteModel, methodId))
            .doReturn(WooPayload(null))

        val result = sut.fetchShippingMethod(defaultSiteModel, methodId)

        assertThat(result.isError).isTrue
        assertThat(result.model).isNull()
    }

    @Test
    fun `when fetch shipping method fails, then response is error`() = runBlockingTest {
        val methodId = "methodId"
        val error = WooError(EMPTY_RESPONSE, NOT_FOUND)
        whenever(shippingMethodsRestClient.fetchShippingMethodsById(defaultSiteModel, methodId))
            .doReturn(WooPayload(error))

        val result = sut.fetchShippingMethod(defaultSiteModel, methodId)

        assertThat(result.isError).isTrue
        assertThat(result.model).isNull()
    }

    @Test
    fun `when fetch shipping methods success, then response is expected`() = runBlockingTest {
        val methodId = "methodId"
        val methodTitle = "Random Title"
        val expected = listOf(WCShippingMethod(methodId, methodTitle))

        val response = listOf(ShippingMethodDto(methodId, methodTitle))
        whenever(shippingMethodsRestClient.fetchShippingMethods(defaultSiteModel))
            .doReturn(WooPayload(response))

        val result = sut.fetchShippingMethods(defaultSiteModel)

        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        assertThat(result.model).isEqualTo(expected)
    }

    @Test
    fun `when fetch shipping methods is null, then response is error`() = runBlockingTest {
        whenever(shippingMethodsRestClient.fetchShippingMethods(defaultSiteModel))
            .doReturn(WooPayload(null))

        val result = sut.fetchShippingMethods(defaultSiteModel)

        assertThat(result.isError).isTrue
        assertThat(result.model).isNull()
    }

    @Test
    fun `when fetch shipping methods fails, then response is error`() = runBlockingTest {
        val error = WooError(EMPTY_RESPONSE, NOT_FOUND)
        whenever(shippingMethodsRestClient.fetchShippingMethods(defaultSiteModel))
            .doReturn(WooPayload(error))

        val result = sut.fetchShippingMethods(defaultSiteModel)

        assertThat(result.isError).isTrue
        assertThat(result.model).isNull()
    }
}
