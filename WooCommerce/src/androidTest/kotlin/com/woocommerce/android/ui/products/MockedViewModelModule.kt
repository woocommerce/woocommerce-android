package com.woocommerce.android.ui.products

import android.content.Context

import androidx.lifecycle.ViewModelProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.utils.WCSiteUtils
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.store.WooCommerceStore

@Module
internal abstract class MockedViewModelModule {
    @Module
    companion object {
        private var product: WCProductModel? = null

        fun setMockProduct(product: WCProductModel) {
            this.product = product
        }

        @JvmStatic
        @Provides
        fun provideViewModelFactory(): ViewModelProvider.Factory {
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSelectedSite = mock<SelectedSite>()
            val mockNetworkStatus = mock<NetworkStatus>()
            val mockProductRepository = mock<ProductRepository>()
            val mockWcStore = WooCommerceStore(
                    mockContext,
                    mockDispatcher,
                    WooCommerceRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
            )

            val mockedProductDetailViewModel = spy(ProductDetailViewModel(
                    Dispatchers.Main, mockWcStore,
                    mockSelectedSite, mockProductRepository, mockNetworkStatus, mock()
            ))

            doReturn(product?.toAppModel()).whenever(mockProductRepository).getProduct(any())
            doReturn(true).whenever(mockNetworkStatus).isConnected()
            doReturn(WCSiteUtils.generateSiteSettings()).whenever(mockedProductDetailViewModel).getSiteSettings()
            doReturn(WcProductTestUtils.generateProductSettings())
                    .whenever(mockedProductDetailViewModel).getProductSiteSettings()

            val mockFactory = mock<ViewModelProvider.Factory>()

            doReturn(mockedProductDetailViewModel).whenever(mockFactory).create<ProductDetailViewModel>(any())

            return mockFactory
        }
    }
}
