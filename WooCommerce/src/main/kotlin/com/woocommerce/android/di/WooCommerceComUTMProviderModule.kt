package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.SelectPaymentMethodViewModel
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel
import com.woocommerce.android.util.UtmProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class WooCommerceComUTMProviderModule {
    @Provides
    @Singleton
    @Named("order-list")
    fun provideOrderListUpsellCardReaderUtm(
        selectedSite: SelectedSite
    ) = object : UtmProvider {
        override val campaign: String
            get() = OrderListViewModel.UTM_CAMPAIGN
        override val source: String
            get() = OrderListViewModel.UTM_SOURCE
        override val content: String?
            get() = OrderListViewModel.UTM_CONTENT
        override val siteId: Long?
            get() = selectedSite.getIfExists()?.siteId
    }

    @Provides
    @Singleton
    @Named("select-payment")
    fun provideSelectPaymentMethodUpsellCardReaderUtm(
        selectedSite: SelectedSite
    ) = object : UtmProvider {
        override val campaign: String
            get() = SelectPaymentMethodViewModel.UTM_CAMPAIGN
        override val source: String
            get() = SelectPaymentMethodViewModel.UTM_SOURCE
        override val content: String?
            get() = SelectPaymentMethodViewModel.UTM_CONTENT
        override val siteId: Long?
            get() = selectedSite.getIfExists()?.siteId
    }

    @Provides
    @Singleton
    @Named("payment-menu")
    fun providePaymentMenuUtm(
        selectedSite: SelectedSite
    ) = object : UtmProvider {
        override val campaign: String
            get() = CardReaderHubViewModel.UTM_CAMPAIGN
        override val source: String
            get() = CardReaderHubViewModel.UTM_SOURCE
        override val content: String?
            get() = null
        override val siteId: Long?
            get() = selectedSite.getIfExists()?.siteId
    }
}
