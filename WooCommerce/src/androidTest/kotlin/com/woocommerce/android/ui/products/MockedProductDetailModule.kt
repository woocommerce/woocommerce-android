package com.woocommerce.android.ui.products

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@Module
internal abstract class MockedProductDetailModule {
    @Module
    companion object {
        private var product: WCProductModel? = null

        fun setMockProduct(product: WCProductModel) {
            this.product = product
        }

        @JvmStatic
        @ActivityScope
        @Provides
        fun provideProductDetailViewModel(
            currencyFormatter: CurrencyFormatter,
            networkStatus: NetworkStatus,
            wcStore: WooCommerceStore,
            site: SelectedSite
        ): ProductDetailViewModel {
            val mockProductRepository = mock<ProductDetailRepository>()

            val mockedProductDetailViewModel = spy(
                    MockedProductDetailViewModel(
                            Dispatchers.Main,
                            wcStore,
                            site,
                            mockProductRepository,
                            networkStatus,
                            currencyFormatter
                    )
            )

            doReturn(product?.toAppModel()).whenever(mockProductRepository).getProduct(any())
            doReturn(true).whenever(networkStatus).isConnected()

            return mockedProductDetailViewModel
        }
    }

    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
