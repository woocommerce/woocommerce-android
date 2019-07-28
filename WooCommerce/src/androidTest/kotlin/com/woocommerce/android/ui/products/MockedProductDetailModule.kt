package com.woocommerce.android.ui.products

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.utils.WCSiteUtils
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@Module
abstract class MockedProductDetailModule {
    @Module
    companion object {
        private var product: WCProductModel? = null

        fun setMockProduct(product: WCProductModel) {
            this.product = product
        }

        @JvmStatic
        @ActivityScope
        @Provides
        fun provideProductDetailPresenter(): ProductDetailContract.Presenter {
            /**
             * Creating a spy object here since we need to mock specific methods of [ProductDetailPresenter] class
             * instead of mocking all the methods in the class.
             * We cannot mock final classes ([WCOrderStore]), so
             * creating a mock instance of those classes and passing to the presenter class constructor.
             */
            val mockDispatcher = mock<Dispatcher>()
            val mockContext = mock<Context>()
            val mockSelectedSite = mock<SelectedSite>()
            val mockNetworkStatus = mock<NetworkStatus>()
            val mockWcStore = WooCommerceStore(
                    mockContext,
                    mockDispatcher,
                    WooCommerceRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
            )

//            doReturn(SiteModel()).whenever(mockSelectedSite).get()
            val mockedProductDetailPresenter = spy(ProductDetailPresenter(
                    mockDispatcher, mockWcStore,
                    WCProductStore(
                            mockDispatcher,
                            ProductRestClient(mockContext, mockDispatcher, mock(), mock(), mock())
                    ),
                    mockSelectedSite, mock(), mockNetworkStatus, mock()
            ))

            /**
             * Mocking the below methods in [ProductDetailPresenter] class to pass mock values.
             * These are the methods that invoke [WCProductModel] methods from FluxC.
             */
            doReturn(product).whenever(mockedProductDetailPresenter).getProduct(any())
            doReturn(WCSiteUtils.generateSiteSettings()).whenever(mockedProductDetailPresenter).getSiteSettings()
            doReturn(WcProductTestUtils.generateProductSettings())
                    .whenever(mockedProductDetailPresenter)
                    .getProductSiteSettings()

            return mockedProductDetailPresenter
        }
    }

    @ContributesAndroidInjector
    abstract fun productDetailfragment(): ProductDetailFragment
}
