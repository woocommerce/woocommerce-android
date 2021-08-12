package com.woocommerce.android.ui.products.addons

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.extensions.unwrap
import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrder
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderItem
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProduct
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultProductAddon
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
    private lateinit var wcOrderModelMock: WCOrderModel
    private lateinit var wcProductModelMock: WCProductModel

    private val localSiteID = 321
    private val remoteOrderID = 123L
    private val remoteProductID = 333L

    @Before
    fun setUp() {
        siteModelMock = mock {
            on { id }.doReturn(123)
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
    fun `test`() {
        val expectedAttributeList = listOf(
            Attribute("test-key", "test-value")
        )
        configureOrderResponseWith(expectedAttributeList)

        val expectedAddonList = listOf(
            defaultProductAddon.copy(name = "test-addon-name")
        )

        configureProductResponseWith(expectedAddonList)

        repositoryUnderTest.fetchOrderAddonsData(remoteOrderID, remoteProductID)
            ?.unwrap { addons, attributes ->
                assertThat(addons).isNotEmpty
                assertThat(attributes).isNotEmpty
            } ?: fail()
    }

    private fun configureOrderResponseWith(
        expectedAttributeList: List<Attribute>
    ) {
        defaultOrder.copy(
            items = listOf(
                defaultOrderItem.copy(productId = 1),
                defaultOrderItem.copy(productId = 2),
                defaultOrderItem.copy(
                    productId = remoteProductID,
                    attributesList = expectedAttributeList
                )
            )
        ).let { order ->
            wcOrderModelMock = mock<WCOrderModel>()
                .apply { whenever(toAppModel()).doReturn(order) }
            whenever(
                orderStoreMock.getOrderByIdentifier(
                    OrderIdentifier(localSiteID, remoteOrderID)
                )
            ).thenReturn(wcOrderModelMock)
        }

    }

    private fun configureProductResponseWith(
        expectedAddonList: List<ProductAddon>
    ) {
        defaultProduct.copy(
            remoteId = remoteProductID,
            addons = expectedAddonList
        ).let { product ->
            wcProductModelMock = mock<WCProductModel>()
                .apply { whenever(toAppModel()).doReturn(product) }
            whenever(
                productStoreMock.getProductByRemoteId(
                    siteModelMock, remoteProductID
                )
            ).thenReturn(wcProductModelMock)
        }
    }
}
