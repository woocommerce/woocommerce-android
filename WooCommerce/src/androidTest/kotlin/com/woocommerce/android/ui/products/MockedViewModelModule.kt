package com.woocommerce.android.ui.products

import androidx.lifecycle.ViewModelProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.model.WCProductModel
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
        fun provideViewModelFactory(
            currencyFormatter: CurrencyFormatter,
            wcStore: WooCommerceStore,
            site: SelectedSite
        ): ViewModelProvider.Factory {
            val mockNetworkStatus = mock<NetworkStatus>()
            val mockProductRepository = mock<ProductRepository>()

            val mockedProductDetailViewModel = spy(
                    ProductDetailViewModel(
                            Dispatchers.Main,
                            wcStore,
                            site,
                            mockProductRepository,
                            mockNetworkStatus,
                            currencyFormatter
                    )
            )

            doReturn(product?.toAppModel()).whenever(mockProductRepository).getProduct(any())
            doReturn(true).whenever(mockNetworkStatus).isConnected()

            val mockFactory = mock<ViewModelProvider.Factory>()

            doReturn(mockedProductDetailViewModel).whenever(mockFactory).create<ProductDetailViewModel>(any())

            return mockFactory
        }
    }
}
