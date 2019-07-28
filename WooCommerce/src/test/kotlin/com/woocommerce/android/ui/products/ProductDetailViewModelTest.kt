package com.woocommerce.android.ui.products

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class ProductDetailViewModelTest : BaseUnitTest() {
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val product = ProductTestUtils.generateProduct()
    private val productRemoteId = product.remoteId
    private lateinit var viewModel: ProductDetailViewModel

    @Before
    fun setup() {
        viewModel = spy(
                ProductDetailViewModel(
                        Dispatchers.Unconfined,
                        wooCommerceStore,
                        selectedSite,
                        productRepository,
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
        doReturn(product).whenever(productRepository).getProduct(any())

        var observedProduct: Product? = null
        viewModel.product.observeForever { observedProduct = it }

        assertThat(observedProduct).isNull()

        viewModel.start(productRemoteId)

        assertThat(observedProduct).isEqualTo(product)
    }

    @Test
    fun `Display error message on fetch product error`() = test {
        whenever(productRepository.fetchProduct(productRemoteId)).thenReturn(null)
        whenever(productRepository.getProduct(productRemoteId)).thenReturn(null)

        var message: Int? = null
        viewModel.showSnackbarMessage.observeForever { message = it }

        viewModel.start(productRemoteId)

        verify(productRepository, times(1)).fetchProduct(productRemoteId)

        assertThat(message).isEqualTo(R.string.product_detail_fetch_product_error)
    }

    @Test
    fun `Do not fetch product from api when not connected`() = test {
        doReturn(product).whenever(productRepository).getProduct(any())
        doReturn(false).whenever(networkStatus).isConnected()

        var message: Int? = null
        viewModel.showSnackbarMessage.observeForever { message = it }

        viewModel.start(productRemoteId)

        verify(productRepository, times(1)).getProduct(productRemoteId)
        verify(productRepository, times(0)).fetchProduct(any())

        assertThat(message).isEqualTo(R.string.offline_error)
    }

    @Test
    fun `Shows and hides product detail skeleton correctly`() = test {
        doReturn(null).whenever(productRepository).getProduct(any())

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.isSkeletonShown.observeForever { isSkeletonShown.add(it) }

        viewModel.start(productRemoteId)

        assertThat(isSkeletonShown).containsExactly(true, false)
    }
}
