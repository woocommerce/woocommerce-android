package com.woocommerce.android.di

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ProductReviewsRepository
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Singleton

@Module
class ProductReviewsRepositoryModule {
    @Provides
    @Singleton
    fun provideProductReviewsRepository(
        dispatcher: Dispatcher,
        productStore: WCProductStore,
        notificationStore: NotificationStore,
        selectedSite: SelectedSite
    ) = ProductReviewsRepository(dispatcher, productStore, notificationStore, selectedSite)
}
