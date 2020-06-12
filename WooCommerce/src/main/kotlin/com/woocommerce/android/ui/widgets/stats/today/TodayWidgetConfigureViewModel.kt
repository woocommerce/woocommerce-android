package com.woocommerce.android.ui.widgets.stats.today

import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore

class TodayWidgetConfigureViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val wooCommerceStore: WooCommerceStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<TodayWidgetConfigureViewModel>
}
