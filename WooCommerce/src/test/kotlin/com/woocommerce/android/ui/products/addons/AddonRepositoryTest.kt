package com.woocommerce.android.ui.products.addons

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrder
import com.woocommerce.android.ui.products.addons.AddonTestFixtures.defaultOrderItem
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore

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
        configureOrderResponse(defaultOrder)
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

        configureOrderResponse(defaultOrder.copy(
            items = listOf(
                defaultOrderItem.copy(productId = 1),
                defaultOrderItem.copy(productId = 2),
                defaultOrderItem.copy(
                    productId = 3,
                    attributesList = expectedAttributeList
                )
            )
        ))

        submitStoreMocks()
    }

    private fun submitStoreMocks() {
        whenever(
            orderStoreMock.getOrderByIdentifier(
                OrderIdentifier(localSiteID, remoteOrderID)
            )
        ).thenReturn(wcOrderModelMock)

        whenever(
            productStoreMock.getProductByRemoteId(
                siteModelMock, remoteProductID
            )
        ).thenReturn(wcProductModelMock)
    }

    private fun configureOrderResponse(order: Order) {
        wcOrderModelMock = mock { on { toAppModel() }.doReturn(order) }
    }

    private fun configureProductResponse(product: Product) {
        wcProductModelMock = mock { on { toAppModel() }.doReturn(product) }
    }
}
