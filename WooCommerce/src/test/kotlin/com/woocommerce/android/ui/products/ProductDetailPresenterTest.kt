package com.woocommerce.android.ui.products

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.CurrencyFormatter
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchSingleProductPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WooCommerceStore

class ProductDetailPresenterTest {
    private val productDetailView: ProductDetailContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val productStore: WCProductStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val uiMessageResolver: UIMessageResolver = mock()
    private val networkStatus: NetworkStatus = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val product = ProductTestUtils.generateProduct()
    private val productRemoteId = product.remoteProductId
    private lateinit var presenter: ProductDetailPresenter

    @Before
    fun setup() {
        presenter = spy(
                ProductDetailPresenter(
                        dispatcher,
                        wooCommerceStore,
                        productStore,
                        selectedSite,
                        uiMessageResolver,
                        networkStatus,
                        currencyFormatter
                )
        )
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays the product detail view correctly`() {
        presenter.takeView(productDetailView)
        doReturn(product).whenever(productStore).getProductByRemoteId(any(), any())
        presenter.loadProductDetail(productRemoteId)
        verify(productDetailView).showProduct(any())
    }

    @Test
    fun `Display error message on fetch product error`() {
        presenter.takeView(productDetailView)
        doReturn(product).whenever(productStore).getProductByRemoteId(any(), any())
        presenter.loadProductDetail(productRemoteId)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchSingleProductPayload>>())

        presenter.onProductChanged(OnProductChanged(0).apply {
            causeOfChange = FETCH_SINGLE_PRODUCT
            error = ProductError()
        })
        verify(productDetailView, times(1)).showFetchProductError()
    }

    @Test
    fun `Do not fetch product from api when not connected`() {
        presenter.takeView(productDetailView)
        doReturn(product).whenever(presenter).getProduct(any())
        doReturn(false).whenever(networkStatus).isConnected()

        presenter.loadProductDetail(productRemoteId)
        verify(presenter, times(1)).getProduct(productRemoteId)
        verify(dispatcher, times(0)).dispatch(any<Action<FetchSingleProductPayload>>())
    }

    @Test
    fun `Shows and hides product detail skeleton correctly`() {
        presenter.takeView(productDetailView)
        presenter.fetchProduct(productRemoteId, true)
        verify(productDetailView).showSkeleton(true)

        presenter.onProductChanged(OnProductChanged(1).apply { causeOfChange = FETCH_SINGLE_PRODUCT })
        verify(productDetailView).showSkeleton(false)
    }
}
