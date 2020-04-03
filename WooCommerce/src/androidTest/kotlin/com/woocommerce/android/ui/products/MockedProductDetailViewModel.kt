package com.woocommerce.android.ui.products

import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.fluxc.store.WooCommerceStore

final class MockedProductDetailViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    wooCommerceStore: WooCommerceStore,
    selectedSite: SelectedSite,
    productRepository: MockedProductDetailRepository,
    networkStatus: NetworkStatus,
    currencyFormatter: CurrencyFormatter,
    @Assisted val arg0: SavedStateWithArgs
) : ProductDetailViewModel(
        arg0,
        dispatchers,
        selectedSite,
        productRepository,
        networkStatus,
        currencyFormatter,
        wooCommerceStore
) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<MockedProductDetailViewModel>
}
