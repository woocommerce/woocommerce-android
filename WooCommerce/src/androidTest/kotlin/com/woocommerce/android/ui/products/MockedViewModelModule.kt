package com.woocommerce.android.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.math.roundToInt

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
            val mockProductRepository = mock<ProductDetailRepository>()

            val mockedProductDetailViewModel = spy(
                    MockedProductDetailViewModel(
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

    // necessary, since we can't mock WooCommerceStore (╯°Д°)╯︵ ┻━┻
    class MockedProductDetailViewModel(
        mainDispatcher: CoroutineDispatcher,
        wooCommerceStore: WooCommerceStore,
        selectedSite: SelectedSite,
        productRepository: ProductDetailRepository,
        networkStatus: NetworkStatus,
        private val currencyFormatter: CurrencyFormatter
    ) : ProductDetailViewModel(
            mainDispatcher,
            wooCommerceStore,
            selectedSite,
            productRepository,
            networkStatus,
            currencyFormatter
    ) {
        override val productData: LiveData<ProductWithParameters>
            get() = Transformations.map(super.productData) {
                combineData(it.product, Parameters("$", "oz", "in"))
            }

        private fun combineData(product: Product, parameters: Parameters): ProductWithParameters {
            val weight = if (product.weight > 0) "${product.weight.roundToInt()}${parameters.weightUnit ?: ""}" else ""

            val hasLength = product.length > 0
            val hasWidth = product.width > 0
            val hasHeight = product.height > 0
            val unit = parameters.dimensionUnit ?: ""
            val size = if (hasLength && hasWidth && hasHeight) {
                "${product.length.roundToInt()} x ${product.width.roundToInt()} x ${product.height.roundToInt()} $unit"
            } else if (hasWidth && hasHeight) {
                "${product.width.roundToInt()} x ${product.height.roundToInt()} $unit"
            } else {
                ""
            }.trim()

            return ProductWithParameters(
                    product,
                    weight,
                    size,
                    formatCurrency(product.price, parameters.currencyCode),
                    formatCurrency(product.salePrice, parameters.currencyCode),
                    formatCurrency(product.regularPrice, parameters.currencyCode)
            )
        }

        private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
            return currencyCode?.let {
                currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
            } ?: amount.toString()
        }
    }
}
