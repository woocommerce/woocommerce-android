package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.hub.PaymentsHubViewModel
import com.woocommerce.android.ui.payments.methodselection.SelectPaymentMethodViewModel
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
    @Named("select-payment")
    fun provideSelectPaymentMethodUpsellCardReaderUtm(
        selectedSite: SelectedSite
    ) = UtmProvider(
        campaign = SelectPaymentMethodViewModel.UTM_CAMPAIGN,
        source = SelectPaymentMethodViewModel.UTM_SOURCE,
        content = SelectPaymentMethodViewModel.UTM_CONTENT,
        siteId = selectedSite.getIfExists()?.siteId
    )

    @Provides
    @Singleton
    @Named("payment-menu")
    fun providePaymentMenuUtm(
        selectedSite: SelectedSite
    ) = UtmProvider(
        campaign = PaymentsHubViewModel.UTM_CAMPAIGN,
        source = PaymentsHubViewModel.UTM_SOURCE,
        content = null,
        siteId = selectedSite.getIfExists()?.siteId
    )
}
