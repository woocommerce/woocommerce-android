package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultAddonWithOptionsList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderAttributes
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultWCOrderItemList
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultWCProductModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCAddonsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.fail

@ExperimentalCoroutinesApi
class AddonRepositoryTest {
    private lateinit var repositoryUnderTest: AddonRepository

    private lateinit var orderStoreMock: WCOrderStore
    private lateinit var productStoreMock: WCProductStore
    private lateinit var addonStoreMock: WCAddonsStore
    private lateinit var selectedSiteMock: SelectedSite
    private lateinit var siteModelMock: SiteModel

    private val localSiteID = 321
    private val remoteOrderID = 123L
    private val remoteProductID = 333L

    @Before
    fun setUp() {
        siteModelMock = mock {
            on { id }.doReturn(321)
            on { siteId }.doReturn(321)
        }
        selectedSiteMock = mock {
            on { get() }.doReturn(siteModelMock)
        }
        orderStoreMock = mock()
        productStoreMock = mock()
        addonStoreMock = mock()

        repositoryUnderTest = AddonRepository(
            orderStoreMock,
            productStoreMock,
            addonStoreMock,
            selectedSiteMock
        )
    }

    @Test
    fun `fetchOrderAddonsData should return both order addons and product addons data`() = runBlockingTest {
        configureSuccessfulOrderResponse()
        configureSuccessfulAddonResponse()

        repositoryUnderTest.getOrderAddonsData(123, 999, 333)
            ?.let { (productAddons, orderAddons) ->

                verify(orderStoreMock, times(1))
                    .getOrderByIdentifier(OrderIdentifier(321, 123))

                verify(productStoreMock, times(1))
                    .getProductByRemoteId(siteModelMock, 333)

                assertThat(productAddons).isNotEmpty
                assertThat(orderAddons).isNotEmpty
            } ?: fail("non-null Pair with valid data was expected")
    }

    @Test
    fun `fetchOrderAddonsData should map and filter orderAddons keys as List correctly`() = runBlockingTest {
        configureSuccessfulOrderResponse()
        configureSuccessfulAddonResponse()

        val expectedAddons = defaultOrderAttributes

        repositoryUnderTest.getOrderAddonsData(123, 999, 333)
            ?.let { (_, orderAddons) ->

                verify(orderStoreMock, times(1))
                    .getOrderByIdentifier(OrderIdentifier(321, 123))

                verify(productStoreMock, times(1))
                    .getProductByRemoteId(siteModelMock, 333)

                assertThat(orderAddons).isEqualTo(expectedAddons)
            } ?: fail("non-null Pair with valid data was expected")
    }

    @Test
    fun `fetchOrderAddonsData should map productAddons correctly`() = runBlockingTest {
        configureSuccessfulOrderResponse()
        configureSuccessfulAddonResponse()

        val expectedAddons = defaultAddonWithOptionsList.map { it.toAppModel() }

        repositoryUnderTest.getOrderAddonsData(123, 999, 333)
            ?.let { (productAddons, _) ->

                verify(productStoreMock, times(1))
                    .getProductByRemoteId(siteModelMock, 333)

                verify(addonStoreMock, times(1))
                    .observeAddonsForProduct(321, defaultWCProductModel)

                assertThat(productAddons).isEqualTo(expectedAddons)
            } ?: fail("non-null Pair with valid data was expected")
    }

    @Test
    fun `getAddonsFrom should return addons successfully`() = runBlockingTest {
        configureSuccessfulAddonResponse()

        val expectedAddons = defaultWCProductModel.addons!!

        val actualAddons = repositoryUnderTest.getAddonsFrom(333)

        assertThat(actualAddons).isEqualTo(expectedAddons)
    }

    @Test
    fun `getAddonsFrom should return null when the requested with wrong ID`() = runBlockingTest {
        configureSuccessfulAddonResponse()

        val actualAddons = repositoryUnderTest.getAddonsFrom(999)

        assertThat(actualAddons).isNull()
    }

    @Test
    fun `fetchOrderAddonsData should return null if product addon data fails`() = runBlockingTest {
        configureSuccessfulOrderResponse()
        val response = repositoryUnderTest.getOrderAddonsData(123, 999, 333)
        assertThat(response).isNull()
    }

    @Test
    fun `fetchOrderAddonsData should return null if order addon data fails`() = runBlockingTest {
        configureSuccessfulAddonResponse()
        val response = repositoryUnderTest.getOrderAddonsData(123, 999, 333)
        assertThat(response).isNull()
    }

    @Test
    fun `fetchOrderAddonsData should return null if order item ID is incorrect`() = runBlockingTest {
        configureSuccessfulAddonResponse()
        val response = repositoryUnderTest.getOrderAddonsData(123, 0, 333)
        assertThat(response).isNull()
    }

    private fun configureSuccessfulOrderResponse() {
        mock<WCOrderModel>().apply {
            whenever(getLineItemList()).thenReturn(defaultWCOrderItemList)
        }.let {
            whenever(
                orderStoreMock.getOrderByIdentifier(
                    OrderIdentifier(localSiteID, remoteOrderID)
                )
            ).thenReturn(it)
        }
    }

    private fun configureSuccessfulAddonResponse() {
        whenever(
            productStoreMock.getProductByRemoteId(
                siteModelMock, remoteProductID
            )
        ).thenReturn(defaultWCProductModel)

        whenever(
            addonStoreMock.observeAddonsForProduct(localSiteID.toLong(), defaultWCProductModel)
        ).thenReturn(MutableStateFlow(defaultAddonWithOptionsList))
    }
}
