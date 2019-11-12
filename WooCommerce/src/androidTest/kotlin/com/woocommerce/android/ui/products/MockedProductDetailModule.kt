package com.woocommerce.android.ui.products

import android.os.Bundle
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers.Unconfined
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
        @Provides
        fun provideProductDetailViewModel(
            currencyFormatter: CurrencyFormatter,
            networkStatus: NetworkStatus,
            wcStore: WooCommerceStore,
            site: SelectedSite
        ): MockedProductDetailViewModel {
            val mockProductRepository = mock<ProductDetailRepository>()
            val coroutineDispatchers = CoroutineDispatchers(Unconfined, Unconfined, Unconfined)
            val savedState: SavedStateWithArgs = mock()

            val mockedProductDetailViewModel = spy(
                    MockedProductDetailViewModel(
                            coroutineDispatchers,
                            wcStore,
                            site,
                            mockProductRepository,
                            networkStatus,
                            currencyFormatter,
                            savedState
                    )
            )

            doReturn(product?.toAppModel()).whenever(mockProductRepository).getProduct(any())
            doReturn(true).whenever(networkStatus).isConnected()

            return mockedProductDetailViewModel
        }

        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }
    }

    @Binds
    @IntoMap
    @ViewModelKey(MockedProductDetailViewModel::class)
    abstract fun bindFactory(factory: MockedProductDetailViewModel.Factory): ViewModelAssistedFactory<out ViewModel>

    @Binds
    abstract fun bindSavedStateRegistryOwner(fragment: ProductDetailFragment): SavedStateRegistryOwner
}
