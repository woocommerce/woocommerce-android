package com.woocommerce.android.ui.products

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.SavedStateWithArgs

final class MockedProductDetailViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    wooCommerceStore: MockedWooStore,
    selectedSite: SelectedSite,
    productRepository: MockedProductDetailRepository,
    productCategoriesRepository: MockedProductCategoriesRepository,
    networkStatus: NetworkStatus,
    currencyFormatter: CurrencyFormatter,
    productImagesServiceWrapper: ProductImagesServiceWrapper,
    @Assisted val arg0: SavedStateWithArgs
) : ProductDetailViewModel(
        arg0,
        dispatchers,
        selectedSite,
        productRepository,
        productCategoriesRepository,
        networkStatus,
        currencyFormatter,
        wooCommerceStore,
        productImagesServiceWrapper
) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedProductDetailViewModel>
}
