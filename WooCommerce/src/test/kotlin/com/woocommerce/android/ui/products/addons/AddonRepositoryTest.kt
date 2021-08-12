package com.woocommerce.android.ui.products.addons

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProduct
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultWCOrderItemList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.fail

class AddonRepositoryTest {
    private lateinit var repositoryUnderTest: AddonRepository

    private lateinit var orderStoreMock: WCOrderStore
    private lateinit var productStoreMock: WCProductStore
    private lateinit var selectedSiteMock: SelectedSite
    private lateinit var siteModelMock: SiteModel
    private lateinit var wcProductModelMock: WCProductModel

    private val localSiteID = 321
    private val remoteOrderID = 123L
    private val remoteProductID = 333L

    @Before
    fun setUp() {
        siteModelMock = mock {
            on { id }.doReturn(321)
        }
        orderStoreMock = mock()
        productStoreMock = mock()
        selectedSiteMock = mock {
            on { get() }.doReturn(siteModelMock)
        }

        repositoryUnderTest = AddonRepository(
            orderStoreMock,
            productStoreMock,
            selectedSiteMock
        )
    }

    @Test
    fun `fetchOrderAddonsData should return both order addons and product addons data`() {
        configureSuccessfulOrderResponse()
        configureSuccessfulProductResponse()

        repositoryUnderTest.fetchOrderAddonsData(123, 333)
            ?.unwrap { productAddons, orderAddons ->
                assertThat(productAddons).isNotEmpty
                assertThat(orderAddons).isNotEmpty
            } ?: fail("non-null Pair with valid data was expected")
    }

    @Test
    fun `fetchOrderAddonsData should map and filter orderAddons keys as List correctly`() {
        configureSuccessfulOrderResponse()
        configureSuccessfulProductResponse()

        val expectedStrings = listOf(
            "Topping ($3,00)",
            "Topping ($4,00)",
            "Topping ($3,00)",
            "Soda ($8,00)",
            "Delivery ($5,00)"
        )

        repositoryUnderTest.fetchOrderAddonsData(123, 333)
            ?.unwrap { _, orderAddons ->
                assertThat(orderAddons).isEqualTo(expectedStrings)
            } ?: fail("non-null Pair with valid data was expected")
    }

    @Test
    fun `fetchOrderAddonsData should return null if product addon data fails`() {
        configureSuccessfulOrderResponse()
        val response = repositoryUnderTest.fetchOrderAddonsData(123, 333)
        assertThat(response).isNull()
    }

    @Test
    fun `fetchOrderAddonsData should return null if order addon data fails`() {
        configureSuccessfulProductResponse()
        val response = repositoryUnderTest.fetchOrderAddonsData(123, 333)
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

    private fun configureSuccessfulProductResponse() {
        whenever(
            productStoreMock.getProductByRemoteId(
                siteModelMock, remoteProductID
            )
        ).thenReturn(defaultProduct)
    }
}
