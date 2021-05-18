package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Singleton
    fun provideCardReaderManager(selectedSite: SelectedSite, payStore: WCPayStore): CardReaderManager {
        return CardReaderManagerFactory.createCardReaderManager(object : CardReaderStore {
            override suspend fun getConnectionToken(): String {
                val result = payStore.fetchConnectionToken(selectedSite.get())
                return result.model?.token.orEmpty()
            }

            override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): Boolean {
                return !payStore.capturePayment(selectedSite.get(), paymentId, orderId).isError
            }
        })
    }
}
